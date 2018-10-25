package org.tron.common.runtime.vm;


import static junit.framework.TestCase.fail;
import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.vm.PrecompiledContracts.PrecompiledContract;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.FreezeBalanceActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Proposal.State;

@Slf4j
public class PrecompiledContractsTest {

  // common
  private static final DataWord voteContractAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010001");
  //  private static final DataWord freezeBalanceAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010002");
//  private static final DataWord unFreezeBalanceAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010003");
  private static final DataWord withdrawBalanceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010004");
  private static final DataWord proposalApproveAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010005");
  private static final DataWord proposalCreateAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010006");
  private static final DataWord proposalDeleteAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010007");
  private static final DataWord convertFromTronBytesAddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010008");
  private static final DataWord convertFromTronBase58AddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010009");

  private static TronApplicationContext context;
  private static Application appT;
  private static Manager dbManager;
  private static final String dbPath = "output_PrecompiledContracts_test";
  private static final String ACCOUNT_NAME = "account";
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_NAME = "witness";
  private static final String WITNESS_ADDRESS;
  private static final String WITNESS_ADDRESS_BASE = "548794500882809695a8a687866e76d4271a1abc";
  private static final String URL = "https://tron.network";

  // withdraw
  private static final long initBalance = 10_000_000_000L;
  private static final long allowance = 32_000_000L;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + WITNESS_ADDRESS_BASE;

  }


  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    // witness: witnessCapsule
    WitnessCapsule witnessCapsule =
        new WitnessCapsule(
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            10L,
            URL);
    // witness: AccountCapsule
    AccountCapsule witnessAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            AccountType.Normal,
            initBalance);
    // some normal account
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            10_000_000_000_000L);

    dbManager.getAccountStore()
        .put(witnessAccountCapsule.getAddress().toByteArray(), witnessAccountCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }


  private Any getFreezeContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        Contract.FreezeBalanceContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  private PrecompiledContract createPrecompiledContract(DataWord addr, String ownerAddress) {
    PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
    contract.setCallerAddress(convertToTronAddress(Hex.decode(ownerAddress)));
    contract.setDeposit(DepositImpl.createRoot(dbManager));
    ProgramResult programResult = new ProgramResult();
    contract.setResult(programResult);
    return contract;
  }

  //@Test
  public void voteWitnessNativeTest()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ContractValidateException, ContractExeException {
    PrecompiledContract contract = createPrecompiledContract(voteContractAddr, OWNER_ADDRESS);
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    byte[] witnessAddressBytes = new byte[32];
    byte[] witnessAddressBytes21 = Hex.decode(WITNESS_ADDRESS);
    System.arraycopy(witnessAddressBytes21, 0, witnessAddressBytes,
        witnessAddressBytes.length - witnessAddressBytes21.length, witnessAddressBytes21.length);

    DataWord voteCount = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000001");
    byte[] voteCountBytes = voteCount.getData();
    byte[] data = new byte[witnessAddressBytes.length + voteCountBytes.length];
    System.arraycopy(witnessAddressBytes, 0, data, 0, witnessAddressBytes.length);
    System.arraycopy(voteCountBytes, 0, data, witnessAddressBytes.length, voteCountBytes.length);

    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    Any freezeContract = getFreezeContract(OWNER_ADDRESS, frozenBalance, duration);
    Constructor<FreezeBalanceActuator> constructor =
        FreezeBalanceActuator.class
            .getDeclaredConstructor(Any.class, dbManager.getClass());
    constructor.setAccessible(true);
    FreezeBalanceActuator freezeBalanceActuator = constructor
        .newInstance(freezeContract, dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    freezeBalanceActuator.validate();
    freezeBalanceActuator.execute(ret);
    contract.setDeposit(deposit);
    Boolean result = contract.execute(data).getLeft();
    deposit.commit();
    Assert.assertEquals(1,
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteCount());
    Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteAddress().toByteArray());
    Assert.assertEquals(true, result);
  }

  //@Test
  public void withdrawBalanceNativeTest() {
    PrecompiledContract contract = createPrecompiledContract(withdrawBalanceAddr, WITNESS_ADDRESS);

    long now = System.currentTimeMillis();
    Deposit deposit = DepositImpl.createRoot(dbManager);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] address = ByteArray.fromHexString(WITNESS_ADDRESS);
    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
    Assert.assertEquals(allowance, accountCapsule.getAllowance());
    Assert.assertEquals(0, accountCapsule.getLatestWithdrawTime());

    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address),
        100, "http://baidu.com");
    dbManager.getWitnessStore().put(address, witnessCapsule);
    contract.setDeposit(deposit);
    contract.execute(new byte[0]);
    deposit.commit();
    AccountCapsule witnessAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(WITNESS_ADDRESS));
    Assert.assertEquals(initBalance + allowance, witnessAccount.getBalance());
    Assert.assertEquals(0, witnessAccount.getAllowance());
    Assert.assertNotEquals(0, witnessAccount.getLatestWithdrawTime());
  }


  //@Test
  public void proposalTest() {

    try {
      /*
       *  create proposal Test
       */
      DataWord key = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000000");
      // 1000000 == 0xF4240
      DataWord value = new DataWord(
          "00000000000000000000000000000000000000000000000000000000000F4240");
      byte[] data4Create = new byte[64];
      System.arraycopy(key.getData(), 0, data4Create, 0, key.getData().length);
      System
          .arraycopy(value.getData(), 0, data4Create, key.getData().length, value.getData().length);
      PrecompiledContract createContract = createPrecompiledContract(proposalCreateAddr,
          WITNESS_ADDRESS);

      Assert.assertEquals(0, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      ProposalCapsule proposalCapsule;
      Deposit deposit1 = DepositImpl.createRoot(dbManager);
      createContract.setDeposit(deposit1);
      byte[] idBytes = createContract.execute(data4Create).getRight();
      long id = ByteUtil.byteArrayToLong(idBytes);
      deposit1.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalCapsule);
      Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      Assert.assertEquals(0, proposalCapsule.getApprovals().size());
      Assert.assertEquals(1000000, proposalCapsule.getCreateTime());
      Assert.assertEquals(261200000, proposalCapsule.getExpirationTime()
      ); // 2000000 + 3 * 4 * 21600000



      /*
       *  approve proposal Test
       */

      byte[] data4Approve = new byte[64];
      DataWord isApprove = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000001");
      System.arraycopy(idBytes, 0, data4Approve, 0, idBytes.length);
      System.arraycopy(isApprove.getData(), 0, data4Approve, idBytes.length,
          isApprove.getData().length);
      PrecompiledContract approveContract = createPrecompiledContract(proposalApproveAddr,
          WITNESS_ADDRESS);
      Deposit deposit2 = DepositImpl.createRoot(dbManager);
      approveContract.setDeposit(deposit2);
      approveContract.execute(data4Approve);
      deposit2.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(1, proposalCapsule.getApprovals().size());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
          proposalCapsule.getApprovals().get(0));

      /*
       *  delete proposal Test
       */
      PrecompiledContract deleteContract = createPrecompiledContract(proposalDeleteAddr,
          WITNESS_ADDRESS);
      Deposit deposit3 = DepositImpl.createRoot(dbManager);
      deleteContract.setDeposit(deposit3);
      deleteContract.execute(idBytes);
      deposit3.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(State.CANCELED, proposalCapsule.getState());

    } catch (ItemNotFoundException e) {
      Assert.fail();
    }
  }


  @Test
  public void convertFromTronBytesAddressNativeTest() {
//    PrecompiledContract contract = createPrecompiledContract(convertFromTronBytesAddressAddr, WITNESS_ADDRESS);
//    byte[] solidityAddress = contract.execute(new DataWord(WITNESS_ADDRESS).getData()).getRight();
//    Assert.assertArrayEquals(solidityAddress,new DataWord(Hex.decode(WITNESS_ADDRESS_BASE)).getData());
  }

  //@Test
  public void convertFromTronBase58AddressNative() {
    // 27WnTihwXsqCqpiNedWvtKCZHsLjDt4Hfmf  TestNet address
    DataWord word1 = new DataWord(
        "3237576e54696877587371437170694e65645776744b435a48734c6a44743448");
    DataWord word2 = new DataWord(
        "666d660000000000000000000000000000000000000000000000000000000000");

    byte[] data = new byte[35];
    System.arraycopy(word1.getData(), 0, data, 0, word1.getData().length);
    System.arraycopy(Arrays.copyOfRange(word2.getData(), 0, 3), 0, data, word1.getData().length, 3);
    PrecompiledContract contract = createPrecompiledContract(convertFromTronBase58AddressAddr,
        WITNESS_ADDRESS);

    byte[] solidityAddress = contract.execute(data).getRight();
    Assert.assertArrayEquals(solidityAddress,
        new DataWord(Hex.decode(WITNESS_ADDRESS_BASE)).getData());
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

}
