package org.tron.tool.litefullnode;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.typesafe.config.Config;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.exception.BadItemException;
import org.tron.tool.litefullnode.db.DBInterface;
import org.tron.tool.litefullnode.iterator.DBIterator;

public class LiteFullNodeTool {

  private static final Args INSTANCE = new Args();
  private static final String SNAPSHOT_DIR_NAME = "snapshot";
  private static final String HISTORY_DIR_NAME = "history";
  private static final String INFO_FILE_NAME = "info.properties";
  private static final String SPLIT_BLOCK_NUM = "split_block_num";
  private static final String BACKUP_DIR_PREFIX = ".bak_";

  private static final long START_TIME = System.currentTimeMillis() / 1000;

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
          //  "common",
          "contract",
          "delegation",
          "exchange",
          "exchange-v2",
          //    "nullifier",
          "properties",
          "proposal",
          "recent-block",
          "storage-row",
          "trans-cache",
          //     "tree-block-index",
          "votes",
          "witness",
          "witness_schedule"
          //    "zkProof"
  );
  private static final String checkpointDb = "tmp";

  //private static final String sourceDir = "/Users/quan/tron/java-tron/build/distributions/java-tron-1.0.0/bin/output-directory/database";
  //private static final String sourceDir = "/Users/quan/tron/java-tron/output-directory/database_bak";
  //  private static final String destDir = "/Users/quan/tron/java-tron/build/distributions/java-tron-1.0.0/bin/output-directory/";
  //private static final String destDir = "/Users/quan/tron/java-tron/output-directory";

  public static void generateSnapshot(String sourceDir, String snapshotDir) throws IOException, RocksDBException {
    snapshotDir = Paths.get(snapshotDir, SNAPSHOT_DIR_NAME).toString();
    split(sourceDir, snapshotDir, snapshotDbs);
    mergeCheckpoint2Snapshot(sourceDir, snapshotDir);
    generateInfoProperties(Paths.get(snapshotDir, INFO_FILE_NAME).toString(), sourceDir);
    // write genesisBlock and latestBlock
    if (!fillSnapshotBlockDb(sourceDir, snapshotDir)) {
      throw new RuntimeException("create snapshot block db failed, exit...");
    }
  }

  public static void generateHistory(String sourceDir, String historyDir) throws IOException, RocksDBException {
    historyDir = Paths.get(historyDir, HISTORY_DIR_NAME).toString();
    split(sourceDir, historyDir, archiveDbs);
    mergeCheckpoint2History(sourceDir, historyDir);
    generateInfoProperties(Paths.get(historyDir, INFO_FILE_NAME).toString(), sourceDir);
  }

  public static void completeHistoryData(String historyDir, String databaseDir) throws IOException, RocksDBException, BadItemException {
    // 1. check block number and genesis block are compatible,
    //    and return the block numbers of snapshot and history
    BlockNumInfo blockNumInfo = checkAndGetBlockNumInfo(historyDir, databaseDir);
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
  }

  private static void mergeCheckpoint2Snapshot(String sourceDir, String historyDir) {
    mergeCheckpoint(sourceDir, historyDir, snapshotDbs);
  }

  private static void mergeCheckpoint2History(String sourceDir, String destDir) {
    mergeCheckpoint(sourceDir, destDir, archiveDbs);
  }

  private static void split(String sourceDir, String destDir, List<String> dbs) throws IOException {
    if (!new File(sourceDir).isDirectory()) {
      throw new RuntimeException("sourceDir must be a directory");
    }
    File destPath = new File(destDir);
    if (new File(destDir).exists()) {
      throw new RuntimeException("destDir is already exist, please remove it first");
    }
    if (!destPath.mkdir()) {
      throw new RuntimeException("destDir create failed, please check");
    }
    Util.copyFolder(Paths.get(sourceDir), Paths.get(destDir), dbs);
  }

  private static void mergeCheckpoint(String sourceDir, String destDir, List<String> destDbs) {
    System.out.println("start");
    try {
      DBInterface tmpDb = DbTool.openDB(sourceDir, checkpointDb);
      try (DBIterator iterator = tmpDb.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          byte[] key = iterator.getKey();
          byte[] value = iterator.getValue();
          String dbName = SnapshotManager.simpleDecode(key);
          byte[] realKey = Arrays.copyOfRange(key, dbName.getBytes().length + 4, key.length);
          byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
          //System.out.println("db: " + db);
          //System.out.println("realKey: " + new String(realKey));
          //System.out.println("realValue: " + new String(realValue));
          //break;
          if (destDbs != null && destDbs.contains(dbName)) {
            DBInterface destDb = DbTool.openDB(destDir, dbName);
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
    System.out.println("end");
  }

  private static void generateInfoProperties(String propertyfile, String databaseDir) throws IOException, RocksDBException {
    if (!FileUtil.createFileIfNotExists(propertyfile)) {
      throw new RuntimeException("create properties file failed...");
    }
    if (!PropUtil.writeProperty(propertyfile, SPLIT_BLOCK_NUM, Long.toString(getLatestBlockHeaderNum(databaseDir)))) {
      throw new RuntimeException("write properties file failed...");
    }
  }

  private static long getLatestBlockHeaderNum(String databaseDir) throws IOException, RocksDBException {
    // query latest_block_header_number from checkpoint first
    final String latestBlockHeaderNumber = "latest_block_header_number";
    byte[] value = DbTool.openDB(databaseDir, checkpointDb).get(
            Bytes.concat(simpleEncode(checkpointDb), latestBlockHeaderNumber.getBytes()));
    if (value != null && value.length > 1) {
      return ByteArray.toLong(Arrays.copyOfRange(value, 1, value.length));
    }
    // query from propertiesDb if checkpoint not contains latest_block_header_number
    DBInterface propertiesDb = DbTool.openDB(databaseDir, "properties");
    long latestBlockNum = Optional.ofNullable(propertiesDb.get(ByteArray.fromString(latestBlockHeaderNumber)))
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found latest block header number"));
    System.out.println(latestBlockNum);
    return latestBlockNum;
  }

  /**
   * Syncing block from peer needs latest block and genesis block,
   * so put them into snapshot.
   */
  private static boolean fillSnapshotBlockDb(String sourceDir, String snapshotDir) {
    try {
      DBInterface sourceBlockIndexDb = DbTool.openDB(sourceDir, "block-index");
      DBInterface sourceBlockDb = DbTool.openDB(sourceDir, "block");
      DBInterface destBlockDb = DbTool.openDB(snapshotDir, "block");
      DBInterface destBlockIndexDb = DbTool.openDB(snapshotDir, "block-index");
      // put genesis block and block-index into snapshot
      long genesisBlockNum = 0L;
      byte[] genesisBlockID = sourceBlockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
      destBlockIndexDb.put(ByteArray.fromLong(genesisBlockNum), genesisBlockID);
      destBlockDb.put(genesisBlockID, sourceBlockDb.get(genesisBlockID));

      long latestBlockNum = getLatestBlockHeaderNum(sourceDir);
      byte[] latestBlockId = sourceBlockIndexDb.get(ByteArray.fromLong(latestBlockNum));
      // put latest block index into snapshot
      destBlockIndexDb.put(ByteArray.fromLong(latestBlockNum), latestBlockId);
      // put latest block into snapshot
      destBlockDb.put(latestBlockId, sourceBlockDb.get(latestBlockId));
      return true;
    } catch (IOException | RocksDBException e) {
      e.printStackTrace();
      return false;
    }
  }

  private static byte[] getGenesisBlockHash(String parentDir) throws IOException, RocksDBException {
    long genesisBlockNum = 0L;
    DBInterface blockIndexDb = DbTool.openDB(parentDir, "block-index");
    return blockIndexDb.get(ByteArray.fromLong(genesisBlockNum));
  }

  private static byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }


  private static BlockNumInfo checkAndGetBlockNumInfo(String historyDir, String databaseDir)
          throws IOException, RocksDBException {
    String snapshotInfo = String.format("%s%s%s", databaseDir, File.separator, INFO_FILE_NAME);
    String historyInfo = String.format("%s%s%s", historyDir, File.separator, INFO_FILE_NAME);
    if (!FileUtil.isExists(snapshotInfo)) {
      throw new FileNotFoundException("snapshot property file is not found. maybe this is a complete fullnode?");
    }
    if (!FileUtil.isExists(historyInfo)) {
      throw new FileNotFoundException("history property file is not found.");
    }
    long snapshotBlkNum = Long.parseLong(PropUtil.readProperty(snapshotInfo, SPLIT_BLOCK_NUM));
    long historyBlkNum = Long.parseLong(PropUtil.readProperty(historyInfo, SPLIT_BLOCK_NUM));
    if (historyBlkNum < snapshotBlkNum) {
      throw new RuntimeException("history does not have the total data that snapshot need.");
    }
    // check genesis block is equal
    System.out.println("genesis block: ");
    System.out.println(getGenesisBlockHash(databaseDir));
    System.out.println(getGenesisBlockHash(historyDir));
    if (!Arrays.equals(getGenesisBlockHash(databaseDir), getGenesisBlockHash(historyDir))) {
      throw new RuntimeException("genesis block is not equal.");
    }
    return new BlockNumInfo(snapshotBlkNum, historyBlkNum);
  }

  private static void backupArchiveDbs(String databaseDir) throws IOException {
    String bakDir = String.format("%s%s%s%d", databaseDir, File.separator, BACKUP_DIR_PREFIX, START_TIME);
    System.out.printf("backup: back archive dbs to %s \n", bakDir);
    if (!FileUtil.createDirIfNotExists(bakDir)) {
      throw new RuntimeException("create bak dir failed");
    }
    Util.copyFolder(Paths.get(databaseDir), Paths.get(bakDir), archiveDbs);
    archiveDbs.stream().forEach(db -> {
      FileUtil.deleteDir(new File(databaseDir, db));
    });
    System.out.println("backup finished.");
  }

  private static void copyHistory2Database(String historyDir, String databaseDir) throws IOException {
    System.out.println("start copy history data to fullnode database");
    Util.copyFolder(Paths.get(historyDir), Paths.get(databaseDir), archiveDbs);
    System.out.println("history data copy finished");
  }

  private static void trimHistory(String databaseDir, BlockNumInfo blockNumInfo)
          throws BadItemException, IOException, RocksDBException {
    DBInterface blockIndexDb = DbTool.openDB(databaseDir, "block-index");
    DBInterface blockDb = DbTool.openDB(databaseDir, "block");
    DBInterface transDb = DbTool.openDB(databaseDir, "trans");
    DBInterface tranRetDb = DbTool.openDB(databaseDir, "transactionRetStore");
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

  private static void mergeBak2Database(String databaseDir) throws IOException, RocksDBException {
    String bakDir = String.format("%s%s%s%d", databaseDir, File.separator, BACKUP_DIR_PREFIX, START_TIME);
    for (String dbName : archiveDbs) {
      DBInterface bakDb = DbTool.openDB(bakDir, dbName);
      DBInterface destDb = DbTool.openDB(databaseDir, dbName);
      try (DBIterator iterator = bakDb.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          destDb.put(iterator.getKey(), iterator.getValue());
        }
      }
    }
  }

  private static void deleteSnapshotFlag(String databaseDir) throws IOException {
    Files.delete(Paths.get(databaseDir, INFO_FILE_NAME));
  }


  public static void main(String[] args) {
    Args argv = new Args();
    JCommander jct = JCommander.newBuilder()
            .addObject(argv)
            .build();
    jct.setProgramName("lite fullnode tool");
    try {
      jct.parse(args);
      if (argv.help) {
        jct.usage();
      } else {
        run(argv);
      }
    } catch (ParameterException parameterException) {
      System.out.print(parameterException.toString() + "\r\n");
      jct.usage();
    }
  }

  private static void run(Args argv) {
    try {
      switch (argv.operate) {
        case "split":
          if (argv.type.equals("snapshot")) {
            System.out.println(argv.fnDataPath);
            System.out.println(argv.datasetPath);
            generateSnapshot(argv.fnDataPath, argv.datasetPath);
            break;
          } else if (argv.type.equals("history")) {
            System.out.println(argv.fnDataPath);
            System.out.println(argv.datasetPath);
            generateHistory(argv.fnDataPath, argv.datasetPath);
            break;
          } else {
            throw new ParameterException("type can't be null when operate=split or not support type:" + argv.type);
          }
        case "merge":
          System.out.println(argv.fnDataPath);
          System.out.println(argv.datasetPath);
          completeHistoryData(argv.datasetPath, argv.fnDataPath);
          break;
        default:
          throw new ParameterException("not supportted operate:" + argv.operate);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RocksDBException e) {
      e.printStackTrace();
    } catch (BadItemException e) {
      e.printStackTrace();
    }
  }

  static class Args {
    @Parameter(names = {"--operate", "-o"}, help = true, required = true, description = "operate: [split | merge]", order = 1)
    String operate;
    @Parameter(names = {"--type", "-t"}, help = true, description = "only used with operate=split: [snapshot | history]", order = 2)
    String type;
    @Parameter(names = {"--fn-data-path"}, help = true, required = true, description = "the fullnode database path, defined as ${storage.db.directory} in config.conf", order = 3)
    String fnDataPath;
    @Parameter(names = {"--dataset-path"}, help = true, required = true, description = "dataset directory, when operation is `split`, `dataset-path` is the path that store the `Snapshot Dataset` or `History Dataset`," +
            " otherwise `dataset-path` should be the `History Dataset` path", order = 4)
    String datasetPath;
    @Parameter(names = "--help", help = true, order = 5)
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



