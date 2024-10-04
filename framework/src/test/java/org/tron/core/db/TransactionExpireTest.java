package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

@Slf4j
public class TransactionExpireTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private TronApplicationContext context;
  private Wallet wallet;
  private Manager dbManager;
  private BlockCapsule blockCapsule;

  @Before
  public void init() throws IOException {
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    CommonParameter.PARAMETER.setMinEffectiveConnection(0);

    context = new TronApplicationContext(DefaultConfig.class);
    wallet = context.getBean(Wallet.class);
    dbManager = context.getBean(Manager.class);
  }

  private void initLocalWitness() {
    String randomPrivateKey = PublicMethod.getRandomPrivateKey();
    LocalWitnesses localWitnesses = new LocalWitnesses();
    localWitnesses.setPrivateKeys(Arrays.asList(randomPrivateKey));
    localWitnesses.initWitnessAccountAddress(true);
    Args.setLocalWitnesses(localWitnesses);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void testExpireTransaction() {
    blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(blockCapsule.getNum());
    dbManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
    dbManager.updateRecentBlock(blockCapsule);
    initLocalWitness();

    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom(Args.getLocalWitnesses()
            .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine())))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();
    TransactionCapsule transactionCapsule =
        new TransactionCapsule(transferContract, ContractType.TransferContract);
    transactionCapsule.setReference(blockCapsule.getNum(), blockCapsule.getBlockId().getBytes());
    Assert.assertEquals(1, blockCapsule.getTimeStamp());

    long blockTimeStamp = blockCapsule.getTimeStamp();
    transactionCapsule.setExpiration(blockTimeStamp - 1);
    transactionCapsule.sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));

    GrpcAPI.Return result = wallet.broadcastTransaction(transactionCapsule.getInstance());
    Assert.assertEquals(response_code.TRANSACTION_EXPIRATION_ERROR, result.getCode());
  }

  @Test
  public void testExpireTransactionNew() {
    blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        System.currentTimeMillis(),
        ByteString.copyFromUtf8("testAddress"));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(blockCapsule.getNum());
    dbManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
    dbManager.updateRecentBlock(blockCapsule);
    initLocalWitness();
    byte[] address = Args.getLocalWitnesses()
        .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine());
    ByteString addressByte = ByteString.copyFrom(address);
    AccountCapsule accountCapsule =
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(addressByte).build());
    accountCapsule.setBalance(1000_000_000L);
    dbManager.getChainBaseManager().getAccountStore()
        .put(accountCapsule.createDbKey(), accountCapsule);

    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(addressByte)
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();
    TransactionCapsule transactionCapsule =
        new TransactionCapsule(transferContract, ContractType.TransferContract);
    transactionCapsule.setReference(blockCapsule.getNum(), blockCapsule.getBlockId().getBytes());

    transactionCapsule.setExpiration(System.currentTimeMillis());
    transactionCapsule.sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));

    GrpcAPI.Return result = wallet.broadcastTransaction(transactionCapsule.getInstance());
    Assert.assertEquals(response_code.TRANSACTION_EXPIRATION_ERROR, result.getCode());
  }

  @Test
  public void testTransactionApprovedList() {
    blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(blockCapsule.getNum());
    dbManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
    dbManager.updateRecentBlock(blockCapsule);
    initLocalWitness();

    byte[] address = Args.getLocalWitnesses()
        .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine());
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom(address))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();
    TransactionCapsule transactionCapsule =
        new TransactionCapsule(transferContract, ContractType.TransferContract);
    transactionCapsule.setReference(blockCapsule.getNum(), blockCapsule.getBlockId().getBytes());
    transactionCapsule.sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));

    TransactionApprovedList transactionApprovedList = wallet.getTransactionApprovedList(
        transactionCapsule.getInstance());
    Assert.assertTrue(
        transactionApprovedList.getResult().getMessage().contains("Account does not exist!"));

    ByteString addressByte = ByteString.copyFrom(address);
    AccountCapsule accountCapsule =
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(addressByte).build());
    accountCapsule.setBalance(1000_000_000L);
    dbManager.getChainBaseManager().getAccountStore()
        .put(accountCapsule.createDbKey(), accountCapsule);
    transactionApprovedList = wallet.getTransactionApprovedList(transactionCapsule.getInstance());
    Assert.assertEquals("", transactionApprovedList.getResult().getMessage());

    byte[] randomSig = org.tron.keystore.Wallet.generateRandomBytes(64);
    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearSignature()
        .addSignature(ByteString.copyFrom(randomSig)).build();
    transactionApprovedList = wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals(TransactionApprovedList.Result.response_code.SIGNATURE_FORMAT_ERROR,
        transactionApprovedList.getResult().getCode());

    randomSig = org.tron.keystore.Wallet.generateRandomBytes(65);
    transaction = transactionCapsule.getInstance().toBuilder().clearSignature()
        .addSignature(ByteString.copyFrom(randomSig)).build();
    transactionApprovedList = wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals(TransactionApprovedList.Result.response_code.COMPUTE_ADDRESS_ERROR,
        transactionApprovedList.getResult().getCode());
  }
}
