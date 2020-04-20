package org.tron.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.MarketPriceLinkedListCapsule;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.store.MarketPairToPriceStore;

@Slf4j
public class MarketPairToPriceStoreTest {

  private static final String dbPath = "output-MarketPairToPriceStore-test";
  private static TronApplicationContext context;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  MarketPairToPriceStore store;

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Before
  public void initDb() {
    this.store = context.getBean(MarketPairToPriceStore.class);
  }

  @Test
  public void testOrderWithSamePair() {
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

    MarketPriceLinkedListCapsule capsule = new MarketPriceLinkedListCapsule(
        sellTokenID1, buyTokenID1);

//    System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
//    System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
//    System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    this.store.put(pairPriceKey2, capsule);
    this.store.put(pairPriceKey1, capsule);
    this.store.put(pairPriceKey3, capsule);

//    Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
//    while (iterator.hasNext()){
//      System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
//    }

    byte[] nextKey = this.store.getNextKey(pairPriceKey2);
//    System.out.println("nextKey："+ByteArray.toHexString(nextKey));

   Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

  }

  @Test
  public void testOrderWithDifferentPair() {
    byte[] sellTokenID1 = ByteArray.fromString("100");
    byte[] buyTokenID1 = ByteArray.fromString("199");
    byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        1000L,
        2001L
    );

    byte[] sellTokenID2 = ByteArray.fromString("100");
    byte[] buyTokenID2 = ByteArray.fromString("200");
    byte[] pairPriceKey21 = MarketUtils.createPairPriceKey(
        sellTokenID2,
        buyTokenID2,
        1000L,
        2001L
    );
    byte[] sellTokenID3 = ByteArray.fromString("100");
    byte[] buyTokenID3 = ByteArray.fromString("201");
    byte[] pairPriceKey31 = MarketUtils.createPairPriceKey(
        sellTokenID3,
        buyTokenID3,
        1000L,
        2001L
    );

    MarketPriceLinkedListCapsule capsule = new MarketPriceLinkedListCapsule(
        sellTokenID1, buyTokenID1);

//    System.out.println("pairPriceKey1 ："+ByteArray.toHexString(pairPriceKey1));
//    System.out.println("pairPriceKey21："+ByteArray.toHexString(pairPriceKey21));
//    System.out.println("pairPriceKey31："+ByteArray.toHexString(pairPriceKey31));

    //Use out-of-order insertion
    this.store.put(pairPriceKey21, capsule);
    this.store.put(pairPriceKey1, capsule);
    this.store.put(pairPriceKey31, capsule);

//    Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
//    while (iterator.hasNext()){
//      System.out.println( ByteArray.toHexString(iterator.next().getKey()));
//    }

    byte[] nextKey = this.store.getNextKey(pairPriceKey21);
//    System.out.println("nextKey："+ByteArray.toHexString(nextKey));
//    System.out.println("pairPriceKey31："+ByteArray.toHexString(pairPriceKey31));
    Assert.assertArrayEquals("nextKey should be pairPriceKey31", pairPriceKey31, nextKey);

  }


}