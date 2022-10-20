package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class UnfreezeBalanceV2ActuatorTest {

  private static final String dbPath = "output_unfreeze_balance_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;
  private static final long smallTatalResource = 100L;
  private static Manager dbManager;
  private static TronApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    AccountCapsule receiverCapsule = new AccountCapsule(ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)), AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);
  }

  private Any getContractForBandwidthV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
                    .setOwnerAddress(
                            ByteString.copyFrom(ByteArray.fromHexString(ownerAddress))
                    )
                    .setUnfreezeBalance(unfreezeBalance)
                    .setResource(ResourceCode.BANDWIDTH)
                    .build()
    );
  }

  private Any getContractForCpuV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.ENERGY).build());
  }

  private Any getContractForTronPowerV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.TRON_POWER).build());
  }

  private Any getDelegatedContractForBandwidth(
          String ownerAddress, long unfreezeBalance
  ) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.BANDWIDTH).build());
  }

  private Any getDelegatedContractForCpu(
          String ownerAddress, String receiverAddress, long unfreezeBalance
  ) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.ENERGY).build());
  }

  private Any getContract(String ownerAddress, ResourceCode resourceCode, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(resourceCode).build());
  }


  private Any getContractForTronPowerV2_001(String ownerAddress, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setUnfreezeBalance(unfreezeBalance).build());
  }

  @Test
  public void testUnfreezeBalanceForBandwidth() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    long unfreezeBalance = frozenBalance - 100;

    Assert.assertEquals(accountCapsule.getFrozenV2BalanceForBandwidth(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getFrozenV2BalanceForBandwidth(), 100);
      Assert.assertEquals(owner.getTronPower(), 100L);

      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore,
              totalNetWeightAfter + (frozenBalance - 100) / 1000_000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForEnergy() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForEnergy(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
              .get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getAllFrozenBalanceForEnergy(), 100);
      Assert.assertEquals(owner.getTronPower(), 100);
      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore,
          totalEnergyWeightAfter + (frozenBalance - 100) / 1000_000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void invalidOwnerAddress() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS_INVALID, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ACCOUNT_INVALID, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] does not exist",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noFrozenBalance() {
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    long unfreezeBalance = frozenBalance;
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance(BANDWIDTH)", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testClearVotes() {
    byte[] ownerAddressBytes = ByteArray.fromHexString(OWNER_ADDRESS);
    ByteString ownerAddress = ByteString.copyFrom(ownerAddressBytes);
    long unfreezeBalance = frozenBalance;
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddressBytes);
    accountCapsule.addFrozenBalanceForBandwidthV2(1_000_000_000L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getVotesStore().reset();
    Assert.assertNull(dbManager.getVotesStore().get(ownerAddressBytes));
    try {
      actuator.validate();
      actuator.execute(ret);
      VotesCapsule votesCapsule = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesCapsule);
      Assert.assertEquals(0, votesCapsule.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // if had votes
    List<Vote> oldVotes = new ArrayList<Vote>();
    VotesCapsule votesCapsule = new VotesCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), oldVotes);
    votesCapsule.addNewVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100);
    dbManager.getVotesStore().put(ByteArray.fromHexString(OWNER_ADDRESS), votesCapsule);
    accountCapsule.addFrozenBalanceForBandwidthV2(1_000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      actuator.validate();
      actuator.execute(ret);
      votesCapsule = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesCapsule);
      Assert.assertEquals(0, votesCapsule.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void commonErrorCheck() {
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();
    long unfreezeBalance = frozenBalance;

    Any invalidContractTypes = Any.pack(
            AssetIssueContractOuterClass.AssetIssueContract.newBuilder().build()
      );
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error, expected type [UnfreezeBalanceContract], real type[");
    actuatorTest.invalidContractType();

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForBandwidth(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    actuatorTest.setContract(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }


  @Test
  public void testUnfreezeBalanceForEnergyWithOldTronPowerAfterNewResourceModel() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.setOldTronPower(frozenBalance);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForEnergy(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getVotesList().size(), 0L);
      Assert.assertEquals(owner.getInstance().getOldTronPower(), -1L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForEnergyWithoutOldTronPowerAfterNewResourceModel() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.setOldTronPower(-1L);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getVotesList().size(), 1L);
      Assert.assertEquals(owner.getInstance().getOldTronPower(), -1L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForTronPowerWithOldTronPowerAfterNewResourceModel() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.addFrozenForTronPowerV2(frozenBalance);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForTronPowerV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getVotesList().size(), 0L);
      Assert.assertEquals(owner.getInstance().getOldTronPower(), -1L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForTronPowerWithOldTronPowerAfterNewResourceModelError() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.addFrozenForTronPowerV2(frozenBalance);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForTronPowerV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      //Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    }
  }


  @Test
  public void testUnfreezeBalanceCheckExistFreezedBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.addFrozenForTronPowerV2(frozenBalance);
    //accountCapsule.addDelegatedFrozenBalanceForBandwidthV2(frozenBalance);
    //accountCapsule.addDelegatedFrozenBalanceForEnergyV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    boolean bret1 = actuator.checkExistFreezedBalance(
            accountCapsule, ResourceCode.BANDWIDTH);
    Assert.assertTrue(true == bret1);
    boolean bret2 = actuator.checkExistFreezedBalance(
            accountCapsule, ResourceCode.ENERGY);
    Assert.assertTrue(true == bret2);
    boolean bret3 = actuator.checkExistFreezedBalance(
            accountCapsule, ResourceCode.TRON_POWER);
    Assert.assertTrue(true == bret3);

  }


  @Test
  public void testUnfreezeBalanceCheckUnfreezeBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.BANDWIDTH)
            .build();
    boolean bret1 = actuator.checkUnfreezeBalance(
            accountCapsule, unfreezeBalanceV2Contract, ResourceCode.BANDWIDTH
    );
    Assert.assertTrue(true == bret1);
  }


  @Test
  public void testUnfreezeBalanceGetFreezeType() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .build();

    ResourceCode freezeType = unfreezeBalanceV2Contract.getResource();

    Assert.assertTrue(ResourceCode.TRON_POWER.equals(freezeType));
  }

  @Test
  public void testUnfreezeBalanceCalcUnfreezeExpireTime() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .build();

    long ret = actuator.calcUnfreezeExpireTime(now);

    Assert.assertTrue(true);
  }

  @Test
  public void testUnfreezeBalanceUpdateAccountFrozenInfo() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .build();

    actuator.updateAccountFrozenInfo(
            ResourceCode.BANDWIDTH, accountCapsule, unfreezeBalance
    );

    Assert.assertTrue(accountCapsule.getAllFrozenBalanceForBandwidth() == 1);
  }


  @Test
  public void testUnfreezeBalanceUnfreezeExpire() {

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long balance = accountCapsule.getBalance();
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            1,
            now + 19 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            10,
            now + 31 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.ENERGY,
            20,
            now + 32 * FROZEN_PERIOD
    );

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now + 31 * FROZEN_PERIOD);
    actuator.unfreezeExpire(accountCapsule,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp());

    Assert.assertEquals(accountCapsule.getBalance(), balance + 11);
    Assert.assertEquals(accountCapsule.getUnfrozenV2List().size(), 1);
    Assert.assertEquals(accountCapsule.getUnfrozenV2List().get(0).getUnfreezeAmount(), 20);
  }


  @Test
  public void testAddTotalResourceWeight() {

    long now = System.currentTimeMillis();
    long total = frozenBalance;
    dbManager.getDynamicPropertiesStore().saveTotalTronPowerWeight(total);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long balance = accountCapsule.getBalance();

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                    .setUnfreezeBalance(unfreezeBalance)
                    .setResource(ResourceCode.TRON_POWER)
                    .build();

    actuator.updateTotalResourceWeight(unfreezeBalanceV2Contract, unfreezeBalance);

    Assert.assertEquals(total - unfreezeBalance / TRX_PRECISION,
            dbManager.getDynamicPropertiesStore().getTotalTronPowerWeight());

  }

  @Test
  public void testUnfreezeBalanceUnfreezeCount() {

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long balance = accountCapsule.getBalance();
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            1,
            now + 19 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            10,
            now + 31 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.ENERGY,
            20,
            now + 32 * FROZEN_PERIOD
    );

    int count = accountCapsule.getUnfreezingV2Count(now);
    Assert.assertEquals(3, count);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    dbManager.getDynamicPropertiesStore()
            .saveLatestBlockHeaderTimestamp(now + 32 * FROZEN_PERIOD);
    actuator.unfreezeExpire(accountCapsule,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp());


    int after_count = accountCapsule.getUnfreezingV2Count(now);
    Assert.assertEquals(0, after_count);

  }


}

