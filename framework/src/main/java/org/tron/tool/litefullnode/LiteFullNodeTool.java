package org.tron.tool.litefullnode;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
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
import org.tron.core.exception.BadItemException;
import org.tron.tool.litefullnode.db.TronDB;
import org.tron.tool.litefullnode.iterator.DBIterator;

@Slf4j(topic = "tool")
public class LiteFullNodeTool {

  private static final long START_TIME = System.currentTimeMillis() / 1000;

  private static final String SNAPSHOT_DIR_NAME = "snapshot";
  private static final String HISTORY_DIR_NAME = "history";
  private static final String INFO_FILE_NAME = "info.properties";
  private static final String BACKUP_DIR_PREFIX = ".bak_";
  public static final String CHECKPOINT_DB = "tmp";
  private static final long VM_NEED_RECENT_BLKS = 256;

  public static final String BLOCK_DB_NAME = "block";
  public static final String BLOCK_INDEX_DB_NAME = "block-index";
  public static final String PROPERTIES_DB_NAME = "properties";

  private static final String DIR_FORMAT_STRING = "%s%s%s";

  private static final List<String> archiveDbs = Arrays.asList(
      BLOCK_DB_NAME,
      BLOCK_INDEX_DB_NAME,
      "trans",
      "transactionRetStore",
      "transactionHistoryStore");

  /**
   * Create the snapshot dataset.
   *
   * @param sourceDir the original fullnode database dir,
   *                  same with {storage.db.directory} in conf file.
   * @param snapshotDir the path that stores the snapshot dataset
   * @param check if check result
   */
  public void generateSnapshot(String sourceDir, String snapshotDir, boolean check) {
    logger.info("start create snapshot.");
    final long start = System.currentTimeMillis();
    snapshotDir = Paths.get(snapshotDir, SNAPSHOT_DIR_NAME).toString();
    try {
      List<String> snapshotDbs = getSnapshotDbs(sourceDir);
      split(sourceDir, snapshotDir, snapshotDbs);
      // write genesisBlock and latestBlock
      // init tmp
      initTmpDb(sourceDir);
      fillSnapshotBlockDb(sourceDir, snapshotDir);
      generateInfoProperties(Paths.get(snapshotDir, INFO_FILE_NAME).toString(), sourceDir);
    } catch (IOException | RocksDBException e) {
      logger.error("create snapshot failed, " + e.getMessage());
      return;
    }
    if (check) {
      checkSnapshot(sourceDir, snapshotDir);
    }

    long end = System.currentTimeMillis();
    logger.info("create snapshot finished, take {}s.", (end - start) / 1000);
  }

  public void checkSnapshot(String sourceDir, String snapshotDir) {
    // check checkSnapshot start
    logger.info("check checkSnapshot start.");
    File snapshotPath = new File(snapshotDir);
    List<File> files = Arrays.stream(Objects.requireNonNull(snapshotPath.listFiles()))
        .filter(File::isDirectory).collect(
            Collectors.toList());
    final int CPUS  = Runtime.getRuntime().availableProcessors();
    final List<Future<Boolean>> res = new ArrayList<>();

    final ThreadPoolExecutor esDb = new ThreadPoolExecutor(
        CPUS, 16 * CPUS, 1, TimeUnit.MINUTES,
        new ArrayBlockingQueue<>(CPUS, true), Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy());

    esDb.allowCoreThreadTimeOut(true);
    files.forEach(f -> res.add(esDb.submit(new CheckLite(sourceDir, snapshotDir, f.getName()))));

    esDb.shutdown();

    for (Future<Boolean> re : res) {
      try {
        re.get();
      } catch (InterruptedException e) {
        logger.error("{}", e);
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        logger.error("{}", e);
        throw  new RuntimeException(e);
      }
    }
    // check checkSnapshot end
    logger.info("check checkSnapshot end.");
  }

