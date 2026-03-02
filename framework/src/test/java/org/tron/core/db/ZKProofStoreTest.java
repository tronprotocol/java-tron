package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.ZKProofStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

public class ZKProofStoreTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()},
            Constant.TEST_CONF);
  }

  @Autowired
  private ZKProofStore proofStore;

  private TransactionCapsule getTransactionCapsule() {
    BalanceContract.TransferContract transferContract =
            BalanceContract.TransferContract.newBuilder()
                    .setAmount(10)
                    .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
                    .setToAddress(ByteString.copyFromUtf8("bbb"))
                    .build();
    return new TransactionCapsule(transferContract,
            Protocol.Transaction.Contract.ContractType.TransferContract);
  }

  @Test
  public void testPut() {
    TransactionCapsule trx = getTransactionCapsule();
    proofStore.put(trx.getTransactionId().getBytes(),
            true);
    boolean has = proofStore.has(trx.getTransactionId().getBytes());
    Assert.assertTrue(has);
  }

  @Test
  public void testGet() {
    TransactionCapsule trx = getTransactionCapsule();
    proofStore.put(trx.getTransactionId().getBytes(),
            true);
    Boolean result = proofStore.get(trx.getTransactionId().getBytes());
    Assert.assertEquals(true, result);

    proofStore.put(trx.getTransactionId().getBytes(),
            false);
    result = proofStore.get(trx.getTransactionId().getBytes());
    Assert.assertEquals(false, result);
  }
}
