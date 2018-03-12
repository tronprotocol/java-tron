package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.Constant;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;

public class WitnessStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  WitnessStore witnessStore;

  @Before
  public void initDb() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
    this.witnessStore = this.witnessStore.create("witness");
  }

  @Test
  public void putAndGetWitness() {
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFromUtf8("100000000x"), 100L,
        "");

    this.witnessStore.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
    WitnessCapsule witnessSource = this.witnessStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(witnessCapsule.getAddress(), witnessSource.getAddress());
    Assert.assertEquals(witnessCapsule.getVoteCount(), witnessSource.getVoteCount());

    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());

    witnessCapsule = new WitnessCapsule(ByteString.copyFromUtf8(""), 100L, "");

    this.witnessStore.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
    witnessSource = this.witnessStore.get(ByteString.copyFromUtf8("").toByteArray());
    Assert.assertEquals(witnessCapsule.getAddress(), witnessSource.getAddress());
    Assert.assertEquals(witnessCapsule.getVoteCount(), witnessSource.getVoteCount());

    Assert.assertEquals(ByteString.copyFromUtf8(""), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());
  }


}