package org.tron.tool.litefullnode;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.RocksDBException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.exception.BadItemException;
import org.tron.tool.litefullnode.db.DBInterface;
import org.tron.tool.litefullnode.iterator.DBIterator;

@Slf4j(topic = "tool")
public class LiteFullNodeTool {

  private static final byte[] DB_KEY_LOWEST_BLOCK_NUM = "lowest_block_num".getBytes();
  private static final byte[] DB_KEY_NODE_TYPE = "node_type".getBytes();

  private static final long START_TIME = System.currentTimeMillis() / 1000;

  private static long RECENT_BLKS = 65536;

  private static final String SNAPSHOT_DIR_NAME = "snapshot";
  private static final String HISTORY_DIR_NAME = "history";
  private static final String INFO_FILE_NAME = "info.properties";
  private static final String BACKUP_DIR_PREFIX = ".bak_";
  private static final String CHECKPOINT_DB = "tmp";
  private static final String CHECKPOINT_DB_V2 = "checkpoint";
  private static final String BLOCK_DB_NAME = "block";
  private static final String BLOCK_INDEX_DB_NAME = "block-index";
  private static final String TRANS_DB_NAME = "trans";
  private static final String COMMON_DB_NAME = "common";
  private static final String TRANSACTION_RET_DB_NAME = "transactionRetStore";
  private static final String TRANSACTION_HISTORY_DB_NAME = "transactionHistoryStore";
  private static final String PROPERTIES_DB_NAME = "properties";

  private static final String DIR_FORMAT_STRING = "%s%s%s";

  private static List<String> archiveDbs = Arrays.asList(
      BLOCK_DB_NAME,
      BLOCK_INDEX_DB_NAME,
      TRANS_DB_NAME,
      TRANSACTION_RET_DB_NAME,
      TRANSACTION_HISTORY_DB_NAME);

