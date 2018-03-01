package org.tron.core.capsule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Protocal.Transaction;


public class TransactionCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  protected TransactionCapsule transactionCapsule;
  Transaction transaction = Transaction.newBuilder().build();

  @Before
  public void step() {

    transactionCapsule = new TransactionCapsule(transaction);
  }

  @Test
  public void testGetHash() {

    logger.info("test getHash = {}", transactionCapsule.getHash());

    Assert.assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        transactionCapsule.getHash().toString());
  }


  @Test
  public void testCheckBalance() {

    Assert.assertTrue(transactionCapsule
        .checkBalance(new byte[]{1, 12, -1, 23}, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4", 1, 2));

    Assert.assertFalse(transactionCapsule
        .checkBalance(new byte[]{1, 12, -1, 23},
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 1, 2));

    Assert.assertFalse(transactionCapsule.checkBalance(new byte[]{1, 12, -1, 23}, "", 3, 2));

  }

}
