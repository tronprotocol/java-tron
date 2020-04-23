package org.tron.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.MarketPriceLinkedListCapsule;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.pojo.AssetIssue;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.protos.Protocol.MarketPrice;

@Slf4j
public class MarketPairToPriceStoreTest {

  private static final String dbPath = "output-MarketPairToPriceStore-test";
  private static TronApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  // MarketPairToPriceStore store;

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  // @Before
  // public void initDb() {
  //   // this.store = context.getBean(MarketPairToPriceStore.class);
  //   ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
  //   this.store = chainBaseManager.getMarketPairToPriceStore();
  // }

  @After
  public void cleanDb() {
    System.out.println("cleanDb");
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    marketPairToPriceStore.forEach(
        bytesCapsuleEntry -> marketPairToPriceStore.delete(bytesCapsuleEntry.getKey())
    );
  }

  @Test
  public void testOrderWithSamePair() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();

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

    BytesCapsule capsule1 = new BytesCapsule(ByteArray.fromLong(1));
    BytesCapsule capsule2 = new BytesCapsule(ByteArray.fromLong(2));
    BytesCapsule capsule3 = new BytesCapsule(ByteArray.fromLong(3));

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairToPriceStore.put(pairPriceKey2, capsule2);
    marketPairToPriceStore.put(pairPriceKey1, capsule1);
    marketPairToPriceStore.put(pairPriceKey3, capsule3);

    Assert
        .assertArrayEquals(capsule1.getData(), marketPairToPriceStore.get(pairPriceKey1).getData());
    Assert
        .assertArrayEquals(capsule2.getData(), marketPairToPriceStore.get(pairPriceKey2).getData());
    Assert
        .assertArrayEquals(capsule3.getData(), marketPairToPriceStore.get(pairPriceKey3).getData());


    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairToPriceStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testOrderWithSamePairOrdinal() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();

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

    BytesCapsule capsule1 = new BytesCapsule(ByteArray.fromLong(1));
    BytesCapsule capsule2 = new BytesCapsule(ByteArray.fromLong(2));
    BytesCapsule capsule3 = new BytesCapsule(ByteArray.fromLong(3));

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairToPriceStore.put(pairPriceKey1, capsule1);
    marketPairToPriceStore.put(pairPriceKey2, capsule2);
    marketPairToPriceStore.put(pairPriceKey3, capsule3);

    Assert
        .assertArrayEquals(capsule1.getData(), marketPairToPriceStore.get(pairPriceKey1).getData());
    Assert
        .assertArrayEquals(capsule2.getData(), marketPairToPriceStore.get(pairPriceKey2).getData());
    Assert
        .assertArrayEquals(capsule3.getData(), marketPairToPriceStore.get(pairPriceKey3).getData());

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairToPriceStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testAddPrice() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();

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

    BytesCapsule capsule1 = new BytesCapsule(ByteArray.fromLong(1));
    BytesCapsule capsule2 = new BytesCapsule(ByteArray.fromLong(2));
    BytesCapsule capsule3 = new BytesCapsule(ByteArray.fromLong(3));

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairToPriceStore.put(pairPriceKey1, capsule1);
    marketPairToPriceStore.put(pairPriceKey2, capsule2);
    marketPairToPriceStore.put(pairPriceKey3, capsule3);

    Assert
        .assertArrayEquals(capsule1.getData(), marketPairToPriceStore.get(pairPriceKey1).getData());
    Assert
        .assertArrayEquals(capsule2.getData(), marketPairToPriceStore.get(pairPriceKey2).getData());
    Assert
        .assertArrayEquals(capsule3.getData(), marketPairToPriceStore.get(pairPriceKey3).getData());

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairToPriceStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testAddPriceAndHeadKey() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();

    byte[] sellTokenID1 = ByteArray.fromString("100");
    byte[] buyTokenID1 = ByteArray.fromString("200");
    byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        0L,
        0L
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

    BytesCapsule capsule1 = new BytesCapsule(ByteArray.fromLong(1));
    BytesCapsule capsule2 = new BytesCapsule(ByteArray.fromLong(2));
    BytesCapsule capsule3 = new BytesCapsule(ByteArray.fromLong(3));

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairToPriceStore.put(pairPriceKey1, capsule1);
    Assert
        .assertArrayEquals(capsule1.getData(), marketPairToPriceStore.get(pairPriceKey1).getData());
    marketPairToPriceStore.put(pairPriceKey2, capsule2);
    Assert
        .assertArrayEquals(capsule2.getData(), marketPairToPriceStore.get(pairPriceKey2).getData());
    marketPairToPriceStore.put(pairPriceKey3, capsule3);
    Assert
        .assertArrayEquals(capsule3.getData(), marketPairToPriceStore.get(pairPriceKey3).getData());

    Assert
        .assertArrayEquals(capsule1.getData(), marketPairToPriceStore.get(pairPriceKey1).getData());
    Assert
        .assertArrayEquals(capsule2.getData(), marketPairToPriceStore.get(pairPriceKey2).getData());
    Assert
        .assertArrayEquals(capsule3.getData(), marketPairToPriceStore.get(pairPriceKey3).getData());

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairToPriceStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testOrderWithDifferentPair() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();

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

    // System.out.println("pairPriceKey1 ："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey21："+ByteArray.toHexString(pairPriceKey21));
    // System.out.println("pairPriceKey31："+ByteArray.toHexString(pairPriceKey31));

    // Use out-of-order insertion
    marketPairToPriceStore.putPrice(pairPriceKey21);
    marketPairToPriceStore.putPrice(pairPriceKey1);
    marketPairToPriceStore.putPrice(pairPriceKey31);

    byte[] nextKey = marketPairToPriceStore.getNextKey(pairPriceKey21);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));
    // System.out.println("pairPriceKey31："+ByteArray.toHexString(pairPriceKey31));
    Assert.assertArrayEquals("nextKey should be pairPriceKey31", pairPriceKey31, nextKey);

  }

  @Test
  public void testDecodePriceKey() {
    long sellTokenQuantity = 1000L;
    long buyTokenQuantity = 2001L;

    byte[] sellTokenID1 = ByteArray.fromString("100");
    byte[] buyTokenID1 = ByteArray.fromString("199");
    byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        sellTokenQuantity,
        buyTokenQuantity
    );
    Assert.assertEquals(54, pairPriceKey1.length);

    MarketPrice marketPrice = MarketUtils.decodeKeyToMarketPrice(pairPriceKey1);
    Assert.assertEquals(sellTokenQuantity, marketPrice.getSellTokenQuantity());
    Assert.assertEquals(buyTokenQuantity, marketPrice.getBuyTokenQuantity());
  }

  @Test
  public void testPriceWithSamePair() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairToPriceStore marketPairToPriceStore = chainBaseManager.getMarketPairToPriceStore();

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

    Assert.assertEquals(0, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairToPriceStore.addPriceKey(sellTokenID1, buyTokenID1, 1000L, 2002L);
    Assert.assertEquals(1, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    marketPairToPriceStore.addPriceKey(sellTokenID1, buyTokenID1, 1000L, 2001L);
    Assert.assertEquals(2, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    marketPairToPriceStore.addPriceKey(sellTokenID1, buyTokenID1, 1000L, 2003L);
    Assert.assertEquals(3, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    MarketPriceLinkedListCapsule capsule = new MarketPriceLinkedListCapsule(
        sellTokenID1, buyTokenID1);

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairToPriceStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);
  }

}