package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.ExchangeV2Store;
import org.tron.protos.Protocol;

public class ExchangeV2StoreTest extends BaseTest {

  @Resource
  private ExchangeV2Store exchangeV2Store;

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
    byte[] key = putToExchangeV2();
    final ExchangeCapsule result = exchangeV2Store.get(key);
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getID(), 1);
  }

  private byte[] putToExchangeV2() {
    Protocol.Exchange.Builder builder = Protocol.Exchange.newBuilder().setExchangeId(1L)
        .setCreatorAddress(ByteString.copyFromUtf8("Address1"));
    ExchangeCapsule exchangeCapsule = new ExchangeCapsule(builder.build());
    byte[] exchangeKey1 = exchangeCapsule.createDbKey();
    exchangeV2Store.put(exchangeKey1, exchangeCapsule);
    return exchangeKey1;
  }
}
