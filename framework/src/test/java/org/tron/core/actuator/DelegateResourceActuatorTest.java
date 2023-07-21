package org.tron.core.actuator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;
import static org.tron.protos.contract.Common.ResourceCode.TRON_POWER;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.BaseTest;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
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
import org.tron.core.state.WorldStateCallBack;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.state.store.DynamicPropertiesStateStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;

@Slf4j
public class DelegateResourceActuatorTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  @Autowired
  private WorldStateCallBack worldStateCallBack;
  @Autowired
  private ChainBaseManager chainBaseManager;

  static {
    dbPath = "output_delegate_resource_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    worldStateCallBack.setExecute(true);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);

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

  @After
  public void reset() {
    worldStateCallBack.setExecute(false);
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
            .setResource(BANDWIDTH)
            .setLock(lock)
            .build());
  }

  private Any getMaxDelegateLockPeriodContractForBandwidth(long unfreezeBalance, long lockPeriod) {
    return Any.pack(DelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
        .setBalance(unfreezeBalance)
        .setResource(BANDWIDTH)
        .setLock(true)
        .setLockPeriod(lockPeriod)
        .build());
  }

  private Any getMaxDelegateLockPeriodContractForEnergy(long unfreezeBalance, long lockPeriod) {
    return Any.pack(DelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
        .setBalance(unfreezeBalance)
        .setResource(ENERGY)
        .setLock(true)
        .setLockPeriod(lockPeriod)
        .build());
  }

  private Any getDelegateContractForCpu(long unfreezeBalance) {
    return Any.pack(
        DelegateResourceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .setBalance(unfreezeBalance)
            .setResource(ENERGY)
            .build());
  }

  private Any getDelegateContractForTronPower(long unfreezeBalance) {
    return Any.pack(
        DelegateResourceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .setBalance(unfreezeBalance)
            .setResource(TRON_POWER)
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals("delegateBalance must be less than available FreezeBandwidthV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      fail();
    }

    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForCpu(1_000_000_000L));
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals(
          "delegateBalance must be less than available FreezeEnergyV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals("delegateBalance must be less than available FreezeBandwidthV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals(
          "delegateBalance must be less than available FreezeEnergyV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      assertEquals("Do not allow delegate resources to contract addresses", e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      assertEquals("receiverAddress must not be the same as ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      assertEquals(code.SUCESS, ret.getInstance().getRet());
      AccountCapsule ownerCapsule =
          dbManager.getAccountStore().get(owner);

      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance - delegateBalance,
          ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance, ownerCapsule.getTronPower());

      WorldStateQueryInstance queryInstance = getQueryInstance();
      ownerCapsule = queryInstance.getAccount(owner);
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance - delegateBalance,
              ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(initBalance, ownerCapsule.getTronPower());

      AccountCapsule receiverCapsule =
          dbManager.getAccountStore().get(receiver);
      assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      assertEquals(0L, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      assertEquals(0L, receiverCapsule.getTronPower());

      receiverCapsule = queryInstance.getAccount(receiver);
      Assert.assertEquals(delegateBalance,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0L,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0L, receiverCapsule.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule
              .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                  ByteArray.fromHexString(RECEIVER_ADDRESS), false));

      Assert.assertEquals(delegateBalance, delegatedResourceCapsule.getFrozenBalanceForBandwidth());

      delegatedResourceCapsule = queryInstance.getDelegatedResource(DelegatedResourceCapsule
              .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                      ByteArray.fromHexString(RECEIVER_ADDRESS), false));
      Assert.assertEquals(delegateBalance, delegatedResourceCapsule.getFrozenBalanceForBandwidth());

      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore, totalNetWeightAfter);
      DynamicPropertiesStateStore stateStore = new DynamicPropertiesStateStore(queryInstance);
      Assert.assertEquals(totalNetWeightBefore, stateStore.getTotalNetWeight());


      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      assertEquals(1, ownerIndexCapsule.getToAccountsList().size());
      assertTrue(ownerIndexCapsule.getToAccountsList()
          .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule receiveCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      assertEquals(0, receiveCapsule.getToAccountsList().size());
      assertEquals(1, receiveCapsule.getFromAccountsList().size());
      assertTrue(receiveCapsule.getFromAccountsList()
          .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
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
      assertEquals(code.SUCESS, ret.getInstance().getRet());
      AccountCapsule ownerCapsule =
              dbManager.getAccountStore().get(owner);

      assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      assertEquals(initBalance - delegateBalance,
              ownerCapsule.getFrozenV2BalanceForBandwidth());
      assertEquals(initBalance, ownerCapsule.getTronPower());

      AccountCapsule receiverCapsule =
              dbManager.getAccountStore().get(receiver);
      assertEquals(delegateBalance,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      assertEquals(0L, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      assertEquals(0L, receiverCapsule.getTronPower());

      //check DelegatedResource
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
      assertEquals(delegateBalance, lockedResourceCapsule.getFrozenBalanceForBandwidth());
      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      assertEquals(totalNetWeightBefore, totalNetWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
              .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      assertEquals(1, ownerIndexCapsule.getToAccountsList().size());
      assertTrue(ownerIndexCapsule.getToAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule receiveCapsule = dbManager
              .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      assertEquals(0, receiveCapsule.getToAccountsList().size());
      assertEquals(1, receiveCapsule.getFromAccountsList().size());
      assertTrue(receiveCapsule.getFromAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMaxDelegateLockPeriodForBandwidthWrongLockPeriod1() {
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(86401);
    freezeBandwidthForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForBandwidth(
            delegateBalance, 370 * 24 * 3600));
    assertThrows("The lock period of delegate resources cannot exceed 1 year!",
        ContractValidateException.class, actuator::validate);
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

  @Test
  public void testMaxDelegateLockPeriodForBandwidthWrongLockPeriod2() {
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(864000L);
    freezeBandwidthForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForBandwidth(
            delegateBalance, 60));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      assertEquals(code.SUCESS, ret.getInstance().getRet());
    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }

    DelegateResourceActuator actuator1 = new DelegateResourceActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForBandwidth(
            delegateBalance, 30));
    assertThrows("The lock period for bandwidth this time cannot be less than the remaining"
            + " time[60000s] of the last lock period for bandwidth!",
        ContractValidateException.class, actuator1::validate);
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

  @Test
  public void testMaxDelegateLockPeriodForBandwidth() {
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(864000L);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(50_000L);
    freezeBandwidthForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForBandwidth(
            delegateBalance, 60));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      assertEquals(code.SUCESS, ret.getInstance().getRet());
    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }

    DelegateResourceActuator actuator1 = new DelegateResourceActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForBandwidth(
            delegateBalance, 60));

    TransactionResultCapsule ret1 = new TransactionResultCapsule();
    try {
      actuator1.validate();
      actuator1.execute(ret1);
      assertEquals(code.SUCESS, ret1.getInstance().getRet());
    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }
    DelegatedResourceCapsule lockedResourceCapsule = dbManager.getDelegatedResourceStore()
        .get(DelegatedResourceCapsule
            .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                ByteArray.fromHexString(RECEIVER_ADDRESS), true));
    long expireTimeForBandwidth = lockedResourceCapsule.getExpireTimeForBandwidth();
    assertEquals(50_000L + 60 * 3 * 1000, expireTimeForBandwidth);
    assertTrue(expireTimeForBandwidth > 60_000);
  }

  @Test
  public void testMaxDelegateLockPeriodForEnergy() {
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(864000L);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(50_000L);
    freezeCpuForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForEnergy(
            delegateBalance, 60));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      assertEquals(code.SUCESS, ret.getInstance().getRet());
    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }

    DelegateResourceActuator actuator1 = new DelegateResourceActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForEnergy(
            delegateBalance, 60));

    TransactionResultCapsule ret1 = new TransactionResultCapsule();
    try {
      actuator1.validate();
      actuator1.execute(ret1);
      assertEquals(code.SUCESS, ret1.getInstance().getRet());
    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }
    DelegatedResourceCapsule lockedResourceCapsule = dbManager.getDelegatedResourceStore()
        .get(DelegatedResourceCapsule
            .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                ByteArray.fromHexString(RECEIVER_ADDRESS), true));
    assertTrue(lockedResourceCapsule.getExpireTimeForEnergy() > 60_000);
  }

  @Test
  public void testMaxDelegateLockPeriodForEnergyWrongLockPeriod2() {
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(864000L);
    freezeCpuForOwner();
    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForEnergy(
            delegateBalance, 60));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      assertEquals(code.SUCESS, ret.getInstance().getRet());
    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
    }

    DelegateResourceActuator actuator1 = new DelegateResourceActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getMaxDelegateLockPeriodContractForEnergy(
            delegateBalance, 30));
    assertThrows("The lock period for energy this time cannot be less than the remaining"
            + " time[60000s] of the last lock period for energy!",
        ContractValidateException.class, actuator1::validate);
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
      assertEquals(code.SUCESS, ret.getInstance().getRet());
      AccountCapsule ownerCapsule =
          dbManager.getAccountStore().get(owner);

      assertEquals(initBalance, ownerCapsule.getBalance());
      assertEquals(0L, ownerCapsule.getFrozenBalance());
      assertEquals(0L, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      assertEquals(initBalance, ownerCapsule.getTronPower());

      AccountCapsule receiverCapsule =
          dbManager.getAccountStore().get(receiver);
      assertEquals(0L, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      assertEquals(0L, receiverCapsule.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));

      assertEquals(0L, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      assertEquals(delegateBalance, delegatedResourceCapsule.getFrozenBalanceForEnergy());

      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      assertEquals(totalEnergyWeightBefore, totalEnergyWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      assertEquals(1, ownerIndexCapsule.getToAccountsList().size());
      assertTrue(ownerIndexCapsule.getToAccountsList()
          .contains(ByteString.copyFrom(receiver)));

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      assertEquals(1,
              receiverIndexCapsule.getFromAccountsList().size());
      assertTrue(receiverIndexCapsule.getFromAccountsList()
          .contains(ByteString.copyFrom(owner)));

    } catch (ContractValidateException | ContractExeException e) {
      fail(e.getMessage());
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals("delegateBalance must be more than 1TRX", e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals("ResourceCode error, valid ResourceCode[BANDWIDTH、ENERGY]",
          e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals("delegateBalance must be less than available FreezeBandwidthV2 balance",
          e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void invalidReceiverAddress() {
    freezeBandwidthForOwner();
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(
            OWNER_ADDRESS, OWNER_ADDRESS_INVALID, 1_000_000_000L));
    assertThrows("Invalid receiverAddress", ContractValidateException.class, actuator::validate);
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
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      fail(e.getMessage());
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

  @Test
  public void testSupportDelegateResource() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(
                OWNER_ADDRESS,
                RECEIVER_ADDRESS,
                1_000_000_000L));
    assertThrows(
        "No support for resource delegate",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testSupportUnfreezeDelay() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0);
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegateContractForBandwidth(
            OWNER_ADDRESS,
            RECEIVER_ADDRESS,
            1_000_000_000L));
    assertThrows(
        "Not support Delegate resource transaction, need to be opened by the committee",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testErrorContract() {
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getErrorContract());
    assertThrows(
        "contract type error, expected type [DelegateResourceContract], "
            + "real type[WithdrawExpireUnfreezeContract]",
        ContractValidateException.class, actuator::validate);
  }

  private Any getErrorContract() {
    return Any.pack(BalanceContract.WithdrawExpireUnfreezeContract.newBuilder().setOwnerAddress(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build()
    );
  }

  private WorldStateQueryInstance getQueryInstance() {
    Assert.assertNotNull(worldStateCallBack.getTrie());
    worldStateCallBack.clear();
    worldStateCallBack.getTrie().commit();
    worldStateCallBack.getTrie().flush();
    return new WorldStateQueryInstance(worldStateCallBack.getTrie().getRootHashByte32(),
            chainBaseManager);
  }
}
