package org.tron.tool.litefullnode;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.typesafe.config.Config;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.config.Configuration;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.tool.litefullnode.db.DBInterface;
import org.tron.tool.litefullnode.iterator.DBIterator;

public class LiteFullNodeTool {

  private static final Args INSTANCE = new Args();
  private static final String SNAPSHOT_DIR_NAME = "snapshot";
  private static final String HISTORY_DIR_NAME = "history";
  private static final String INFO_FILE_NAME = "info.properties";

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
          "common",
          "contract",
          "delegation",
          "exchange",
          "exchange-v2",
          "nullifier",
          "properties",
          "proposal",
          "recent-block",
          "storage-row",
          "tree-block-index",
          "votes",
          "witness",
          "witness_schedule",
          "zkProof"
  );
  private static final String checkpointDb = "tmp";

  private static final String sourceDir = "/Users/quan/tron/java-tron/build/distributions/java-tron-1.0.0/bin/output-directory/database";
  private static final String destDir = "/Users/quan/tron/java-tron/build/distributions/java-tron-1.0.0/bin/output-directory/";

  public static void generateSnapshot(String sourceDir, String destDir) throws IOException, RocksDBException {
    destDir = Paths.get(destDir, SNAPSHOT_DIR_NAME).toString();
    split(sourceDir, destDir, snapshotDbs);
    mergeCheckpoint2Snapshot();
    generateInfoProperties(Paths.get(destDir, INFO_FILE_NAME).toString());
  }

  public static void generateHistory(String sourceDir, String destDir) throws IOException, RocksDBException {
    destDir = Paths.get(destDir, HISTORY_DIR_NAME).toString();
    split(sourceDir, destDir, archiveDbs);
    mergeCheckpoint2History();
    generateInfoProperties(Paths.get(destDir, INFO_FILE_NAME).toString());
  }

  private static void mergeCheckpoint2Snapshot() {
    mergeCheckpoint(SNAPSHOT_DIR_NAME, snapshotDbs);
  }

  private static void mergeCheckpoint2History() {
    mergeCheckpoint(HISTORY_DIR_NAME, archiveDbs);
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

  private static void mergeCheckpoint(String destDirName, List<String> destDbs) {
    System.out.println("start");
    try {
      DBInterface levelDb = DbTool.openDB(sourceDir, checkpointDb);
      DBIterator iterator = levelDb.iterator();
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        byte[] key = iterator.getKey();
        byte[] value = iterator.getValue();
        String db = SnapshotManager.simpleDecode(key);
        byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);
        byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
        System.out.println("db: " + db);
        System.out.println("realKey: " + new String(realKey));
        //System.out.println("realValue: " + new String(realValue));
        //break;
        if (destDbs != null && destDbs.contains(db)) {
          DBInterface blockDb = DbTool.openDB(
                  String.format("%s%s%s", destDir, File.separator, destDirName), db);
          if (realValue != null) {
            blockDb.put(realKey, realValue);
          } else {
            blockDb.delete(realKey);
          }
        }
      }
    } catch (IOException | RocksDBException e) {
      throw new RuntimeException(e);
    }
    System.out.println("end");
  }

  private static void generateInfoProperties(String file) throws IOException, RocksDBException {
    if (!FileUtil.createFileIfNotExists(file)) {
      throw new RuntimeException("create properties file failed...");
    }
    if (!PropUtil.writeProperty(file, "block_hight", Long.toString(getLatestBlockHeaderNum()))) {
      throw new RuntimeException("write properties file failed...");
    }
  }

  private static long getLatestBlockHeaderNum() throws IOException, RocksDBException {
    // first query latest_block_header_number from checkpoint
    final String latestBlockHeaderNumber = "latest_block_header_number";
    byte[] value = DbTool.openDB(sourceDir, checkpointDb).get(
            Bytes.concat(simpleEncode(checkpointDb), latestBlockHeaderNumber.getBytes()));
    if (value != null && value.length > 1) {
      return ByteArray.toLong(Arrays.copyOfRange(value, 1, value.length));
    }
    // query from propertiesDb if checkpoint not contains latest_block_header_number
    DBInterface propertiesDb = DbTool.openDB(sourceDir, "properties");
    long latestBlockNum = Optional.ofNullable(propertiesDb.get(ByteArray.fromString(latestBlockHeaderNumber)))
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found latest block header number"));
    propertiesDb.close();
    System.out.println(latestBlockNum);
    return latestBlockNum;
  }

  private static byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  public static void main(String[] args) throws IOException, RocksDBException {
    /*DB levelDb = openLevelDb(sourceDir, "block-index");
    long blockNum = 1;
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.wrap(levelDb.get(ByteArray.fromLong(blockNum))), blockNum);
    System.out.println(blockId.getString());
    levelDb.close();*/

    generateSnapshot(sourceDir, destDir);
    //generateHistory(sourceDir, destDir);

  }

  public static void initArgs(String[] args) {
    JCommander.newBuilder().addObject(INSTANCE).build().parse(args);
    Config config = Configuration.getByFileName(INSTANCE.shellConfFileName, null);
  }

  static class Args {
    @Parameter(names = {"-c", "--config"}, description = "Config File")
    String shellConfFileName = "";
  }
}



