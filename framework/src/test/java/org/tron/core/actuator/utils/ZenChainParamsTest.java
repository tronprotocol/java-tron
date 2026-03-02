package org.tron.core.actuator.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.tron.core.utils.ZenChainParams;


@Slf4j(topic = "capsule")
public class ZenChainParamsTest {

  @Test
  public void variableCheck() {
    ZenChainParams zenChainParams = new ZenChainParams();
    assertNotNull(zenChainParams);
    assertEquals(16, ZenChainParams.NOTEENCRYPTION_AUTH_BYTES);
    assertEquals(1, ZenChainParams.ZC_NOTEPLAINTEXT_LEADING);
    assertEquals(8, ZenChainParams.ZC_V_SIZE);
    assertEquals(32, ZenChainParams.ZC_R_SIZE);
    assertEquals(512, ZenChainParams.ZC_MEMO_SIZE);
    assertEquals(11, ZenChainParams.ZC_DIVERSIFIER_SIZE);
    assertEquals(32, ZenChainParams.ZC_JUBJUB_POINT_SIZE);
    assertEquals(32, ZenChainParams.ZC_JUBJUB_SCALAR_SIZE);
    int ZC_ENCPLAINTEXT_SIZE =
        ZenChainParams.ZC_NOTEPLAINTEXT_LEADING + ZenChainParams.ZC_DIVERSIFIER_SIZE
            + ZenChainParams.ZC_V_SIZE + ZenChainParams.ZC_R_SIZE + ZenChainParams.ZC_MEMO_SIZE;
    assertEquals(ZenChainParams.ZC_ENCPLAINTEXT_SIZE, ZC_ENCPLAINTEXT_SIZE);
    int ZC_ENCCIPHERTEXT_SIZE = (ZenChainParams.ZC_ENCPLAINTEXT_SIZE
        + ZenChainParams.NOTEENCRYPTION_AUTH_BYTES);
    assertEquals(ZenChainParams.ZC_ENCCIPHERTEXT_SIZE, ZC_ENCCIPHERTEXT_SIZE);
    int ZC_OUTCIPHERTEXT_SIZE = (ZenChainParams.ZC_OUTPLAINTEXT_SIZE
        + ZenChainParams.NOTEENCRYPTION_AUTH_BYTES);
    assertEquals(ZenChainParams.ZC_OUTCIPHERTEXT_SIZE, ZC_OUTCIPHERTEXT_SIZE);
  }

}
