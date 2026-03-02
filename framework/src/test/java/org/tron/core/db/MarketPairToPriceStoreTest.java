package org.tron.core.db;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.core.store.MarketPairToPriceStore;

public class MarketPairToPriceStoreTest extends BaseTest {

  @Resource
  private MarketPairToPriceStore marketPairToPriceStore;

  @Resource
  private MarketPairPriceToOrderStore marketPairPriceToOrderStore;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }

  @Test
  public void testGet() throws Exception {
    marketPairToPriceStore.put("testGet".getBytes(), new BytesCapsule(
        ByteArray.fromString("11.0")));
    final BytesCapsule result = marketPairToPriceStore.get("testGet".getBytes());
    Assert.assertNotNull(result);
    Assert.assertEquals(new String(result.getData()), "11.0");
  }

  @Test
  public void testGetPriceNum() {
    marketPairToPriceStore.put("testGetPriceNum".getBytes(), new BytesCapsule(
        ByteArray.fromLong(100)));
    final long result = marketPairToPriceStore.getPriceNum("testGetPriceNum".getBytes());
    assertEquals(100L, result);
    assertEquals(0L, marketPairToPriceStore.getPriceNum("testGetPriceNum1".getBytes()));
  }

  @Test
  public void testGetPriceNumByTokenId() {
    marketPairToPriceStore.setPriceNum("tokenId1".getBytes(), "tokenId2".getBytes(),99);
    final long result =
        marketPairToPriceStore.getPriceNum("tokenId1".getBytes(), "tokenId2".getBytes());
    assertEquals(99L, result);
    assertEquals(0L, marketPairToPriceStore.getPriceNum("tokenId2".getBytes(),
        "tokenId1".getBytes()));
  }

  @Test
  public void testSetPriceNum() {
    marketPairToPriceStore.setPriceNum("testSetPriceNum1".getBytes(), 98L);
    long result = marketPairToPriceStore.getPriceNum("testSetPriceNum1".getBytes());
    assertEquals(result, 98);
  }

  @Test
  public void testSetPriceNumByToken() {
    marketPairToPriceStore.setPriceNum("token3".getBytes(), "token4".getBytes(), 97L);
    long result = marketPairToPriceStore.getPriceNum("token3".getBytes(), "token4".getBytes());
    assertEquals(result, 97);
  }

  @Test
  public void testAddNewPriceKey() {
    marketPairToPriceStore
        .addNewPriceKey("token5".getBytes(), "token6".getBytes(), marketPairPriceToOrderStore);
    long result = marketPairToPriceStore.getPriceNum("token5".getBytes(), "token6".getBytes());
    assertEquals(1, result);
  }

}
