package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;

public class TronJsonRpcImplTest {

  /**
   * Pins the TRC-10 synthetic transfer topic hex. Clients hard-code this value, so the
   * canonical signature string is a published contract. This test keeps its own copy of
   * the literal: editing the production literal in {@link TronJsonRpcImpl} without also
   * updating the literal here will diverge and fail CI.
   */
  @Test
  public void trc10TransferTopicHex_isStable() {
    String canonicalSignature = "TRC10Transfer(address,address,uint256,uint256)";
    String expected = ByteArray.toHexString(
        Hash.sha3(canonicalSignature.getBytes(StandardCharsets.UTF_8)));
    assertEquals(expected, TronJsonRpcImpl.TRC10_TRANSFER_TOPIC_HEX);
  }
}
