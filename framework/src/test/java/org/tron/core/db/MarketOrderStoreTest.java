package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketOrderStore;
import org.tron.protos.Protocol;

public class MarketOrderStoreTest extends BaseTest {

  @Resource
  private MarketOrderStore marketOrderStore;

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
    byte[] orderId = "testGet".getBytes();
    marketOrderStore.put(orderId,
        new MarketOrderCapsule(Protocol.MarketOrder.newBuilder()
            .setOrderId(ByteString.copyFrom(orderId))
            .setSellTokenId(ByteString.copyFrom("addr1".getBytes()))
            .setSellTokenQuantity(200L)
            .setBuyTokenId(ByteString.copyFrom("addr2".getBytes()))
            .setBuyTokenQuantity(100L)
            .build()));
    final MarketOrderCapsule result = marketOrderStore.get(orderId);
    Assert.assertNotNull(result);
    Assert.assertEquals(new String(result.getSellTokenId()), "addr1");
    Assert.assertEquals(result.getSellTokenQuantity(), 200L);
    Assert.assertEquals(new String(result.getBuyTokenId()), "addr2");
    Assert.assertEquals(result.getBuyTokenQuantity(), 100L);
  }

  @Test
  public void testDelete() throws ItemNotFoundException {
    byte[] orderId = "testDelete".getBytes();
    marketOrderStore.put(orderId,
            new MarketOrderCapsule(Protocol.MarketOrder.newBuilder()
                    .setOrderId(ByteString.copyFrom(orderId))
                    .setSellTokenId(ByteString.copyFrom("addr1".getBytes()))
                    .setSellTokenQuantity(200L)
                    .setBuyTokenId(ByteString.copyFrom("addr2".getBytes()))
                    .setBuyTokenQuantity(100L)
                    .build()));
    marketOrderStore.delete(orderId);
    final MarketOrderCapsule result = marketOrderStore.getUnchecked(orderId);
    Assert.assertNull(result);
  }

}
