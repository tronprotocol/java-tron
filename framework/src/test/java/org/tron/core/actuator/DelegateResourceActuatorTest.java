package org.tron.core.actuator;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
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
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class DelegateResourceActuatorTest {

  private static final String dbPath = "output_delegate_resource_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static Manager dbManager;
  private static final TronApplicationContext context;

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
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(owner),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(receiver),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    // clear delegate
    dbManager.getDelegatedResourceStore().delete(DelegatedResourceCapsule.createDbKeyV2(
        owner, receiver, false));
    dbManager.getDelegatedResourceStore().delete(DelegatedResourceCapsule.createDbKeyV2(
        owner, receiver, true));
    dbManager.getDelegatedResourceAccountIndexStore().unDelegateV2(owner, receiver);
  }

  public void freezeBandwidthForOwner() {
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.addFrozenBalanceForBandwidthV2(initBalance);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(initBalance / TRX_PRECISION);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  public void freezeCpuForOwner() {
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.addFrozenBalanceForEnergyV2(initBalance);
    dbManager.getDynamicPropertiesStore().addTotalEnergyWeight(initBalance / TRX_PRECISION);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private Any getDelegateContractForBandwidth(String ownerAddress, String receiveAddress,
                                              long unfreezeBalance) {
    return getLockedDelegateContractForBandwidth(ownerAddress, receiveAddress,
            unfreezeBalance, false);
  }

  private Any getLockedDelegateContractForBandwidth(String ownerAddress, String receiveAddress,
                                              long unfreezeBalance, boolean lock) {
    return Any.pack(DelegateResourceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiveAddress)))
            .setBalance(unfreezeBalance)
            .setResource(ResourceCode.BANDWIDTH)
            .setLock(lock)
            .build());
  }

  private Any getDelegateContractForCpu(long unfreezeBalance) {
    return Any.pack(
        DelegateResourceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .setBalance(unfreezeBalance)
            .setResource(ResourceCode.ENERGY)
            .build());
  }

  private Any getDelegateContractForTronPower(long unfreezeBalance) {
    return Any.pack(
        DelegateResourceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .setBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            .build());
  }

  @Test
  public void testDelegateResourceWithNoFreeze() {
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(
            OWNER_ADDRESS,
            RECEIVER_ADDRESS,
            1_000_000_000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegateBalance must be less than available FreezeBandwidthV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail();
    }

    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForCpu(1_000_000_000L));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "delegateBalance must be less than available FreezeEnergyV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDelegateBandwidthWithUsage() {
    freezeBandwidthForOwner();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    ownerCapsule.setNetUsage(dynamicPropertiesStore.getTotalNetLimit() / 4);
    ownerCapsule.setLatestConsumeTime(dbManager.getChainBaseManager().getHeadSlot());
    dbManager.getAccountStore().put(owner, ownerCapsule);

    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, initBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegateBalance must be less than available FreezeBandwidthV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void testDelegateCpuWithUsage() {
    freezeCpuForOwner();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    ownerCapsule.setEnergyUsage(dynamicPropertiesStore.getTotalEnergyCurrentLimit() / 4);
    ownerCapsule.setLatestConsumeTimeForEnergy(dbManager.getChainBaseManager().getHeadSlot());
    dbManager.getAccountStore().put(owner, ownerCapsule);

    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForCpu(initBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "delegateBalance must be less than available FreezeEnergyV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void testDelegateResourceWithContractAddress() {
    freezeBandwidthForOwner();
    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            AccountType.Contract,
            initBalance);
    dbManager.getAccountStore().put(ByteArray.fromHexString(RECEIVER_ADDRESS), receiverCapsule);


    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, 1_000_000_000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertEquals("Do not allow delegate resources to contract addresses", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDelegateResourceToSelf() {
    freezeBandwidthForOwner();

    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(OWNER_ADDRESS, OWNER_ADDRESS, 1_000_000_000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertEquals("receiverAddress must not be the same as ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDelegateResourceForBandwidth() {
    freezeBandwidthForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());
      AccountCapsule ownerCapsule =
          dbManager.getAccountStore().get(owner);

      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance - delegateBalance,
          ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance, ownerCapsule.getTronPower());

      AccountCapsule receiverCapsule =
          dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0L, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0L, receiverCapsule.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule
              .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                  ByteArray.fromHexString(RECEIVER_ADDRESS), false));

      Assert.assertEquals(delegateBalance, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore, totalNetWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());
      Assert.assertTrue(ownerIndexCapsule.getToAccountsList()
          .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule receiveCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiveCapsule.getToAccountsList().size());
      Assert.assertEquals(1, receiveCapsule.getFromAccountsList().size());
      Assert.assertTrue(receiveCapsule.getFromAccountsList()
          .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testLockedDelegateResourceForBandwidth() {
    freezeBandwidthForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
            getLockedDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS,
                    delegateBalance, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());
      AccountCapsule ownerCapsule =
              dbManager.getAccountStore().get(owner);

      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance - delegateBalance,
              ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance, ownerCapsule.getTronPower());

      AccountCapsule receiverCapsule =
              dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(delegateBalance,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0L, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0L, receiverCapsule.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule
                      .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                              ByteArray.fromHexString(RECEIVER_ADDRESS), false));
      DelegatedResourceCapsule lockedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule
                      .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                              ByteArray.fromHexString(RECEIVER_ADDRESS), true));
      Assert.assertNull(delegatedResourceCapsule);
      Assert.assertNotNull(lockedResourceCapsule);
      Assert.assertNotEquals(0, lockedResourceCapsule.getExpireTimeForBandwidth());
      Assert.assertEquals(delegateBalance, lockedResourceCapsule.getFrozenBalanceForBandwidth());
      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore, totalNetWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
              .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());
      Assert.assertTrue(ownerIndexCapsule.getToAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule receiveCapsule = dbManager
              .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiveCapsule.getToAccountsList().size());
      Assert.assertEquals(1, receiveCapsule.getFromAccountsList().size());
      Assert.assertTrue(receiveCapsule.getFromAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDelegateResourceForCpu() {
    freezeCpuForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForCpu(delegateBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());
      AccountCapsule ownerCapsule =
          dbManager.getAccountStore().get(owner);

      Assert.assertEquals(initBalance, ownerCapsule.getBalance());
      Assert.assertEquals(0L, ownerCapsule.getFrozenBalance());
      Assert.assertEquals(0L, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(initBalance, ownerCapsule.getTronPower());

      AccountCapsule receiverCapsule =
          dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0L, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0L, receiverCapsule.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));

      Assert.assertEquals(0L, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance, delegatedResourceCapsule.getFrozenBalanceForEnergy());

      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore, totalEnergyWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert
          .assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());
      Assert.assertTrue(ownerIndexCapsule.getToAccountsList()
          .contains(ByteString.copyFrom(receiver)));

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert
          .assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert
          .assertEquals(1,
              receiverIndexCapsule.getFromAccountsList().size());
      Assert.assertTrue(receiverIndexCapsule.getFromAccountsList()
          .contains(ByteString.copyFrom(owner)));

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void delegateLessThanZero() {
    long delegateBalance = -1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(
            OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegateBalance must be more than 1TRX", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void delegateTronPower() {
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForTronPower(delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("ResourceCode error, valid ResourceCode[BANDWIDTH、ENERGY]",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void delegateMoreThanBalance() {
    long delegateBalance = 11_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegateBalance must be less than available FreezeBandwidthV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void invalidOwnerAddress() {
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(
            OWNER_ADDRESS_INVALID, RECEIVER_ADDRESS, 1_000_000_000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void invalidOwnerAccount() {
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(
            OWNER_ACCOUNT_INVALID, RECEIVER_ADDRESS, 1_000_000_000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void commonErrorCheck() {
    freezeBandwidthForOwner();
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [DelegateResourceContract],real type[");
    actuatorTest.invalidContractType();

    actuatorTest.setContract(
        getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, 1_000_000_000L));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }
}
