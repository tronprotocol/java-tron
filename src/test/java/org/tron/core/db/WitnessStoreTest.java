package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocal.Witness;

public class WitnessStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  WitnessStore witnessStore;

  @Before
  public void initDb() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
    witnessStore = witnessStore.create("witness");
  }

  @Test
  public void putAndGetWitness() {
    Witness witnessTemp = Witness.newBuilder().setVoteCount(100L)
        .setAddress(ByteString.copyFromUtf8("100000000x")).build();
    witnessStore.putWitness(witnessTemp);
    Witness witnessSource = witnessStore.getWitness(ByteString.copyFromUtf8("100000000x"));
    Assert.assertEquals(witnessTemp, witnessSource);
    //logger.info(witnessSource.getAddress().toString());
    //logger.info(String.valueOf(witnessSource.getVoteCount()));
    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());
  }


}