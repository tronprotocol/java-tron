package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.consensus.ConsensusDelegate;
import org.tron.core.Constant;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db2.ISession;
import org.tron.core.store.WitnessStore;

@Slf4j
public class TronCacheStoreTest extends BaseTest {

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Resource
  private WitnessStore witnessStore;

  @Resource
  private ConsensusDelegate consensusDelegate;

  @Resource
  private RevokingDatabase revokingDatabase;


  @Test
  public void getWitnessStandby() {
    revokingDatabase.setMaxFlushCount(1);
    List<WitnessCapsule> witnessStandby = witnessStore.getWitnessStandby();
    WitnessCapsule witness1 = new WitnessCapsule(ByteString.copyFromUtf8(
        UUID.randomUUID().toString()), 100L, "www.w1.com");
    WitnessCapsule witness2 = new WitnessCapsule(ByteString.copyFromUtf8(
        UUID.randomUUID().toString()), 200L, "www.w2.com");
    ISession session = revokingDatabase.buildSession();
    witnessStore.put(witness1.createDbKey(), witness1);
    witnessStore.put(witness2.createDbKey(), witness2);
    consensusDelegate.updateWitnessStandby(null);
    session.commit();
    List<WitnessCapsule> witnessStandby2 = witnessStore.getWitnessStandby();
    Assert.assertEquals(witnessStandby.size() + 2, witnessStandby2.size());
    revokingDatabase.fastPop();
    List<WitnessCapsule> witnessStandby3 = witnessStore.getWitnessStandby();
    Assert.assertEquals(witnessStandby, witnessStandby3);
    Assert.assertNotEquals(witnessStandby2, witnessStandby3);
    WitnessCapsule witness3 = new WitnessCapsule(ByteString.copyFromUtf8(
        UUID.randomUUID().toString()), 300L, "www.w3.com");
    session = revokingDatabase.buildSession();
    witnessStore.put(witness3.createDbKey(), witness3);
    consensusDelegate.updateWitnessStandby(null);
    session.commit();
    List<WitnessCapsule> witnessStandby4 = witnessStore.getWitnessStandby();
    for (int i = 0; i < 27; i++) {
      session = revokingDatabase.buildSession();
      session.commit();
    }
    List<WitnessCapsule> witnessStandby5 = witnessStore.getWitnessStandby();
    Assert.assertEquals(witnessStandby4, witnessStandby5);
    Assert.assertEquals(witnessStandby.size() + 1, witnessStandby5.size());
  }
}