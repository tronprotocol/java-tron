package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.VoteWitnessContract.Vote;
import org.tron.protos.Contract.WitnessCreateContract;
import org.tron.protos.Protocol.AccountType;

public class TransactionStoreTest {

  private static String dbPath = "output_TransactionStore_test";
  private static TransactionStore transactionStore;
  private static AnnotationConfigApplicationContext context;
  private static final byte[] data = TransactionStoreTest.randomBytes(21);
  private static Manager dbManager;


  private static final String URL = "https://tron.network";

  private static final String ACCOUNT_NAME = "ownerF";
  private static final String OWNER_ADDRESS =
      Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final String TO_ADDRESS =
      Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final long AMOUNT = 100;
  private static final String WITNESS_ADDRESS =
      Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    transactionStore = dbManager.getTransactionStore();

  }

  /**
   * get AccountCreateContract.
   */
  private AccountCreateContract getContract(String name, String address) {
    return AccountCreateContract.newBuilder()
        .setAccountName(ByteString.copyFromUtf8(name))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .build();
  }

  /**
   * get TransferContract.
   */
  private TransferContract getContract(long count, String owneraddress, String toaddress) {
    return TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
        .setAmount(count)
        .build();
  }

  /**
   * get WitnessCreateContract.
   */
  private WitnessCreateContract getWitnessContract(String address, String url) {
    return WitnessCreateContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setUrl(ByteString.copyFrom(ByteArray.fromString(url)))
        .build();
  }

  /**
   * get VoteWitnessContract.
   */
  private VoteWitnessContract getVoteWitnessContract(String address, String voteaddress,
      Long value) {
    return
        VoteWitnessContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .addVotes(Vote.newBuilder()
                .setVoteAddress(ByteString.copyFrom(ByteArray.fromHexString(voteaddress)))
                .setVoteCount(value).build())
            .build();
  }


  /**
   * put and get CreateAccountTransaction.
   */
  @Test
  public void CreateAccountTransactionStoreTest() {
    AccountCreateContract accountCreateContract = getContract(ACCOUNT_NAME,
        OWNER_ADDRESS);
    TransactionCapsule ret = new TransactionCapsule(accountCreateContract,
        dbManager.getAccountStore());
    transactionStore.put(data, ret);
    Assert.assertEquals("Store CreateAccountTransaction is error",
        transactionStore.get(data).getInstance(),
        ret.getInstance());
    Assert.assertTrue(transactionStore.has(data));
  }

  /**
   * put and get CreateWitnessTransaction.
   */
  @Test
  public void CreateWitnessTransactionStoreTest() {
    WitnessCreateContract witnessContract = getWitnessContract(OWNER_ADDRESS, URL);
    TransactionCapsule transactionCapsule = new TransactionCapsule(witnessContract);
    transactionStore.put(data, transactionCapsule);
    Assert.assertEquals("Store CreateWitnessTransaction is error",
        transactionStore.get(data).getInstance(),
        transactionCapsule.getInstance());
  }

  /**
   * put and get TransferTransaction.
   */
  @Test
  public void TransferTransactionStorenTest() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.AssetIssue,
            1000000L
        );
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    TransferContract transferContract = getContract(AMOUNT, OWNER_ADDRESS, TO_ADDRESS);
    TransactionCapsule transactionCapsule = new TransactionCapsule(transferContract,
        dbManager.getAccountStore());
    transactionStore.put(data, transactionCapsule);
    Assert.assertEquals("Store TransferTransaction is error",
        transactionStore.get(data).getInstance(),
        transactionCapsule.getInstance());
  }

  /**
   * put and get VoteWitnessTransaction.
   */

  @Test
  public void voteWitnessTransactionTest() {

    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            1_000_000_000_000L);
    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    ownerAccountFirstCapsule.setFrozen(frozenBalance, duration);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    VoteWitnessContract actuator = getVoteWitnessContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L);
    TransactionCapsule transactionCapsule = new TransactionCapsule(actuator);
    transactionStore.put(data, transactionCapsule);
    Assert.assertEquals("Store VoteWitnessTransaction is error",
        transactionStore.get(data).getInstance(),
        transactionCapsule.getInstance());
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }


  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Constant.ADD_PRE_FIX_BYTE_TESTNET;
    return result;
  }
}
