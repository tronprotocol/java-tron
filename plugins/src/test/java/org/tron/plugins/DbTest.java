package org.tron.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.MarketUtils;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

public class DbTest {

  public String INPUT_DIRECTORY;
  private static final String ACCOUNT = "account";
  private static final String MARKET = DBUtils.MARKET_PAIR_PRICE_TO_ORDER;
  public CommandLine cli = new CommandLine(new Toolkit());
  String tmpDir = System.getProperty("java.io.tmpdir");

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();


  public void init(DbTool.DbType dbType) throws IOException, RocksDBException {
    INPUT_DIRECTORY = temporaryFolder.newFolder().toString();
    initDB(INPUT_DIRECTORY, ACCOUNT, dbType);
    initDB(INPUT_DIRECTORY, MARKET, dbType);
    initDB(INPUT_DIRECTORY, DBUtils.CHECKPOINT_DB_V2, dbType);
  }

  private static void initDB(String sourceDir, String dbName, DbTool.DbType dbType)
      throws IOException, RocksDBException {
    if (DBUtils.CHECKPOINT_DB_V2.equalsIgnoreCase(dbName)) {
      File dbFile = new File(sourceDir, DBUtils.CHECKPOINT_DB_V2);
      if (dbFile.mkdirs()) {
        for (int i = 0; i < 3; i++) {
          try (DBInterface db = DbTool.getDB(dbFile.getPath(),
              System.currentTimeMillis() + "", dbType)) {
            for (int j = 0; j < 100; j++) {
              byte[] bytes = UUID.randomUUID().toString().getBytes();
              db.put(bytes, bytes);
            }
          }
        }
      }
      return;
    }
    try (DBInterface db =  DbTool.getDB(sourceDir, dbName, dbType)) {
      if (MARKET.equalsIgnoreCase(dbName)) {
        byte[] sellTokenID1 = ByteArray.fromString("100");
        byte[] buyTokenID1 = ByteArray.fromString("200");
        byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
            sellTokenID1,
            buyTokenID1,
            1000L,
            2001L
        );
        byte[] pairPriceKey2 = MarketUtils.createPairPriceKey(
            sellTokenID1,
            buyTokenID1,
            1000L,
            2002L
        );
        byte[] pairPriceKey3 = MarketUtils.createPairPriceKey(
            sellTokenID1,
            buyTokenID1,
            1000L,
            2003L
        );


        //Use out-of-order insertion，key in store should be 1,2,3
        db.put(pairPriceKey1, "1".getBytes(StandardCharsets.UTF_8));
        db.put(pairPriceKey2, "2".getBytes(StandardCharsets.UTF_8));
        db.put(pairPriceKey3, "3".getBytes(StandardCharsets.UTF_8));
      } else {
        for (int i = 0; i < 100; i++) {
          byte[] bytes = UUID.randomUUID().toString().getBytes();
          db.put(bytes, bytes);
        }
      }
    }
  }

  /**
   * Generate a not-exist temporary directory path.
   * @return temporary path
   */
  public String genarateTmpDir() {
    File dir = Paths.get(tmpDir, UUID.randomUUID().toString()).toFile();
    dir.deleteOnExit();
    return dir.getPath();
  }
}
