package org.tron.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.iq80.leveldb.DB;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import org.tron.plugins.utils.MarketUtils;
import picocli.CommandLine;

public class DbConvertTest {


  private static final String INPUT_DIRECTORY = "output-directory/convert-database/";
  private static final String OUTPUT_DIRECTORY = "output-directory/convert-database-dest/";
  private static final String ACCOUNT = "account";
  private static final String MARKET = DBUtils.MARKET_PAIR_PRICE_TO_ORDER;
  CommandLine cli = new CommandLine(new Toolkit());


  @BeforeClass
  public static void init() throws IOException {
    if (new File(INPUT_DIRECTORY).mkdirs()) {
      initDB(new File(INPUT_DIRECTORY,ACCOUNT));
      initDB(new File(INPUT_DIRECTORY,MARKET));
    }
  }

  private static void initDB(File file) throws IOException {
    try (DB db = DBUtils.newLevelDb(file.toPath())) {
      if (MARKET.equalsIgnoreCase(file.getName())) {
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
          byte[] bytes = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
          db.put(bytes, bytes);
        }
      }
    }
  }

  @AfterClass
  public static void destroy() {
    FileUtils.deleteDir(new File(INPUT_DIRECTORY));
    FileUtils.deleteDir(new File(OUTPUT_DIRECTORY));
  }

  @Test
  public void testRun() {
    String[] args = new String[] { "db", "convert",  INPUT_DIRECTORY, OUTPUT_DIRECTORY };
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testHelp() {
    String[] args = new String[] {"db", "convert", "-h"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testNotExist() {
    String[] args = new String[] {"db", "convert",
        OUTPUT_DIRECTORY + File.separator + UUID.randomUUID(),
        OUTPUT_DIRECTORY};
    Assert.assertEquals(404, cli.execute(args));
  }

  @Test
  public void testEmpty() {
    File file = new File(OUTPUT_DIRECTORY + File.separator + UUID.randomUUID());
    file.mkdirs();
    file.deleteOnExit();
    String[] args = new String[] {"db", "convert", file.toString(), OUTPUT_DIRECTORY};
    Assert.assertEquals(0, cli.execute(args));
  }

}
