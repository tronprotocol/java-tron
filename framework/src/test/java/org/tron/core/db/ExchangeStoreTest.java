package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.ExchangeStore;
import org.tron.protos.Protocol;

public class ExchangeStoreTest extends BaseTest {

  @Resource
  private ExchangeStore exchangeStore;
  private byte[] exchangeKey1;
  private byte[] exchangeKey2;

  static {
    Args.setParam(
        new String[] {
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    Protocol.Exchange.Builder builder = Protocol.Exchange.newBuilder();
    builder.setExchangeId(1L).setCreatorAddress(ByteString.copyFromUtf8("Address1"));
    ExchangeCapsule exchangeCapsule = new ExchangeCapsule(builder.build());
    exchangeKey1 = exchangeCapsule.createDbKey();
    chainBaseManager.getExchangeStore().put(exchangeKey1, exchangeCapsule);
    builder.setExchangeId(2L).setCreatorAddress(ByteString.copyFromUtf8("Address2"));
    exchangeCapsule = new ExchangeCapsule(builder.build());
    exchangeKey2 = exchangeCapsule.createDbKey();
    chainBaseManager.getExchangeStore().put(exchangeKey2, exchangeCapsule);
  }


  @Test
  public void testGet() throws Exception {
    final ExchangeCapsule result = exchangeStore.get(exchangeKey1);
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getID(), 1);
  }

  @Test
  public void testPut() throws ItemNotFoundException {
    Protocol.Exchange.Builder builder = Protocol.Exchange.newBuilder();
    builder.setExchangeId(1L).setCreatorAddress(ByteString.copyFromUtf8("Address1"));
    ExchangeCapsule exchangeCapsule = new ExchangeCapsule(builder.build());
    exchangeKey1 = exchangeCapsule.createDbKey();
    chainBaseManager.getExchangeStore().put(exchangeKey1, exchangeCapsule);

    final ExchangeCapsule result = exchangeStore.get(exchangeKey1);
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getID(), 1);
  }

  @Test
  public void testDelete() throws Exception {
    final ExchangeCapsule result = exchangeStore.get(exchangeKey1);
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getID(), 1);

    exchangeStore.delete(exchangeKey1);
    ExchangeCapsule exchangeCapsule = exchangeStore.getUnchecked(exchangeKey1);
    Assert.assertNull(exchangeCapsule);
  }

  @Test
  public void testGetAllExchanges() {
    List<ExchangeCapsule> exchangeCapsuleList = exchangeStore.getAllExchanges();
    ExchangeCapsule exchangeCapsule1 = exchangeCapsuleList.get(0);
    ExchangeCapsule exchangeCapsule2 = exchangeCapsuleList.get(1);
    Assert.assertEquals(exchangeCapsuleList.size(), 2);
    Assert.assertEquals(exchangeCapsule1.getCreatorAddress(), ByteString.copyFromUtf8("Address1"));
    Assert.assertEquals(exchangeCapsule2.getCreatorAddress(), ByteString.copyFromUtf8("Address2"));
  }
}
