package org.tron.tool.litefullnode;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.exception.BadItemException;
import org.tron.tool.litefullnode.db.DBInterface;
import org.tron.tool.litefullnode.iterator.DBIterator;

@Slf4j(topic = "tool")
public class LiteFullNodeTool {

  private static final long START_TIME = System.currentTimeMillis() / 1000;

  private static final String SNAPSHOT_DIR_NAME = "snapshot";
  private static final String HISTORY_DIR_NAME = "history";
  private static final String INFO_FILE_NAME = "info.properties";
  private static final String SPLIT_BLOCK_NUM = "split_block_num";
  private static final String BACKUP_DIR_PREFIX = ".bak_";
  private static final String CHECKPOINT_DB = "tmp";
  private static final long VM_NEED_RECENT_bLKS = 256;

  private static List<String> archiveDbs = Arrays.asList(
          "block", "block-index", "trans", "transactionRetStore", "transactionHistoryStore");
  private static List<String> snapshotDbs = Arrays.asList(
      "DelegatedResource",
      "DelegatedResourceAccountIndex",
      "IncrementalMerkleTree",
      "account",
      "account-index",
      "accountTrie",
      "accountid-index",
      "asset-issue",
      "asset-issue-v2",
      "block_KDB",
      "code",
      //"common",
      "contract",
      "delegation",
      "exchange",
      "exchange-v2",
      //"nullifier",
      "properties",
      "proposal",
      "recent-block",
      "storage-row",
      "trans-cache",
      //"tree-block-index",
      "votes",
      "witness",
      "witness_schedule"
  );

