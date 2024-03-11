package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketAccountStore;

public class MarketAccountStoreTest extends BaseTest {

  @Resource
  private MarketAccountStore marketAccountStore;

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
    String address = "Address1";

    MarketAccountOrderCapsule marketAccountOrderCapsule =
            marketAccountStore.getUnchecked(address.getBytes());
    Assert.assertNull(marketAccountOrderCapsule);

    marketAccountStore.put(address.getBytes(),
        new MarketAccountOrderCapsule(ByteString.copyFrom(address.getBytes())));
    final MarketAccountOrderCapsule result = marketAccountStore.get(address.getBytes());
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getOwnerAddress(), ByteString.copyFrom(address.getBytes()));
  }

  @Test
  public void testPut() throws ItemNotFoundException {
    String address = "Address1";
    marketAccountStore.put(address.getBytes(),
            new MarketAccountOrderCapsule(ByteString.copyFrom(address.getBytes())));
    final MarketAccountOrderCapsule result = marketAccountStore.get(address.getBytes());
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getOwnerAddress(), ByteString.copyFrom(address.getBytes()));
  }

}
