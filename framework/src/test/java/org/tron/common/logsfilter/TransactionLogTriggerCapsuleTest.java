package org.tron.common.logsfilter;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.p2p.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.Common;
import org.tron.protos.contract.SmartContractOuterClass;

public class TransactionLogTriggerCapsuleTest {

  private static final String OWNER_ADDRESS = "41548794500882809695a8a687866e76d4271a1abc";
  private static final String RECEIVER_ADDRESS = "41abd4b9367799eaa3197fecb144eb71de1e049150";
  private static final String CONTRACT_ADDRESS = "1111";

  public TransactionCapsule transactionCapsule;
  public BlockCapsule blockCapsule;

  @Before
  public void setup() {
    blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
  }

  @Test
  public void testConstructorWithUnfreezeBalanceTrxCapsule() {
    BalanceContract.UnfreezeBalanceContract.Builder builder2 =
        BalanceContract.UnfreezeBalanceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.UnfreezeBalanceContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getToAddress());
  }


  @Test
  public void testConstructorWithFreezeBalanceV2TrxCapsule() {
    BalanceContract.FreezeBalanceV2Contract.Builder builder2 =
        BalanceContract.FreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setFrozenBalance(TRX_PRECISION + 100000)
        .setResource(Common.ResourceCode.BANDWIDTH);
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.FreezeBalanceV2Contract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertEquals("trx", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(TRX_PRECISION + 100000,
        triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }

  @Test
  public void testConstructorWithUnfreezeBalanceV2TrxCapsule() {
    BalanceContract.UnfreezeBalanceV2Contract.Builder builder2 =
        BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setUnfreezeBalance(TRX_PRECISION + 4000)
        .setResource(Common.ResourceCode.BANDWIDTH);
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.UnfreezeBalanceV2Contract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertEquals("trx", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(TRX_PRECISION + 4000,
        triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }


  @Test
  public void testConstructorWithWithdrawExpireTrxCapsule() {
    BalanceContract.WithdrawExpireUnfreezeContract.Builder builder2 =
        BalanceContract.WithdrawExpireUnfreezeContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.WithdrawExpireUnfreezeContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertEquals("trx", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(0L, triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }


  @Test
  public void testConstructorWithDelegateResourceTrxCapsule() {
    BalanceContract.DelegateResourceContract.Builder builder2 =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
        .setBalance(TRX_PRECISION + 2000);
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.DelegateResourceContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getToAddress());
    Assert.assertEquals("trx", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(TRX_PRECISION + 2000,
        triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }

  @Test
  public void testConstructorWithUnDelegateResourceTrxCapsule() {
    BalanceContract.UnDelegateResourceContract.Builder builder2 =
        BalanceContract.UnDelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
        .setBalance(TRX_PRECISION + 10000);
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.UnDelegateResourceContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getToAddress());
    Assert.assertEquals("trx", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(TRX_PRECISION + 10000,
        triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }

  @Test
  public void testConstructorWithCancelAllUnfreezeTrxCapsule() {
    BalanceContract.CancelAllUnfreezeV2Contract.Builder builder2 =
        BalanceContract.CancelAllUnfreezeV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.CancelAllUnfreezeV2Contract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
  }

  @Test
  public void testConstructorWithTransferTrxCapsule() {
    BalanceContract.TransferContract.Builder builder2 =
        BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .setAmount(TRX_PRECISION);
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TransferContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getToAddress());
    Assert.assertEquals("trx", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(TRX_PRECISION, triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }

  @Test
  public void testConstructorWithTransferAssetTrxCapsule() {
    AssetIssueContractOuterClass.TransferAssetContract.Builder builder2 =
        AssetIssueContractOuterClass.TransferAssetContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .setAssetName(ByteString.copyFrom("eth".getBytes()))
            .setAmount(TRX_PRECISION);
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TransferAssetContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getToAddress());
    Assert.assertEquals("eth", triggerCapsule.getTransactionLogTrigger().getAssetName());
    Assert.assertEquals(TRX_PRECISION, triggerCapsule.getTransactionLogTrigger().getAssetAmount());
  }

  @Test
  public void testConstructorWithTriggerSmartContractTrxCapsule() {
    SmartContractOuterClass.TriggerSmartContract.Builder builder2 =
        SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_ADDRESS)));
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TriggerSmartContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getToAddress());
  }

  @Test
  public void testConstructorWithCreateSmartContractTrxCapsule() {
    SmartContractOuterClass.CreateSmartContract.Builder builder2 =
        SmartContractOuterClass.CreateSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.CreateSmartContract);

    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger().getFromAddress());
  }

}