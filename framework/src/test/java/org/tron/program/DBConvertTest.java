package org.tron.program;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.capsule.utils.MarketUtils;

public class DBConvertTest {


  private static final String INPUT_DIRECTORY = "output-directory/convert-database/";
  private static final String OUTPUT_DIRECTORY = "output-directory/convert-database-dest/";
  private static final String ACCOUNT = "account";
  private static final String MARKET = "market_pair_price_to_order";


  @BeforeClass
  public static void init() throws IOException {
    if (new File(INPUT_DIRECTORY).mkdirs()) {
      initDB(new File(INPUT_DIRECTORY,ACCOUNT));
      initDB(new File(INPUT_DIRECTORY,MARKET));
    }
  }

  private static void initDB(File file) throws IOException {
    Options dbOptions = DBConvert.newDefaultLevelDbOptions();
    if (file.getName().contains("market_pair_price_to_order")) {
      dbOptions.comparator(new MarketOrderPriceComparatorForLevelDB());
      try (DB db = factory.open(file,dbOptions)) {

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

        MarketOrderIdListCapsule capsule1 = new MarketOrderIdListCapsule(ByteArray.fromLong(1),
            ByteArray.fromLong(1));
        MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
            ByteArray.fromLong(2));
        MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
            ByteArray.fromLong(3));

        //Use out-of-order insertion，key in store should be 1,2,3
        db.put(pairPriceKey1, capsule1.getData());
        db.put(pairPriceKey2, capsule2.getData());
        db.put(pairPriceKey3, capsule3.getData());
      }

    } else {
      try (DB db = factory.open(file,dbOptions)) {
        for (int i = 0; i < 100; i++) {
          byte[] bytes = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
          db.put(bytes, bytes);
        }
      }
    }

  }

  @AfterClass
  public static void destroy() {
    FileUtil.deleteDir(new File(INPUT_DIRECTORY));
    FileUtil.deleteDir(new File(OUTPUT_DIRECTORY));
  }

  @Test
  public void testRun() {
    String[] args = new String[] { INPUT_DIRECTORY, OUTPUT_DIRECTORY };
    Assert.assertEquals(0, DBConvert.run(args));
  }

  @Test
  public void testNotExist() {
    String[] args = new String[] {OUTPUT_DIRECTORY + File.separator + UUID.randomUUID(),
        OUTPUT_DIRECTORY};
    Assert.assertEquals(404, DBConvert.run(args));
  }

  @Test
  public void testEmpty() {
    File file = new File(OUTPUT_DIRECTORY + File.separator + UUID.randomUUID());
    file.mkdirs();
    file.deleteOnExit();
    String[] args = new String[] {file.toString(), OUTPUT_DIRECTORY};
    Assert.assertEquals(0, DBConvert.run(args));
  }
}
