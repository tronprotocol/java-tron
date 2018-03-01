package org.tron.core.capsule;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxInputCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testTxOutputCapsule() {
    byte[] txId = {1, 2, 3, 4, 5, 6};
    long vout = 12345L;
    byte[] signature = {13, 14, 15, 16, 17};
    byte[] pubkey = {123, 22, 24, 36};

    TxInputCapsule txInputCapsule = new TxInputCapsule(txId, vout, signature, pubkey);
    logger.info("txId={}",
        txInputCapsule.getTxInput().getRawData().getTxID().toByteArray());
    logger.info("vout={}",
        txInputCapsule.getTxInput().getRawData().getVout());
    logger.info("signature={}", txInputCapsule.getTxInput().getSignature().toByteArray());
    logger.info("pubkey={}",
        txInputCapsule.getTxInput().getRawData().getPubKey().toByteArray());

    Assert
        .assertArrayEquals(txId, txInputCapsule.getTxInput().getRawData().getTxID().toByteArray());
    Assert.assertEquals(vout, txInputCapsule.getTxInput().getRawData().getVout());
    Assert.assertArrayEquals(signature, txInputCapsule.getTxInput().getSignature().toByteArray());
    Assert.assertArrayEquals(pubkey,
        txInputCapsule.getTxInput().getRawData().getPubKey().toByteArray());
    Assert.assertTrue(txInputCapsule.validate());
  }
}