  /**
   * Create the snapshot dataset.
   *
   * @param sourceDir the original fullnode database dir,
   *                  same with {storage.db.directory} in conf file.
   * @param snapshotDir the path that stores the snapshot dataset
   */
  public void generateSnapshot(String sourceDir, String snapshotDir) {
    logger.info("start create snapshot.");
    long start = System.currentTimeMillis();
    snapshotDir = Paths.get(snapshotDir, SNAPSHOT_DIR_NAME).toString();
    try {
      split(sourceDir, snapshotDir, snapshotDbs);
      mergeCheckpoint2Snapshot(sourceDir, snapshotDir);
      // write genesisBlock and latestBlock
      fillSnapshotBlockDb(sourceDir, snapshotDir);
      // create tran-cache if not exist, for compatible
      checkTranCacheStore(sourceDir, snapshotDir);
      generateInfoProperties(Paths.get(snapshotDir, INFO_FILE_NAME).toString(), sourceDir);
    } catch (IOException | RocksDBException e) {
      System.out.println("create snapshot failed, details please check the log: " + e.getMessage());
      logger.error("create snapshot failed, " + e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    System.out.printf("create snapshot finished, take %ds.\n", (end - start) / 1000);
  }

  /**
   * Create the history dataset.
   *
   * @param sourceDir the original fullnode database dir,
   *                  same with {storage.db.directory} in conf file.
   * @param historyDir the path that stores the history dataset
   */
  public void generateHistory(String sourceDir, String historyDir) {
    long start = System.currentTimeMillis();
    historyDir = Paths.get(historyDir, HISTORY_DIR_NAME).toString();
    try {
      split(sourceDir, historyDir, archiveDbs);
      mergeCheckpoint2History(sourceDir, historyDir);
      generateInfoProperties(Paths.get(historyDir, INFO_FILE_NAME).toString(), sourceDir);
    } catch (IOException | RocksDBException e) {
      System.out.println("create history failed, details please check the log: " + e.getMessage());
      logger.error("create history failed, " + e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    System.out.printf("create history finished, take %ds.\n", (end - start) / 1000);
  }

  /**
   * Merge the history dataset into database.
   *
   * @param historyDir the path that stores the history dataset
   *
   * @param databaseDir lite fullnode database path
   */
  public void completeHistoryData(String historyDir, String databaseDir) {
    long start = System.currentTimeMillis();
    BlockNumInfo blockNumInfo = null;
    try {
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
      System.out.println("merge history data to database failed, " + e.getMessage());
      logger.error("merge history data to database failed, " + e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    System.out.printf("merge history finished, take %ds \n", (end - start) / 1000);
  }

  private void mergeCheckpoint2Snapshot(String sourceDir, String historyDir) {
    mergeCheckpoint(sourceDir, historyDir, snapshotDbs);
  }

  private void mergeCheckpoint2History(String sourceDir, String destDir) {
    mergeCheckpoint(sourceDir, destDir, archiveDbs);
  }

  private void split(String sourceDir, String destDir, List<String> dbs) throws IOException {
    logger.info("begin to split the dbs.");
    System.out.println("-- begin to split the dbs.");
    if (!new File(sourceDir).isDirectory()) {
      throw new RuntimeException("sourceDir must be a directory, sourceDir: " + sourceDir);
    }
    File destPath = new File(destDir);
    if (new File(destDir).exists()) {
      throw new RuntimeException("destDir is already exist, please remove it first");
    }
    if (!destPath.mkdir()) {
      throw new RuntimeException("destDir create failed, please check");
    }
    Util.copyDatabases(Paths.get(sourceDir), Paths.get(destDir), dbs);
  }

  private void mergeCheckpoint(String sourceDir, String destDir, List<String> destDbs) {
    logger.info("begin to merge checkpoint to dataset");
    System.out.println(("-- begin to merge checkpoint to dataset"));
    try {
      DBInterface tmpDb = DbTool.getDB(sourceDir, CHECKPOINT_DB);
      try (DBIterator iterator = tmpDb.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          byte[] key = iterator.getKey();
          byte[] value = iterator.getValue();
          String dbName = SnapshotManager.simpleDecode(key);
          byte[] realKey = Arrays.copyOfRange(key, dbName.getBytes().length + 4, key.length);
          byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
          if (destDbs != null && destDbs.contains(dbName)) {
            DBInterface destDb = DbTool.getDB(destDir, dbName);
            if (realValue != null) {
              destDb.put(realKey, realValue);
            } else {
              destDb.delete(realKey);
            }
          }
        }
      }
    } catch (IOException | RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  private void generateInfoProperties(String propertyfile, String databaseDir)
          throws IOException, RocksDBException {
    logger.info("create {} for dataset", INFO_FILE_NAME);
    System.out.printf("-- create %s for dataset.\n", INFO_FILE_NAME);
    if (!FileUtil.createFileIfNotExists(propertyfile)) {
      throw new RuntimeException("create properties file failed...");
    }
    if (!PropUtil.writeProperty(propertyfile, SPLIT_BLOCK_NUM,
            Long.toString(getLatestBlockHeaderNum(databaseDir)))) {
      throw new RuntimeException("write properties file failed...");
    }
  }

  private long getLatestBlockHeaderNum(String databaseDir) throws IOException, RocksDBException {
    // query latest_block_header_number from checkpoint first
    final String latestBlockHeaderNumber = "latest_block_header_number";
    byte[] value = DbTool.getDB(databaseDir, CHECKPOINT_DB).get(
            Bytes.concat(simpleEncode(CHECKPOINT_DB), latestBlockHeaderNumber.getBytes()));
    if (value != null && value.length > 1) {
      return ByteArray.toLong(Arrays.copyOfRange(value, 1, value.length));
    }
    // query from propertiesDb if checkpoint not contains latest_block_header_number
    DBInterface propertiesDb = DbTool.getDB(databaseDir, "properties");
    return Optional.ofNullable(propertiesDb.get(ByteArray.fromString(latestBlockHeaderNumber)))
            .map(ByteArray::toLong)
            .orElseThrow(
                () -> new IllegalArgumentException("not found latest block header number"));
  }

  /**
   * Syncing block from peer that needs latest block and genesis block,
   * also VM need recent blocks.
   */
  private void fillSnapshotBlockDb(String sourceDir, String snapshotDir)
          throws IOException, RocksDBException {
    logger.info("begin to fill latest block and genesis block to snapshot");
    System.out.println(("-- begin to fill latest block and genesis block to snapshot"));
    DBInterface sourceBlockIndexDb = DbTool.getDB(sourceDir, "block-index");
    DBInterface sourceBlockDb = DbTool.getDB(sourceDir, "block");
    DBInterface destBlockDb = DbTool.getDB(snapshotDir, "block");
    DBInterface destBlockIndexDb = DbTool.getDB(snapshotDir, "block-index");
    // put genesis block and block-index into snapshot
    long genesisBlockNum = 0L;
    byte[] genesisBlockID = sourceBlockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
    destBlockIndexDb.put(ByteArray.fromLong(genesisBlockNum), genesisBlockID);
    destBlockDb.put(genesisBlockID, sourceBlockDb.get(genesisBlockID));

    long latestBlockNum = getLatestBlockHeaderNum(sourceDir);
    long startIndex = latestBlockNum > VM_NEED_RECENT_bLKS
            ? latestBlockNum - VM_NEED_RECENT_bLKS : 0;
    // put the recent blocks in snapshot, VM needs recent 256 blocks.
    LongStream.rangeClosed(startIndex, latestBlockNum).forEach(
        blockNum -> {
          byte[] blockId = null;
          byte[] block = null;
          try {
            blockId = getDataFromSourceDB(sourceDir, "block-index", Longs.toByteArray(blockNum));
            block = getDataFromSourceDB(sourceDir, "block", blockId);
          } catch (IOException | RocksDBException e) {
            throw new RuntimeException(e.getMessage());
          }
          // put recent blocks index into snapshot
          destBlockIndexDb.put(ByteArray.fromLong(blockNum), blockId);
          // put latest blocks into snapshot
          destBlockDb.put(blockId, block);
        });
  }

  private void checkTranCacheStore(String sourceDir, String snapshotDir)
          throws IOException, RocksDBException {
    logger.info("create trans-cache db if not exists.");
    System.out.println("-- create trans-cache db if not exists.");
    if (FileUtil.isExists(String.format("%s%s%s", snapshotDir, File.separator, "trans-cache"))) {
      return;
    }
    // fullnode is old version, create trans-cache database
    DBInterface recentBlockDb = DbTool.getDB(snapshotDir, "recent-block");
    DBInterface transCacheDb = DbTool.getDB(snapshotDir, "trans-cache");
    long headNum = getLatestBlockHeaderNum(sourceDir);
    long recentBlockCount = recentBlockDb.size();

    LongStream.rangeClosed(headNum - recentBlockCount + 1, headNum).forEach(
        blockNum -> {
          byte[] blockId = null;
          byte[] block = null;
          try {
            blockId = getDataFromSourceDB(sourceDir, "block-index", Longs.toByteArray(blockNum));
            block = getDataFromSourceDB(sourceDir, "block", blockId);
          } catch (IOException | RocksDBException e) {
            throw new RuntimeException(e.getMessage());
          }
          BlockCapsule blockCapsule = null;
          try {
            blockCapsule = new BlockCapsule(block);
          } catch (BadItemException e) {
            e.printStackTrace();
            throw new RuntimeException("construct block failed, num: " + blockNum);
          }
          if (blockCapsule.getTransactions().isEmpty()) {
            return;
          }
          blockCapsule.getTransactions().stream()
                  .map(tc -> tc.getTransactionId().getBytes())
                  .map(bytes -> Maps.immutableEntry(bytes, Longs.toByteArray(blockNum)))
                  .forEach(e -> transCacheDb.put(e.getKey(), e.getValue()));
        });
  }

  private byte[] getGenesisBlockHash(String parentDir) throws IOException, RocksDBException {
    long genesisBlockNum = 0L;
    DBInterface blockIndexDb = DbTool.getDB(parentDir, "block-index");
    byte[] result = blockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
    // when merge history, block-index db will be moved to bak dir and replaced by history
    // so should close this db and reopen it.
    DbTool.closeDB(parentDir, "block-index");
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
    logger.info("check the compatibility of this history");
    System.out.println("-- check the compatibility of this history");
    String snapshotInfo = String.format("%s%s%s", databaseDir, File.separator, INFO_FILE_NAME);
    String historyInfo = String.format("%s%s%s", historyDir, File.separator, INFO_FILE_NAME);
    if (!FileUtil.isExists(snapshotInfo)) {
      throw new FileNotFoundException(
              "snapshot property file is not found. maybe this is a complete fullnode?");
    }
    if (!FileUtil.isExists(historyInfo)) {
      throw new FileNotFoundException("history property file is not found.");
    }
    long snapshotBlkNum = Long.parseLong(PropUtil.readProperty(snapshotInfo, SPLIT_BLOCK_NUM));
    long historyBlkNum = Long.parseLong(PropUtil.readProperty(historyInfo, SPLIT_BLOCK_NUM));
    if (historyBlkNum < snapshotBlkNum) {
      logger.error("history latest block number is lower than snapshot, history: {}, snapshot: {}",
              historyBlkNum, snapshotBlkNum);
      throw new RuntimeException("history latest block number is lower than snapshot.");
    }
    // check genesis block is equal
    if (!Arrays.equals(getGenesisBlockHash(databaseDir), getGenesisBlockHash(historyDir))) {
      logger.error("genesis block hash is not equal, history: {}, database: {}",
              getGenesisBlockHash(historyDir), getGenesisBlockHash(databaseDir));
      throw new RuntimeException("genesis block is not equal.");
    }
    return new BlockNumInfo(snapshotBlkNum, historyBlkNum);
  }

  private void backupArchiveDbs(String databaseDir) throws IOException {
    String bakDir = String.format("%s%s%s%d",
            databaseDir, File.separator, BACKUP_DIR_PREFIX, START_TIME);
    logger.info("backup the archive dbs to {}", bakDir);
    System.out.printf("-- backup the archive dbs to %s \n", bakDir);
    if (!FileUtil.createDirIfNotExists(bakDir)) {
      throw new RuntimeException("create bak dir failed");
    }
    Util.copyDatabases(Paths.get(databaseDir), Paths.get(bakDir), archiveDbs);
    archiveDbs.forEach(db -> {
      FileUtil.deleteDir(new File(databaseDir, db));
    });
  }

  private void copyHistory2Database(String historyDir, String databaseDir) throws IOException {
    logger.info("begin to copy history to database");
    System.out.println("-- begin to copy history to database");
    Util.copyDatabases(Paths.get(historyDir), Paths.get(databaseDir), archiveDbs);
  }

  private void trimHistory(String databaseDir, BlockNumInfo blockNumInfo)
          throws BadItemException, IOException, RocksDBException {
    logger.info("begin to trim the history data.");
    System.out.println("-- begin to trim the history data.");
    DBInterface blockIndexDb = DbTool.getDB(databaseDir, "block-index");
    DBInterface blockDb = DbTool.getDB(databaseDir, "block");
    DBInterface transDb = DbTool.getDB(databaseDir, "trans");
    DBInterface tranRetDb = DbTool.getDB(databaseDir, "transactionRetStore");
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
    logger.info("begin to merge {} to database", bakDir);
    System.out.printf("-- begin to merge %s to database. \n", bakDir);
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
    DBInterface checkpointDb = DbTool.getDB(sourceDir, "tmp");
    byte[] value = sourceDb.get(key);
    if (isEmptyBytes(value)) {
      byte[] valueFromTmp = checkpointDb.get(Bytes.concat(simpleEncode(dbName), key));
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

  private void deleteSnapshotFlag(String databaseDir) throws IOException {
    logger.info("delete the info file to identify this node is a real fullnode.");
    System.out.println("-- delete the info file to identify this node is a real fullnode.");
    Files.delete(Paths.get(databaseDir, INFO_FILE_NAME));
  }

  private void run(Args argv) {
    if (argv.fnDataPath.isEmpty() || argv.datasetPath.isEmpty()) {
      throw new ParameterException("fnDataPath or datasetPath can't be null");
    }
    switch (argv.operate) {
      case "split":
        if (Strings.isNullOrEmpty(argv.type)) {
          throw new ParameterException("type can't be null when operate=split");
        }
        if ("snapshot".equals(argv.type)) {
          generateSnapshot(argv.fnDataPath, argv.datasetPath);
        } else if ("history".equals(argv.type)) {
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
    } catch (ParameterException parameterException) {
      System.out.print(parameterException.toString() + "\r\n");
      jct.usage();
    }
  }

  static class Args {
    @Parameter(
            names = {"--operate", "-o"},
            help = true, required = true,
            description = "operate: [ split | merge ]",
            order = 1)
    String operate;
    @Parameter(names = {"--type", "-t"},
            help = true,
            description = "only used with operate=split: [ snapshot | history ]",
            order = 2)
    String type;
    @Parameter(
            names = {"--fn-data-path"},
            help = true, required = true,
            description = "the fullnode database path,"
                    + " defined as ${storage.db.directory} in config.conf",
            order = 3)
    String fnDataPath;
    @Parameter(
            names = {"--dataset-path"},
            help = true, required = true,
            description = "dataset directory, when operation is `split`, "
                    + "`dataset-path` is the path that store the `Snapshot Dataset` or "
                    + "`History Dataset`, otherwise `dataset-path` should be "
                    + "the `History Dataset` path",
            order = 4)
    String datasetPath;
    @Parameter(
            names = "--help",
            help = true,
            order = 5)
    boolean help;
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



