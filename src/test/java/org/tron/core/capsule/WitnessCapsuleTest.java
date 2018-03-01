package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocal.Witness;

public class WitnessCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  protected WitnessCapsule witnessCapsule;
  String pubKeyStr = "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81";

  Witness.Builder witness = Witness.newBuilder().setPubKey(ByteString.copyFrom(ByteArray
      .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")));

  @Before
  public void step() {

    witnessCapsule = new WitnessCapsule(witness.build());
  }


  @Test
  public void testSetPubKey() {

    String pubKey = ByteArray.toHexString(witness.getPubKey().toByteArray());

    Assert.assertEquals("is not expect:", pubKeyStr, pubKey);

  }

}