  /**
   * Create the snapshot dataset.
   *
   * @param sourceDir the original fullnode database dir,
   *                  same with {storage.db.directory} in conf file.
   * @param snapshotDir the path that stores the snapshot dataset
   */
  public void generateSnapshot(String sourceDir, String snapshotDir) {
    logger.info("Start create snapshot.");
    long start = System.currentTimeMillis();
    snapshotDir = Paths.get(snapshotDir, SNAPSHOT_DIR_NAME).toString();
    try {
      hasEnoughBlock(sourceDir);
      List<String> snapshotDbs = getSnapshotDbs(sourceDir);
      split(sourceDir, snapshotDir, snapshotDbs);
      mergeCheckpoint2Snapshot(sourceDir, snapshotDir);
      // write genesisBlock , latest recent blocks and trans
      fillSnapshotBlockAndTransDb(sourceDir, snapshotDir);
      generateInfoProperties(Paths.get(snapshotDir, INFO_FILE_NAME).toString(), sourceDir);
    } catch (IOException | RocksDBException e) {
      logger.error("Create snapshot failed, {}.", e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    logger.info("Create snapshot finished, take {} s.\n", (end - start) / 1000);
  }

  /**
   * Create the history dataset.
   *
   * @param sourceDir the original fullnode database dir,
   *                  same with {storage.db.directory} in conf file.
   * @param historyDir the path that stores the history dataset
   */
  public void generateHistory(String sourceDir, String historyDir) {
    logger.info("Start create history.");
    long start = System.currentTimeMillis();
    historyDir = Paths.get(historyDir, HISTORY_DIR_NAME).toString();
    try {
      if (isLite(sourceDir)) {
        throw new IllegalStateException(
            String.format("Unavailable sourceDir: %s is not fullNode data.", sourceDir));
      }
      hasEnoughBlock(sourceDir);
      split(sourceDir, historyDir, archiveDbs);
      mergeCheckpoint2History(sourceDir, historyDir);
      generateInfoProperties(Paths.get(historyDir, INFO_FILE_NAME).toString(), sourceDir);
    } catch (IOException | RocksDBException e) {
      logger.error("Create history failed, {}.", e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    logger.info("Create history finished, take {} s.\n", (end - start) / 1000);
  }

  /**
   * Merge the history dataset into database.
   *
   * @param historyDir the path that stores the history dataset
   *
   * @param databaseDir lite fullnode database path
   */
  public void completeHistoryData(String historyDir, String databaseDir) {
    logger.info("Start merge history to lite node.");
    long start = System.currentTimeMillis();
    BlockNumInfo blockNumInfo = null;
    try {
      // check historyDir is from lite data
      if (isLite(historyDir)) {
        throw new IllegalStateException(
            String.format("Unavailable history: %s is not generated by fullNode data.",
                historyDir));
      }
      // 1. check block number and genesis block are compatible,
      //    and return the block numbers of snapshot and history
      blockNumInfo = checkAndGetBlockNumInfo(historyDir, databaseDir);
      // 2. move archive dbs to bak
      backupArchiveDbs(databaseDir);
      // 3. copy history data to databaseDir
      copyHistory2Database(historyDir, databaseDir);
      // 4. delete the duplicate block data in history data
      trimHistory(databaseDir, blockNumInfo);
      // 5. merge bak to database
      mergeBak2Database(databaseDir);
      // 6. delete snapshot flag
      deleteSnapshotFlag(databaseDir);
    } catch (IOException | RocksDBException | BadItemException e) {
      logger.error("Merge history data to database failed, {}.", e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    logger.info("Merge history finished, take {} s. \n", (end - start) / 1000);
  }

  private List<String> getSnapshotDbs(String sourceDir) {
    List<String> snapshotDbs = Lists.newArrayList();
    File basePath = new File(sourceDir);
    Arrays.stream(Objects.requireNonNull(basePath.listFiles()))
            .filter(File::isDirectory)
            .filter(dir -> !archiveDbs.contains(dir.getName()))
            .forEach(dir -> snapshotDbs.add(dir.getName()));
    return snapshotDbs;
  }

  private void mergeCheckpoint2Snapshot(String sourceDir, String historyDir) {
    List<String> snapshotDbs = getSnapshotDbs(sourceDir);
    mergeCheckpoint(sourceDir, historyDir, snapshotDbs);
  }

  private void mergeCheckpoint2History(String sourceDir, String destDir) {
    mergeCheckpoint(sourceDir, destDir, archiveDbs);
  }

  private void split(String sourceDir, String destDir, List<String> dbs) throws IOException {
    logger.info("Begin to split the dbs.");
    if (!new File(sourceDir).isDirectory()) {
      throw new RuntimeException(String.format("sourceDir: %s must be a directory ", sourceDir));
    }
    File destPath = new File(destDir);
    if (new File(destDir).exists()) {
      throw new RuntimeException(String.format(
          "destDir: %s is already exist, please remove it first", destDir));
    }
    if (!destPath.mkdirs()) {
      throw new RuntimeException(String.format("destDir: %s create failed, please check", destDir));
    }
    Util.copyDatabases(Paths.get(sourceDir), Paths.get(destDir), dbs);
  }

  private void mergeCheckpoint(String sourceDir, String destDir, List<String> destDbs) {
    logger.info("Begin to merge checkpoint to dataset.");
    try {
      List<String> cpList = getCheckpointV2List(sourceDir);
      if (cpList.size() > 0) {
        for (String cp: cpList) {
          DBInterface checkpointDb = DbTool.getDB(sourceDir + "/" + CHECKPOINT_DB_V2, cp);
          recover(checkpointDb, destDir, destDbs);
        }
      } else {
        DBInterface tmpDb = DbTool.getDB(sourceDir, CHECKPOINT_DB);
        recover(tmpDb, destDir, destDbs);
      }
    } catch (IOException | RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  private void recover(DBInterface db, String destDir, List<String> destDbs)
      throws IOException, RocksDBException {
    try (DBIterator iterator = db.iterator()) {
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        byte[] key = iterator.getKey();
        byte[] value = iterator.getValue();
        String dbName = SnapshotManager.simpleDecode(key);
        byte[] realKey = Arrays.copyOfRange(key, dbName.getBytes().length + 4, key.length);
        byte[] realValue =
            value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
        if (destDbs != null && destDbs.contains(dbName)) {
          DBInterface destDb = DbTool.getDB(destDir, dbName);
          if (realValue != null) {
            destDb.put(realKey, realValue);
          } else {
            byte op = value[0];
            if (Value.Operator.DELETE.getValue() == op) {
              destDb.delete(realKey);
            } else {
              destDb.put(realKey, new byte[0]);
            }
          }
        }
      }
    }
  }

  private void generateInfoProperties(String propertyfile, String databaseDir)
          throws IOException, RocksDBException {
    logger.info("Create {} for dataset.", INFO_FILE_NAME);
    if (!FileUtil.createFileIfNotExists(propertyfile)) {
      throw new RuntimeException("Create properties file failed.");
    }
    if (!PropUtil.writeProperty(propertyfile, Constant.SPLIT_BLOCK_NUM,
            Long.toString(getLatestBlockHeaderNum(databaseDir)))) {
      throw new RuntimeException("Write properties file failed.");
    }
  }

  private long getLatestBlockHeaderNum(String databaseDir) throws IOException, RocksDBException {
    // query latest_block_header_number from checkpoint first
    final String latestBlockHeaderNumber = "latest_block_header_number";
    List<String> cpList = getCheckpointV2List(databaseDir);
    DBInterface checkpointDb = null;
    if (cpList.size() > 0) {
      String lastestCp = cpList.get(cpList.size() - 1);
      checkpointDb = DbTool.getDB(databaseDir + "/" + CHECKPOINT_DB_V2, lastestCp);
    } else {
      checkpointDb = DbTool.getDB(databaseDir, CHECKPOINT_DB);
    }
    Long blockNumber = getLatestBlockHeaderNumFromCP(checkpointDb,
        latestBlockHeaderNumber.getBytes());
    if (blockNumber != null) {
      return blockNumber;
    }
    // query from propertiesDb if checkpoint not contains latest_block_header_number
    DBInterface propertiesDb = DbTool.getDB(databaseDir, PROPERTIES_DB_NAME);
    return Optional.ofNullable(propertiesDb.get(ByteArray.fromString(latestBlockHeaderNumber)))
            .map(ByteArray::toLong)
            .orElseThrow(
                () -> new IllegalArgumentException("not found latest block header number"));
  }

  private Long getLatestBlockHeaderNumFromCP(DBInterface db, byte[] key) {
    byte[] value = db.get(Bytes.concat(simpleEncode(PROPERTIES_DB_NAME), key));
    if (value != null && value.length > 1) {
      return ByteArray.toLong(Arrays.copyOfRange(value, 1, value.length));
    }
    return null;
  }

  /**
   * recent blocks, trans and genesis block.
   */
  private void fillSnapshotBlockAndTransDb(String sourceDir, String snapshotDir)
          throws IOException, RocksDBException {
    logger.info("Begin to fill {} block, genesis block and trans to snapshot.", RECENT_BLKS);
    DBInterface sourceBlockIndexDb = DbTool.getDB(sourceDir, BLOCK_INDEX_DB_NAME);
    DBInterface sourceBlockDb = DbTool.getDB(sourceDir, BLOCK_DB_NAME);
    DBInterface destBlockDb = DbTool.getDB(snapshotDir, BLOCK_DB_NAME);
    DBInterface destBlockIndexDb = DbTool.getDB(snapshotDir, BLOCK_INDEX_DB_NAME);
    DBInterface destTransDb = DbTool.getDB(snapshotDir, TRANS_DB_NAME);
    // put genesis block and block-index into snapshot
    long genesisBlockNum = 0L;
    byte[] genesisBlockID = sourceBlockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
    destBlockIndexDb.put(ByteArray.fromLong(genesisBlockNum), genesisBlockID);
    destBlockDb.put(genesisBlockID, sourceBlockDb.get(genesisBlockID));

    long latestBlockNum = getLatestBlockHeaderNum(sourceDir);
    long startIndex = latestBlockNum - RECENT_BLKS + 1;
    // put the recent blocks and trans in snapshot
    for (long blockNum = startIndex; blockNum <= latestBlockNum; blockNum++) {
      try {
        byte[] blockId = getDataFromSourceDB(sourceDir, BLOCK_INDEX_DB_NAME,
            Longs.toByteArray(blockNum));
        byte[] block = getDataFromSourceDB(sourceDir, BLOCK_DB_NAME, blockId);
        // put block
        destBlockDb.put(blockId, block);
        // put block index
        destBlockIndexDb.put(ByteArray.fromLong(blockNum), blockId);
        // put trans
        long finalBlockNum = blockNum;
        new BlockCapsule(block).getTransactions().stream().map(
            tc -> tc.getTransactionId().getBytes())
            .map(bytes -> Maps.immutableEntry(bytes, Longs.toByteArray(finalBlockNum)))
            .forEach(e -> destTransDb.put(e.getKey(), e.getValue()));
      } catch (IOException | RocksDBException  | BadItemException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    DBInterface destCommonDb = DbTool.getDB(snapshotDir, COMMON_DB_NAME);
    destCommonDb.put(DB_KEY_NODE_TYPE, ByteArray.fromInt(Constant.NODE_TYPE_LIGHT_NODE));
    destCommonDb.put(DB_KEY_LOWEST_BLOCK_NUM, ByteArray.fromLong(startIndex));
    // copy engine.properties for block、block-index、trans from source if exist
    copyEngineIfExist(sourceDir, snapshotDir, BLOCK_DB_NAME, BLOCK_INDEX_DB_NAME, TRANS_DB_NAME);
  }

  private void copyEngineIfExist(String source, String dest, String... dbNames) {
    for (String dbName : dbNames) {
      Path ori = Paths.get(source, dbName, DbTool.ENGINE_FILE);
      if (ori.toFile().exists()) {
        Util.copy(ori, Paths.get(dest, dbName, DbTool.ENGINE_FILE));
      }
    }
  }

  private byte[] getGenesisBlockHash(String parentDir) throws IOException, RocksDBException {
    long genesisBlockNum = 0L;
    DBInterface blockIndexDb = DbTool.getDB(parentDir, BLOCK_INDEX_DB_NAME);
    byte[] result = blockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
    // when merge history, block-index db will be moved to bak dir and replaced by history
    // so should close this db and reopen it.
    DbTool.closeDB(parentDir, BLOCK_INDEX_DB_NAME);
    return result;
  }

  private static byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  private BlockNumInfo checkAndGetBlockNumInfo(String historyDir, String databaseDir)
          throws IOException, RocksDBException {
    logger.info("Check the compatibility of this history.");
    String snapshotInfo = String.format(
            DIR_FORMAT_STRING, databaseDir, File.separator, INFO_FILE_NAME);
    String historyInfo = String.format(
            DIR_FORMAT_STRING, historyDir, File.separator, INFO_FILE_NAME);
    if (!FileUtil.isExists(snapshotInfo)) {
      throw new FileNotFoundException(
              "Snapshot property file is not found. maybe this is a complete fullnode?");
    }
    if (!FileUtil.isExists(historyInfo)) {
      throw new FileNotFoundException("history property file is not found.");
    }
    long snapshotBlkNum = Long.parseLong(PropUtil.readProperty(snapshotInfo, Constant
            .SPLIT_BLOCK_NUM));
    long historyBlkNum = Long.parseLong(PropUtil.readProperty(historyInfo, Constant
            .SPLIT_BLOCK_NUM));
    if (historyBlkNum < snapshotBlkNum) {
      throw new RuntimeException(
          String.format(
              "History latest block number is lower than snapshot, history: %d, snapshot: %d",
          historyBlkNum, snapshotBlkNum));
    }
    // check genesis block is equal
    if (!Arrays.equals(getGenesisBlockHash(databaseDir), getGenesisBlockHash(historyDir))) {
      throw new RuntimeException(String.format(
          "Genesis block hash is not equal, history: %s, database: %s",
          Arrays.toString(getGenesisBlockHash(historyDir)),
          Arrays.toString(getGenesisBlockHash(databaseDir))));
    }
    return new BlockNumInfo(snapshotBlkNum, historyBlkNum);
  }

  private void backupArchiveDbs(String databaseDir) throws IOException {
    String bakDir = String.format("%s%s%s%d",
            databaseDir, File.separator, BACKUP_DIR_PREFIX, START_TIME);
    logger.info("Backup the archive dbs to {}.", bakDir);
    if (!FileUtil.createDirIfNotExists(bakDir)) {
      throw new RuntimeException(String.format("create bak dir %s failed", bakDir));
    }
    Util.copyDatabases(Paths.get(databaseDir), Paths.get(bakDir), archiveDbs);
    archiveDbs.forEach(db -> FileUtil.deleteDir(new File(databaseDir, db)));
  }

  private void copyHistory2Database(String historyDir, String databaseDir) throws IOException {
    logger.info("Begin to copy history to database.");
    Util.copyDatabases(Paths.get(historyDir), Paths.get(databaseDir), archiveDbs);
  }

  private void trimHistory(String databaseDir, BlockNumInfo blockNumInfo)
          throws BadItemException, IOException, RocksDBException {
    logger.info("Begin to trim the history data.");
    DBInterface blockIndexDb = DbTool.getDB(databaseDir, BLOCK_INDEX_DB_NAME);
    DBInterface blockDb = DbTool.getDB(databaseDir, BLOCK_DB_NAME);
    DBInterface transDb = DbTool.getDB(databaseDir, TRANS_DB_NAME);
    DBInterface tranRetDb = DbTool.getDB(databaseDir, TRANSACTION_RET_DB_NAME);
    for (long n = blockNumInfo.getHistoryBlkNum(); n > blockNumInfo.getSnapshotBlkNum(); n--) {
      byte[] blockIdHash = blockIndexDb.get(ByteArray.fromLong(n));
      BlockCapsule block = new BlockCapsule(blockDb.get(blockIdHash));
      // delete transactions
      for (TransactionCapsule e : block.getTransactions()) {
        transDb.delete(e.getTransactionId().getBytes());
      }
      // delete transaction result
      tranRetDb.delete(ByteArray.fromLong(n));
      // delete block
      blockDb.delete(blockIdHash);
      // delete block index
      blockIndexDb.delete(ByteArray.fromLong(n));
    }
  }

  private void mergeBak2Database(String databaseDir) throws IOException, RocksDBException {
    String bakDir = String.format("%s%s%s%d",
            databaseDir, File.separator, BACKUP_DIR_PREFIX, START_TIME);
    logger.info("Begin to merge {} to database.", bakDir);
    for (String dbName : archiveDbs) {
      DBInterface bakDb = DbTool.getDB(bakDir, dbName);
      DBInterface destDb = DbTool.getDB(databaseDir, dbName);
      try (DBIterator iterator = bakDb.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          destDb.put(iterator.getKey(), iterator.getValue());
        }
      }
    }
  }

  private byte[] getDataFromSourceDB(String sourceDir, String dbName, byte[] key)
          throws IOException, RocksDBException {
    DBInterface sourceDb = DbTool.getDB(sourceDir, dbName);
    DBInterface checkpointDb = DbTool.getDB(sourceDir, CHECKPOINT_DB);
    // get data from tmp first.
    byte[] valueFromTmp = checkpointDb.get(Bytes.concat(simpleEncode(dbName), key));
    byte[] value;
    if (isEmptyBytes(valueFromTmp)) {
      value = sourceDb.get(key);
    } else {
      value = valueFromTmp.length == 1
          ? null : Arrays.copyOfRange(valueFromTmp, 1, valueFromTmp.length);
    }
    if (isEmptyBytes(value)) {
      throw new RuntimeException(String.format("data not found in store, dbName: %s, key: %s",
              dbName, Arrays.toString(key)));
    }
    return value;
  }

  /**
   * return true if byte array is null or length is 0.
   * @param b bytes
   * @return true or false
   */
  private static boolean isEmptyBytes(byte[] b) {
    if (b != null) {
      return b.length == 0;
    }
    return true;
  }

  private void deleteSnapshotFlag(String databaseDir) throws IOException, RocksDBException {
    logger.info("Delete the info file from {}.", databaseDir);
    Files.delete(Paths.get(databaseDir, INFO_FILE_NAME));
    if (!isLite(databaseDir)) {
      DBInterface destCommonDb = DbTool.getDB(databaseDir, COMMON_DB_NAME);
      destCommonDb.delete(DB_KEY_NODE_TYPE);
      destCommonDb.delete(DB_KEY_LOWEST_BLOCK_NUM);
      logger.info("Deleted {} and {} from {} to identify this node is a real fullnode.",
          "node_type", "lowest_block_num", COMMON_DB_NAME);
    }

  }

  private void hasEnoughBlock(String sourceDir) throws RocksDBException, IOException {
    // check latest
    long latest = getLatestBlockHeaderNum(sourceDir);
    // check second ,skip 0;
    long second = getSecondBlock(sourceDir);
    if (latest - second + 1 < RECENT_BLKS) {
      throw new NoSuchElementException(
          String.format("At least %d blocks in block store, actual latestBlock:%d, firstBlock:%d.",
          RECENT_BLKS, latest, second));
    }
  }

  private boolean isLite(String databaseDir) throws RocksDBException, IOException {
    return getSecondBlock(databaseDir) > 1;
  }

  private long getSecondBlock(String databaseDir) throws RocksDBException, IOException {
    long num = 0;
    DBInterface sourceBlockIndexDb = DbTool.getDB(databaseDir, BLOCK_INDEX_DB_NAME);
    DBIterator iterator = sourceBlockIndexDb.iterator();
    iterator.seek(ByteArray.fromLong(1));
    if (iterator.hasNext()) {
      num =  Longs.fromByteArray(iterator.getKey());
    }
    return num;
  }

  @VisibleForTesting
  public static void setRecentBlks(long recentBlks) {
    RECENT_BLKS = recentBlks;
  }

  @VisibleForTesting
  public static void reSetRecentBlks() {
    RECENT_BLKS = 65536;
  }

  private List<String> getCheckpointV2List(String sourceDir) {
    File file = new File(Paths.get(sourceDir, CHECKPOINT_DB_V2).toString());
    if (file.exists() && file.isDirectory() && file.list() != null) {
      return Arrays.stream(file.list()).sorted().collect(Collectors.toList());
    }
    return Lists.newArrayList();
  }

  private void run(Args argv) {
    if (StringUtils.isBlank(argv.fnDataPath) || StringUtils.isBlank(argv.datasetPath)) {
      throw new ParameterException("fnDataPath or datasetPath can't be null");
    }
    switch (argv.operate) {
      case "split":
        if (Strings.isNullOrEmpty(argv.type)) {
          throw new ParameterException("type can't be null when operate=split");
        }
        if (SNAPSHOT_DIR_NAME.equals(argv.type)) {
          generateSnapshot(argv.fnDataPath, argv.datasetPath);
        } else if (HISTORY_DIR_NAME.equals(argv.type)) {
          generateHistory(argv.fnDataPath, argv.datasetPath);
        } else {
          throw new ParameterException("not support type:" + argv.type);
        }
        break;
      case "merge":
        completeHistoryData(argv.datasetPath, argv.fnDataPath);
        break;
      default:
        throw new ParameterException("not supportted operate:" + argv.operate);
    }
    DbTool.close();
  }

  /**
   * main.
   */
  public static void main(String[] args) {
    Args argv = new Args();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    LiteFullNodeTool tool = new LiteFullNodeTool();
    JCommander jct = JCommander.newBuilder()
            .addObject(argv)
            .build();
    jct.setProgramName("lite fullnode tool");
    try {
      jct.parse(args);
      if (argv.help) {
        jct.usage();
      } else {
        tool.run(argv);
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      jct.usage();
    }
  }

  static class Args {
    @Parameter(
            names = {"--operate", "-o"},
            help = true, required = true,
            description = "operate: [ split | merge ]",
            order = 1)
    private String operate;
    @Parameter(names = {"--type", "-t"},
            help = true,
            description = "only used with operate=split: [ snapshot | history ]",
            order = 2)
    private String type;
    @Parameter(
            names = {"--fn-data-path"},
            help = true, required = true,
            description = "the fullnode database path,"
                    + " defined as ${storage.db.directory} in config.conf",
            order = 3)
    private String fnDataPath;
    @Parameter(
            names = {"--dataset-path"},
            help = true, required = true,
            description = "dataset directory, when operation is `split`, "
                    + "`dataset-path` is the path that store the `Snapshot Dataset` or "
                    + "`History Dataset`, otherwise `dataset-path` should be "
                    + "the `History Dataset` path",
            order = 4)
    private String datasetPath;
    @Parameter(
            names = "--help",
            help = true,
            order = 5)
    private boolean help;
  }

  static class BlockNumInfo {
    private long snapshotBlkNum;
    private long historyBlkNum;

    public BlockNumInfo(long snapshotBlkNum, long historyBlkNum) {
      this.snapshotBlkNum = snapshotBlkNum;
      this.historyBlkNum = historyBlkNum;
    }

    public long getSnapshotBlkNum() {
      return snapshotBlkNum;
    }

    public long getHistoryBlkNum() {
      return historyBlkNum;
    }
  }
}



