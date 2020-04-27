package org.tron.core.db;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.protos.Protocol.MarketPrice;

@Slf4j
public class MarketPairPriceToOrderStoreTest {

  private static final String dbPath = "output-MarketPairPriceToOrderStore-test";
  private static TronApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  // MarketPairPriceToOrderStore store;

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
  //   // this.store = context.getBean(MarketPairPriceToOrderStore.class);
  //   ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
  //   this.store = chainBaseManager.getMarketPairPriceToOrderStore();
  // }

  @After
  public void cleanDb() {
    System.out.println("======== cleanDb ========");
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();
    MarketPairToPriceStore marketPairToPriceStore = dbManager.getChainBaseManager()
        .getMarketPairToPriceStore();

    marketPairPriceToOrderStore.forEach(
        entry -> marketPairPriceToOrderStore.delete(entry.getKey())
    );
    marketPairToPriceStore.forEach(
        entry -> marketPairToPriceStore.delete(entry.getKey())
    );
  }

  @Test
  public void testOrderWithSamePair() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

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


    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);

    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }


    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testOrderWithSamePairOrdinal() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager.getMarketPairPriceToOrderStore();

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

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);

    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testAddPrice() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager.getMarketPairPriceToOrderStore();

    byte[] sellTokenID1 = ByteArray.fromString("100");
    byte[] buyTokenID1 = ByteArray.fromString("200");
    byte[] pairPriceKey0 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        0L,
        0L
    );
    byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        1L,
        1L
    );
    byte[] pairPriceKey2 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        1L,
        2L
    );
    byte[] pairPriceKey3 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        1L,
        3L
    );

    MarketOrderIdListCapsule capsule0 = new MarketOrderIdListCapsule(ByteArray.fromLong(0),
        ByteArray.fromLong(0));
    MarketOrderIdListCapsule capsule1 = new MarketOrderIdListCapsule(ByteArray.fromLong(1),
        ByteArray.fromLong(1));
    MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
        ByteArray.fromLong(2));
    MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
        ByteArray.fromLong(3));

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    marketPairPriceToOrderStore.put(pairPriceKey0, capsule0);

    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    List<byte[]> keyList = marketPairPriceToOrderStore.getKeysNext(pairPriceKey0, 4);
    Assert.assertArrayEquals(pairPriceKey0, keyList.get(0));
    Assert.assertArrayEquals(pairPriceKey1, keyList.get(1));
    Assert.assertArrayEquals(pairPriceKey2, keyList.get(2));
    Assert.assertArrayEquals(pairPriceKey3, keyList.get(3));

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testAddPriceWithoutHeadKey() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager.getMarketPairPriceToOrderStore();

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

    MarketOrderIdListCapsule capsule1 = new MarketOrderIdListCapsule(ByteArray.fromLong(1),
        ByteArray.fromLong(1));
    MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
        ByteArray.fromLong(2));
    MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
        ByteArray.fromLong(3));

    // System.out.println("pairPriceKey1："+ByteArray.toHexString(pairPriceKey1));
    // System.out.println("pairPriceKey2："+ByteArray.toHexString(pairPriceKey2));
    // System.out.println("pairPriceKey3："+ByteArray.toHexString(pairPriceKey3));

    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    System.out.println("--- put 1000 2002 ---");
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    System.out.println("--- put 1000 2002 done ---");
    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    System.out.println("--- check has pairPriceKey2 ---");
    Assert.assertEquals(true, marketPairPriceToOrderStore.has(pairPriceKey2));
    System.out.println("--- check has pairPriceKey1 ---");
    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey3));

    System.out.println("--- put 1000 2003 ---");
    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    try {
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertEquals(true, marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertEquals(true, marketPairPriceToOrderStore.has(pairPriceKey3));

    // Assert
    //     .assertArrayEquals(capsule1.getData(), marketPairPriceToOrderStore.get(pairPriceKey1).getData());
    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
  }

  @Test
  public void testAddPriceAndHeadKey() {
    System.out.println("testOrderWithSamePair");

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager.getMarketPairPriceToOrderStore();

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

    MarketOrderIdListCapsule capsule1 = new MarketOrderIdListCapsule(ByteArray.fromLong(1),
        ByteArray.fromLong(1));
    MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
        ByteArray.fromLong(2));
    MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
        ByteArray.fromLong(3));

    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertEquals(false, marketPairPriceToOrderStore.has(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    System.out.println("--- put 0 0 ---");
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    System.out.println("--- put 1000 2002 ---");
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    System.out.println("--- put 1000 2002 done ---");
    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    System.out.println("--- put 1000 2003 ---");
    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    try {
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());

      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.assertTrue(false);
    }

    // Iterator<Entry<byte[], MarketPriceLinkedListCapsule>> iterator = this.store.iterator();
    // while (iterator.hasNext()){
    //   System.out.println("keys:" + ByteArray.toHexString(iterator.next().getKey()));
    // }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    System.out.println("testOrderWithSamePair end");
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
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();
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
    marketPairPriceToOrderStore.put(pairPriceKey1, new MarketOrderIdListCapsule());
    marketPairToPriceStore.addNewPriceKey(sellTokenID1, buyTokenID1, marketPairPriceToOrderStore);
    Assert.assertEquals(1, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    marketPairPriceToOrderStore.put(pairPriceKey2, new MarketOrderIdListCapsule());
    marketPairToPriceStore.addNewPriceKey(sellTokenID1, buyTokenID1, marketPairPriceToOrderStore);
    Assert.assertEquals(2, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    marketPairPriceToOrderStore.put(pairPriceKey3, new MarketOrderIdListCapsule());
    marketPairToPriceStore.addNewPriceKey(sellTokenID1, buyTokenID1, marketPairPriceToOrderStore);
    Assert.assertEquals(3, marketPairToPriceStore.getPriceNum(sellTokenID1, buyTokenID1));

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // System.out.println("nextKey："+ByteArray.toHexString(nextKey));

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);
  }

}