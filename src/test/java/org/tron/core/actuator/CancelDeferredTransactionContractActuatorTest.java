package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.DeferredTransactionCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.Protocol.Transaction.raw.Builder;

@Slf4j
public class CancelDeferredTransactionContractActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_cancel_deferred_transaction_test";
  private static TronApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final long AMOUNT = 100;
  private static final long OWNER_BALANCE = 9999999;
  private static final long TO_BALANCE = 100001;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String TO_ADDRESS_INVALID = "bbb";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final String OWNER_NO_BALANCE;
  private static final String To_ACCOUNT_INVALID;
  private static Transaction transaction = null;
  private static DeferredTransaction deferredTransaction = null;
  private static DeferredTransactionCapsule deferredTransactionCapsule = null;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_NO_BALANCE = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3433";
    To_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3422";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    initDeferredTransaction();
    deferredTransaction = getBuildDeferredTransaction(transaction);
    deferredTransactionCapsule = new DeferredTransactionCapsule(deferredTransaction);
    dbManager.getDeferredTransactionIdIndexStore().put(deferredTransactionCapsule);
    dbManager.getDeferredTransactionStore().put(deferredTransactionCapsule);
  }

  private static void initDeferredTransaction() {
    transaction = getBuildTransaction(
        getBuildTransferContract(OWNER_ADDRESS, TO_ADDRESS),
        System.currentTimeMillis(), 100);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  private static DeferredTransaction getBuildDeferredTransaction(Transaction transaction) {
    Builder rawData = transaction.getRawData().toBuilder().setDelaySeconds(100);
    transaction = transaction.toBuilder().setRawData(rawData).build();
    DeferredTransaction.Builder deferredTransaction = DeferredTransaction.newBuilder();
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    deferredTransaction.setTransactionId(transactionCapsule.getTransactionId().getByteString());
    deferredTransaction.setDelaySeconds(transactionCapsule.getDeferredSeconds());
    deferredTransaction.setDelayUntil(System.currentTimeMillis() + 10000);
    ByteString senderAddress = transactionCapsule.getSenderAddress();
    ByteString toAddress = transactionCapsule.getToAddress();

    deferredTransaction.setSenderAddress(senderAddress);
    deferredTransaction.setReceiverAddress(toAddress);
    deferredTransaction.setTransaction(transactionCapsule.getInstance());
    return deferredTransaction.build();
  }

  private static Transaction getBuildTransaction(
      TransferContract transferContract, long transactionTimestamp, long refBlockNum) {
    return Transaction.newBuilder().setRawData(
        Transaction.raw.newBuilder().setTimestamp(transactionTimestamp).setRefBlockNum(refBlockNum)
            .addContract(
                Transaction.Contract.newBuilder().setType(ContractType.TransferContract)
                    .setParameter(Any.pack(transferContract)).build()).build()).build();
  }

  private static TransferContract getBuildTransferContract(String ownerAddress, String toAddress) {
    return TransferContract.newBuilder().setAmount(10)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress))).build();
  }

  private Any getOwnerAddressContract() {
    return Any.pack(
        Contract.CancelDeferredTransactionContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setTransactionId(deferredTransactionCapsule.getTransactionId())
            .build());
  }

  private Any getToAddressContract() {
    return Any.pack(
        Contract.CancelDeferredTransactionContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setTransactionId(deferredTransactionCapsule.getTransactionId())
            .build());
  }

  @Test
  public void perfectCancelDeferredTransaction() {
    CancelDeferredTransactionContractActuator actuator = new CancelDeferredTransactionContractActuator(
        getOwnerAddressContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    byte[] key = dbManager.getDeferredTransactionIdIndexStore().getDeferredTransactionKeyById(deferredTransaction.getTransactionId());
    Assert.assertNotNull("perfect cancel deferred transaction", key);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      key = dbManager.getDeferredTransactionIdIndexStore().getDeferredTransactionKeyById(deferredTransaction.getTransactionId());
      Assert.assertNull("perfect cancel deferred transaction", key);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void failedCancelDeferredTransaction() throws ContractValidateException {
    CancelDeferredTransactionContractActuator actuator = new CancelDeferredTransactionContractActuator(
        getToAddressContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    byte[] key = dbManager.getDeferredTransactionIdIndexStore().getDeferredTransactionKeyById(deferredTransaction.getTransactionId());
    try {
      actuator.validate();

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    }
  }

}
