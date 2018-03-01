package org.tron.core.capsule;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;

public class TxOutputCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");


  @Test
  public void testTxOutputCapsule() {
    long value = 123456L;
    String address = "1921681012";
    TxOutputCapsule txOutputCapsule = new TxOutputCapsule(value, address);
    logger.info("value={}", txOutputCapsule.getTxOutput().getValue());
    logger.info("address={}",
        ByteArray.toHexString(txOutputCapsule.getTxOutput().getPubKeyHash().toByteArray()));
    logger.info("validate={}", txOutputCapsule.validate());
    Assert.assertEquals(value, txOutputCapsule.getTxOutput().getValue());
    Assert.assertEquals(address,
        ByteArray.toHexString(txOutputCapsule.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule.validate());

    long value1 = 98765L;
    String address1 = "192168101";
    String address2 = "0" + address1;
    TxOutputCapsule txOutputCapsule1 = new TxOutputCapsule(value1, address1);
    logger.info("value1={}", txOutputCapsule1.getTxOutput().getValue());
    logger.info("address1={}",
        ByteArray.toHexString(txOutputCapsule1.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertEquals(value1, txOutputCapsule1.getTxOutput().getValue());
    Assert.assertEquals(address2,
        ByteArray.toHexString(txOutputCapsule1.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule1.validate());
    logger.info("validate1={}", txOutputCapsule1.validate());

    long value3 = 9852448L;
    String address3 = "0x1921681011";
    String address4 = "1921681011";
    TxOutputCapsule txOutputCapsule2 = new TxOutputCapsule(value1, address1);
    logger.info("value2={}", txOutputCapsule2.getTxOutput().getValue());
    logger.info("address2={}",
        ByteArray.toHexString(txOutputCapsule2.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertEquals(value1, txOutputCapsule2.getTxOutput().getValue());
    Assert.assertEquals(address2,
        ByteArray.toHexString(txOutputCapsule2.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule2.validate());
    logger.info("validate2={}", txOutputCapsule2.validate());

    long value5 = 67549L;
    String address5 = null;
    TxOutputCapsule txOutputCapsule3 = new TxOutputCapsule(value5, address5);
    logger.info("value5={}", txOutputCapsule3.getTxOutput().getValue());
    logger.info("address5={}",
        ByteArray.toHexString(txOutputCapsule3.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertEquals(value5, txOutputCapsule3.getTxOutput().getValue());
    Assert.assertEquals("",
        ByteArray.toHexString(txOutputCapsule3.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule3.validate());
    logger.info("validate5={}", txOutputCapsule3.validate());

  }

}