  /**
   * Create the history dataset.
   *
   * @param sourceDir the original fullnode database dir,
   *                  same with {storage.db.directory} in conf file.
   * @param historyDir the path that stores the history dataset
   */
  public void generateHistory(String sourceDir, String historyDir) {
    logger.info("start create history.");
    long start = System.currentTimeMillis();
    historyDir = Paths.get(historyDir, HISTORY_DIR_NAME).toString();
    try {
      split(sourceDir, historyDir, archiveDbs);
      mergeCheckpoint2History(sourceDir, historyDir);
      generateInfoProperties(Paths.get(historyDir, INFO_FILE_NAME).toString(), sourceDir);
    } catch (IOException | RocksDBException e) {
      logger.error("create history failed, " + e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    logger.info("create history finished, take {}s.\n", (end - start) / 1000);
  }

  /**
   * Merge the history dataset into database.
   *
   * @param historyDir the path that stores the history dataset
   *
   * @param databaseDir lite fullnode database path
   */
  public void completeHistoryData(String historyDir, String databaseDir) {
    logger.info("start merge history to lite fullnode.");
    long start = System.currentTimeMillis();
    BlockNumInfo blockNumInfo = null;
    try {
      //0. init tmp db
      initTmpDb(databaseDir);
      initTmpDb(historyDir);
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
      logger.error("merge history data to database failed, " + e.getMessage());
      return;
    }
    long end = System.currentTimeMillis();
    logger.info("merge history finished, take {}s \n", (end - start) / 1000);
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

  private void mergeCheckpoint2History(String sourceDir, String destDir) {
    mergeCheckpoint(sourceDir, destDir, archiveDbs);
  }

  private void split(String sourceDir, String destDir, List<String> dbs) throws IOException {
    logger.info("-- begin to split the dbs.");
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
    logger.info("-- begin to merge checkpoint to dataset");
    try {
      TronDB tmpDb = DbTool.getDB(sourceDir, CHECKPOINT_DB);
      try (DBIterator iterator = tmpDb.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          byte[] key = iterator.getKey();
          byte[] value = iterator.getValue();
          String dbName = tmpDb.simpleDecode(key);
          byte[] realKey = Arrays.copyOfRange(key, dbName.getBytes().length + 4, key.length);
          byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
          if (destDbs != null && destDbs.contains(dbName)) {
            TronDB destDb = DbTool.getDB(destDir, dbName);
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
    logger.info("-- create {} for dataset", INFO_FILE_NAME);
    if (!FileUtil.createFileIfNotExists(propertyfile)) {
      throw new RuntimeException("create properties file failed...");
    }
    if (!PropUtil.writeProperty(propertyfile, Constant.SPLIT_BLOCK_NUM,
            Long.toString(getLatestBlockHeaderNum(databaseDir)))) {
      throw new RuntimeException("write properties file failed...");
    }
  }

  private long getLatestBlockHeaderNum(String databaseDir) throws IOException, RocksDBException {
    // query latest_block_header_number
    final String latestBlockHeaderNumber = "latest_block_header_number";
    TronDB propertiesDb = DbTool.getDB(databaseDir, PROPERTIES_DB_NAME);
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
    logger.info("-- begin to fill latest block and genesis block to snapshot");
    TronDB sourceBlockIndexDb = DbTool.getDB(sourceDir, BLOCK_INDEX_DB_NAME);
    TronDB sourceBlockDb = DbTool.getDB(sourceDir, BLOCK_DB_NAME);
    TronDB destBlockDb = DbTool.getDB(snapshotDir, BLOCK_DB_NAME);
    TronDB destBlockIndexDb = DbTool.getDB(snapshotDir, BLOCK_INDEX_DB_NAME);
    // put genesis block and block-index into snapshot
    long genesisBlockNum = 0L;
    byte[] genesisBlockID = sourceBlockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
    destBlockIndexDb.put(ByteArray.fromLong(genesisBlockNum), genesisBlockID);
    destBlockDb.put(genesisBlockID, sourceBlockDb.get(genesisBlockID));

    long latestBlockNum = getLatestBlockHeaderNum(sourceDir);
    long startIndex = latestBlockNum > VM_NEED_RECENT_BLKS
            ? latestBlockNum - VM_NEED_RECENT_BLKS : 0;
    // put the recent blocks in snapshot, VM needs recent 256 blocks.
    LongStream.rangeClosed(startIndex, latestBlockNum).forEach(
        blockNum -> {
          byte[] blockId = sourceBlockIndexDb.get(Longs.toByteArray(blockNum));
          byte[] block = sourceBlockDb.get(blockId);
          // put recent blocks index into snapshot
          destBlockIndexDb.put(ByteArray.fromLong(blockNum), blockId);
          // put latest blocks into snapshot
          destBlockDb.put(blockId, block);
        });
  }

  private byte[] getGenesisBlockHash(String parentDir) throws IOException, RocksDBException {
    long genesisBlockNum = 0L;
    TronDB blockIndexDb = DbTool.getDB(parentDir, BLOCK_INDEX_DB_NAME);
    byte[] result = blockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
    // when merge history, block-index db will be moved to bak dir and replaced by history
    // so should close this db and reopen it.
    DbTool.closeDB(parentDir, BLOCK_INDEX_DB_NAME);
    return result;
  }

  private BlockNumInfo checkAndGetBlockNumInfo(String historyDir, String databaseDir)
          throws IOException, RocksDBException {
    logger.info("-- check the compatibility of this history");
    String snapshotInfo = String.format(
            DIR_FORMAT_STRING, databaseDir, File.separator, INFO_FILE_NAME);
    String historyInfo = String.format(
            DIR_FORMAT_STRING, historyDir, File.separator, INFO_FILE_NAME);
    if (!FileUtil.isExists(snapshotInfo)) {
      throw new FileNotFoundException(
              "snapshot property file is not found. maybe this is a complete fullnode?");
    }
    if (!FileUtil.isExists(historyInfo)) {
      throw new FileNotFoundException("history property file is not found.");
    }
    long snapshotBlkNum = Long.parseLong(PropUtil.readProperty(snapshotInfo, Constant
            .SPLIT_BLOCK_NUM));
    long historyBlkNum = Long.parseLong(PropUtil.readProperty(historyInfo, Constant
            .SPLIT_BLOCK_NUM));
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
    logger.info("-- backup the archive dbs to {}", bakDir);
    if (!FileUtil.createDirIfNotExists(bakDir)) {
      throw new RuntimeException("create bak dir failed");
    }
    Util.copyDatabases(Paths.get(databaseDir), Paths.get(bakDir), archiveDbs);
    archiveDbs.forEach(db -> FileUtil.deleteDir(new File(databaseDir, db)));
  }

  private void copyHistory2Database(String historyDir, String databaseDir) throws IOException {
    logger.info("-- begin to copy history to database");
    Util.copyDatabases(Paths.get(historyDir), Paths.get(databaseDir), archiveDbs);
  }

  private void trimHistory(String databaseDir, BlockNumInfo blockNumInfo)
          throws BadItemException, IOException, RocksDBException {
    logger.info("-- begin to trim the history data.");
    TronDB blockIndexDb = DbTool.getDB(databaseDir, BLOCK_INDEX_DB_NAME);
    TronDB blockDb = DbTool.getDB(databaseDir, BLOCK_DB_NAME);
    TronDB transDb = DbTool.getDB(databaseDir, "trans");
    TronDB tranRetDb = DbTool.getDB(databaseDir, "transactionRetStore");
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
    logger.info("-- begin to merge {} to database", bakDir);
    for (String dbName : archiveDbs) {
      TronDB bakDb = DbTool.getDB(bakDir, dbName);
      TronDB destDb = DbTool.getDB(databaseDir, dbName);
      try (DBIterator iterator = bakDb.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          destDb.put(iterator.getKey(), iterator.getValue());
        }
      }
    }
  }

  private void deleteSnapshotFlag(String databaseDir) throws IOException {
    logger.info("-- delete the info file to identify this node is a real fullnode.");
    Files.delete(Paths.get(databaseDir, INFO_FILE_NAME));
  }

  private void initTmpDb(String sourceDir) throws IOException, RocksDBException {
    DbTool.getDB(sourceDir, "tmp");
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
          generateSnapshot(argv.fnDataPath, argv.datasetPath, argv.check);
        } else if (HISTORY_DIR_NAME.equals(argv.type)) {
          generateHistory(argv.fnDataPath, argv.datasetPath);
        } else {
          throw new ParameterException("not support type:" + argv.type);
        }
        break;
      case "merge":
        completeHistoryData(argv.datasetPath, argv.fnDataPath);
        break;
      case "check":
        if (SNAPSHOT_DIR_NAME.equals(argv.type)) {
          checkSnapshot(argv.fnDataPath, argv.datasetPath);
        } else {
          throw new ParameterException("not support type:" + argv.type);
        }
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
      logger.error("{}", e);
      jct.usage();
    }
  }

  static class Args {
    @Parameter(
            names = {"--operate", "-o"},
            help = true, required = true,
            description = "operate: [ split | merge | check],check only support snapshot",
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
    @Parameter(
        names = "--check",
        help = true,
        description = "if check the destDir data when type is snapshot && operate is split",
        order = 6)
    private boolean check;
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



