package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

public class RecentTransactionStoreTest extends BaseTest {

  @Resource
  private RecentTransactionStore recentTransactionStore;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath()
            },
            Constant.TEST_CONF
    );
  }

  private TransactionCapsule createTransaction() {
    BalanceContract.TransferContract tc =
            BalanceContract.TransferContract.newBuilder()
                    .setAmount(10)
                    .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
                    .setToAddress(ByteString.copyFromUtf8("bbb"))
                    .build();
    return new TransactionCapsule(tc,
            Protocol.Transaction.Contract.ContractType.TransferContract);
  }


  @Test
  public void testPut() {
    TransactionCapsule transaction = createTransaction();
    byte[] key = transaction.getTransactionId().getBytes();
    BytesCapsule value = new BytesCapsule(ByteArray.subArray(transaction
                    .getTransactionId().getBytes(),
            8,
            16));
    recentTransactionStore.put(key, value);
    Assert.assertTrue(recentTransactionStore.has(key));
  }

  @Test
  public void testGet() throws ItemNotFoundException {
    TransactionCapsule transaction = createTransaction();
    byte[] key = transaction.getTransactionId().getBytes();
    BytesCapsule value = new BytesCapsule(
            ByteArray.subArray(transaction
                    .getTransactionId().getBytes(),
            8,
            16));
    recentTransactionStore.put(key, value);

    BytesCapsule bytesCapsule = recentTransactionStore.get(key);
    Assert.assertNotNull(bytesCapsule);
    Assert.assertArrayEquals(value.getData(), bytesCapsule.getData());

  }
}
