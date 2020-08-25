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
import org.tron.common.utils.ByteUtil;
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
import org.tron.protos.Protocol.MarketOrderPair;
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

  @After
  public void cleanDb() {
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

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

  @Test
  public void testOrderWithSamePair() {
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
      Assert.fail();
    }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);
  }

  @Test
  public void testOrderWithSamePairOrdinal() {
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
      Assert.fail();
    }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);
  }

  @Test
  public void testAddPrice() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

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
        3L,
        3L
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

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    marketPairPriceToOrderStore.put(pairPriceKey0, capsule0);

    try {
      Assert
          .assertArrayEquals(capsule0.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey0).getData());
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
      Assert.fail();
    }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

    List<byte[]> keyList = marketPairPriceToOrderStore.getKeysNext(pairPriceKey0, 4);
    Assert.assertArrayEquals(pairPriceKey0, keyList.get(0));
    Assert.assertArrayEquals(pairPriceKey1, keyList.get(1));
    Assert.assertArrayEquals(pairPriceKey2, keyList.get(2));
    Assert.assertArrayEquals(pairPriceKey3, keyList.get(3));
  }

  @Test
  public void testAddPriceWithoutHeadKey() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

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

    MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
        ByteArray.fromLong(2));
    MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
        ByteArray.fromLong(3));

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    Assert.assertTrue(marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey3));

    marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    try {
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertTrue(marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertTrue(marketPairPriceToOrderStore.has(pairPriceKey3));

    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);

  }

  @Test
  public void testAddPriceAndHeadKey() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

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

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey1));
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey2));
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey3));

    //Use out-of-order insertion，key in store should be 1,2,3
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);
    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

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
      Assert.fail();
    }

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);
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

    Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey3, nextKey);
  }

  @Test
  public void testPriceSeqWithSamePair() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

    // put order: pairPriceKey2 pairPriceKey1 pairPriceKey3 pairPriceKey0
    // lexicographical order: pairPriceKey0 < pairPriceKey3 < pairPriceKey1 = pairPriceKey2
    // key order: pairPriceKey0 < pairPriceKey1 = pairPriceKey2 < pairPriceKey3
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
        10L,
        21L
    );
    byte[] pairPriceKey2 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        30L,
        63L
    );
    byte[] pairPriceKey3 = MarketUtils.createPairPriceKey(
        sellTokenID1,
        buyTokenID1,
        1L,
        4L
    );

    // lexicographical order: pairPriceKey0 < pairPriceKey3 < pairPriceKey1 = pairPriceKey2
    Assert.assertTrue(ByteUtil.compare(pairPriceKey0, pairPriceKey3) < 0);
    Assert.assertTrue(ByteUtil.compare(pairPriceKey3, pairPriceKey1) < 0);
    Assert.assertEquals(0, ByteUtil.compare(pairPriceKey1, pairPriceKey2));

    MarketOrderIdListCapsule capsule0 = new MarketOrderIdListCapsule(ByteArray.fromLong(0),
        ByteArray.fromLong(0));
    MarketOrderIdListCapsule capsule1 = new MarketOrderIdListCapsule(ByteArray.fromLong(1),
        ByteArray.fromLong(1));
    MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
        ByteArray.fromLong(2));
    MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
        ByteArray.fromLong(3));

    // put: 2 1 0 3
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey2));
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);

    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    // pairPriceKey1 and pairPriceKey2 has the same value,
    // After put pairPriceKey2, pairPriceKey2 will be replaced by pairPriceKey1, both key and value.
    // But you can still get(pairPriceKey2) return pairPriceKey1's value
    Assert.assertTrue(marketPairPriceToOrderStore.has(pairPriceKey1));
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    Assert.assertEquals(1, marketPairPriceToOrderStore.size());

    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey0));
    if (!marketPairPriceToOrderStore.has(pairPriceKey0)) {
      marketPairPriceToOrderStore.put(pairPriceKey0, capsule0);
    }

    Assert.assertEquals(2, marketPairPriceToOrderStore.size());

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey3));
    if (!marketPairPriceToOrderStore.has(pairPriceKey3)) {
      marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    }

    Assert.assertEquals(3, marketPairPriceToOrderStore.size());

    // get pairPriceKey1, will get pairPriceKey2's value capsule2
    try {
      Assert
          .assertArrayEquals(capsule0.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey0).getData());
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    // We will not have pairPriceKey2 in DB
    // byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey2, nextKey);

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey1);
    Assert.assertArrayEquals(pairPriceKey3, nextKey);

    List<byte[]> keyList = marketPairPriceToOrderStore.getKeysNext(pairPriceKey0, 2 + 1);
    Assert.assertArrayEquals(pairPriceKey0, keyList.get(0));
    Assert.assertArrayEquals(pairPriceKey1, keyList.get(1));
    Assert.assertArrayEquals(pairPriceKey3, keyList.get(2));
  }

  @Test
  public void testPriceSeqWithSamePairNoGCD() {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

    // put order: pairPriceKey2 pairPriceKey1 pairPriceKey3 pairPriceKey0
    // lexicographical order: pairPriceKey0 < pairPriceKey3 < pairPriceKey1 < pairPriceKey2
    // key order: pairPriceKey0 < pairPriceKey1 = pairPriceKey2 < pairPriceKey3
    byte[] sellTokenID1 = ByteArray.fromString("100");
    byte[] buyTokenID1 = ByteArray.fromString("200");
    byte[] pairPriceKey0 = MarketUtils.createPairPriceKeyNoGCD(
        sellTokenID1,
        buyTokenID1,
        0L,
        0L
    );
    byte[] pairPriceKey1 = MarketUtils.createPairPriceKeyNoGCD(
        sellTokenID1,
        buyTokenID1,
        2L,
        6L
    );
    byte[] pairPriceKey2 = MarketUtils.createPairPriceKeyNoGCD(
        sellTokenID1,
        buyTokenID1,
        3L,
        9L
    );
    byte[] pairPriceKey3 = MarketUtils.createPairPriceKeyNoGCD(
        sellTokenID1,
        buyTokenID1,
        1L,
        4L
    );

    // lexicographical order: pairPriceKey0 < pairPriceKey3 < pairPriceKey1 < pairPriceKey2
    Assert.assertTrue(ByteUtil.compare(pairPriceKey0, pairPriceKey3) < 0);
    Assert.assertTrue(ByteUtil.compare(pairPriceKey3, pairPriceKey1) < 0);
    Assert.assertTrue(ByteUtil.compare(pairPriceKey1, pairPriceKey2) < 0);

    MarketOrderIdListCapsule capsule0 = new MarketOrderIdListCapsule(ByteArray.fromLong(0),
        ByteArray.fromLong(0));
    MarketOrderIdListCapsule capsule1 = new MarketOrderIdListCapsule(ByteArray.fromLong(1),
        ByteArray.fromLong(1));
    MarketOrderIdListCapsule capsule2 = new MarketOrderIdListCapsule(ByteArray.fromLong(2),
        ByteArray.fromLong(2));
    MarketOrderIdListCapsule capsule3 = new MarketOrderIdListCapsule(ByteArray.fromLong(3),
        ByteArray.fromLong(3));

    // put: 2 1 0 3
    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey2));
    marketPairPriceToOrderStore.put(pairPriceKey2, capsule2);

    try {
      Assert
          .assertArrayEquals(capsule2.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    // pairPriceKey1 and pairPriceKey2 has the same value,
    // After put pairPriceKey2, pairPriceKey2 will be replaced by pairPriceKey1, both key and value.
    // But you can still get(pairPriceKey2) return pairPriceKey1's value
    Assert.assertTrue(marketPairPriceToOrderStore.has(pairPriceKey1));
    marketPairPriceToOrderStore.put(pairPriceKey1, capsule1);
    Assert.assertEquals(1, marketPairPriceToOrderStore.size());

    try {
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey0));
    if (!marketPairPriceToOrderStore.has(pairPriceKey0)) {
      marketPairPriceToOrderStore.put(pairPriceKey0, capsule0);
    }

    Assert.assertEquals(2, marketPairPriceToOrderStore.size());

    Assert.assertFalse(marketPairPriceToOrderStore.has(pairPriceKey3));
    if (!marketPairPriceToOrderStore.has(pairPriceKey3)) {
      marketPairPriceToOrderStore.put(pairPriceKey3, capsule3);
    }

    Assert.assertEquals(3, marketPairPriceToOrderStore.size());

    // get pairPriceKey1, will get pairPriceKey2's value capsule2
    try {
      Assert
          .assertArrayEquals(capsule0.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey0).getData());
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey1).getData());
      Assert
          .assertArrayEquals(capsule1.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey2).getData());
      Assert
          .assertArrayEquals(capsule3.getData(),
              marketPairPriceToOrderStore.get(pairPriceKey3).getData());
    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

    // We will not have pairPriceKey2 in DB
    // byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey2);
    // Assert.assertArrayEquals("nextKey should be pairPriceKey3", pairPriceKey2, nextKey);

    byte[] nextKey = marketPairPriceToOrderStore.getNextKey(pairPriceKey1);
    Assert.assertArrayEquals(pairPriceKey3, nextKey);

    List<byte[]> keyList = marketPairPriceToOrderStore.getKeysNext(pairPriceKey0, 2 + 1);
    Assert.assertArrayEquals(pairPriceKey0, keyList.get(0));
    Assert.assertArrayEquals(pairPriceKey1, keyList.get(1));
    Assert.assertArrayEquals(pairPriceKey3, keyList.get(2));
  }

  @Test
  public void testFindGCD() {
    Assert.assertEquals(0, MarketUtils.findGCD(0, 0));
    Assert.assertEquals(0, MarketUtils.findGCD(1, 0));
    Assert.assertEquals(0, MarketUtils.findGCD(0, 1));
    Assert.assertEquals(1, MarketUtils.findGCD(1, 3));
    Assert.assertEquals(1, MarketUtils.findGCD(3, 1));
    Assert.assertEquals(3, MarketUtils.findGCD(3, 15));
    Assert.assertEquals(1, MarketUtils.findGCD(13, 15));
    Assert.assertEquals(9, MarketUtils.findGCD(27, 9));
  }

  private boolean randomOp() {
    int i = randomInt(0, 999999);
    return i % 2 == 0;
  }

  /**
   * From this test we know that, if we use getKeysNext to get the priceKey list of one token pair,
   * we should know the count of priceKey previously. Update: getKeysNext will just return
   * (sellToken, buyToken)'s price, so the result will be 0 now.
   */
  @Test
  public void testGetKeysNextNotExitsWithRandom() {
    int maxInt = 99999999;
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketPairPriceToOrderStore marketPairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

    int sellToken = randomInt(100, 9999);
    int buyToken = randomInt(10000, 9999999);

    // randomSellToken != sellToken, randomBuyToken != buyToken
    for (int i = 0; i < 1000; i++) {
      int randomSellToken =
          randomOp() ? sellToken + randomInt(1, maxInt) : sellToken - randomInt(1, sellToken - 1);
      int randomBuyToken =
          randomOp() ? buyToken + randomInt(1, maxInt) : buyToken - randomInt(1, buyToken - 1);
      byte[] pairPriceKey = MarketUtils.createPairPriceKeyNoGCD(
          ByteArray.fromString(String.valueOf(randomSellToken)),
          ByteArray.fromString(String.valueOf(randomBuyToken)),
          randomInt(1, 999999),
          randomInt(1, 999999)
      );
      MarketOrderIdListCapsule capsule = new MarketOrderIdListCapsule(
          ByteArray.fromLong(randomInt(1, 999999)),
          ByteArray.fromLong(randomInt(1, 999999)));

      marketPairPriceToOrderStore.put(pairPriceKey, capsule);
    }

    byte[] sellTokenId = ByteArray.fromString(String.valueOf(sellToken));
    byte[] buyTokenId = ByteArray.fromString(String.valueOf(buyToken));
    byte[] headKey = MarketUtils.getPairPriceHeadKey(sellTokenId, buyTokenId);

    List<byte[]> list = marketPairPriceToOrderStore.getKeysNext(headKey, 100);
    Assert.assertEquals(0, list.size());
  }

  @Test
  public void testTrim() {
    byte[] tokenId = ByteArray.fromString("10000010");
    Assert.assertArrayEquals(tokenId, MarketUtils.trim(tokenId));

    byte[] sellTokenId = ByteArray.fromString("100");
    byte[] buyTokenId = ByteArray.fromString("200");
    byte[] pairKey = MarketUtils.createPairKey(sellTokenId, buyTokenId);
    MarketOrderPair marketOrderPair = MarketUtils.decodeKeyToMarketPair(pairKey);
    Assert.assertArrayEquals(sellTokenId,
        MarketUtils.trim(marketOrderPair.getSellTokenId().toByteArray()));
    Assert.assertArrayEquals(buyTokenId,
        MarketUtils.trim(marketOrderPair.getBuyTokenId().toByteArray()));
  }
}