package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_PERIOD;
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
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class UnDelegateResourceActuatorTest {

  private static final String dbPath = "output_unDelegate_resource_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long delegateBalance = 1_000_000_000L;
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
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    AccountCapsule receiverCapsule = new AccountCapsule(ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)), AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);

    // clear delegate
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    dbManager.getDelegatedResourceStore().delete(DelegatedResourceCapsule.createDbKeyV2(
        owner, receiver, false));
    dbManager.getDelegatedResourceStore().delete(DelegatedResourceCapsule.createDbKeyV2(
        owner, receiver, true));
    dbManager.getDelegatedResourceAccountIndexStore().unDelegateV2(owner, receiver);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
  }

  public void delegateBandwidthForOwner() {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.addDelegatedFrozenV2BalanceForBandwidth(delegateBalance);
    dbManager.getAccountStore().put(owner, ownerCapsule);
    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.addAcquiredDelegatedFrozenV2BalanceForBandwidth(delegateBalance);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(delegateBalance / TRX_PRECISION);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        ByteString.copyFrom(owner),
        ByteString.copyFrom(receiver));
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(delegateBalance, 0);
    dbManager.getDelegatedResourceStore().put(
        DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false), delegatedResourceCapsule);

    dbManager.getDelegatedResourceAccountIndexStore().delegateV2(owner, receiver, 1);
  }

  public void delegateLockedBandwidthForOwner(long period) {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.addDelegatedFrozenV2BalanceForBandwidth(delegateBalance);
    dbManager.getAccountStore().put(owner, ownerCapsule);
    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.addAcquiredDelegatedFrozenV2BalanceForBandwidth(delegateBalance);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(delegateBalance / TRX_PRECISION);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
            ByteString.copyFrom(owner),
            ByteString.copyFrom(receiver));
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(delegateBalance, period);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(owner, receiver, true), delegatedResourceCapsule);

    dbManager.getDelegatedResourceAccountIndexStore().delegateV2(owner, receiver, 1);
  }

  public void delegateCpuForOwner() {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.addDelegatedFrozenV2BalanceForEnergy(delegateBalance);
    dbManager.getAccountStore().put(owner, ownerCapsule);
    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.addAcquiredDelegatedFrozenV2BalanceForEnergy(delegateBalance);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    dbManager.getDynamicPropertiesStore().addTotalEnergyWeight(delegateBalance / TRX_PRECISION);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        ByteString.copyFrom(owner),
        ByteString.copyFrom(receiver));
    delegatedResourceCapsule.setFrozenBalanceForEnergy(delegateBalance, 0);
    dbManager.getDelegatedResourceStore().put(
        DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false), delegatedResourceCapsule);

    dbManager.getDelegatedResourceAccountIndexStore().delegateV2(owner, receiver, 1);
  }

  private Any getDelegatedContractForBandwidth(String ownerAddress, long balance) {
    return Any.pack(UnDelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
        .setBalance(balance).build());
  }

  private Any getDelegatedContractForCpu(long balance) {
    return Any.pack(UnDelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
        .setResource(ResourceCode.ENERGY)
        .setBalance(balance).build());
  }

  // test start
  @Test
  public void testUnDelegateForBandwidth() {
    delegateBandwidthForOwner();

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setNetUsage(1_000_000_000);
    long nowSlot = dbManager.getChainBaseManager().getHeadSlot();
    receiverCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.setNetUsage(1_000_000_000);
    ownerCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(owner, ownerCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1000000000, ownerCapsule.getNetUsage());
      Assert.assertEquals(nowSlot, ownerCapsule.getLatestConsumeTime());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, receiverCapsule.getNetUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testLockedUnDelegateForBandwidth() {
    delegateLockedBandwidthForOwner(DELEGATE_PERIOD);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setNetUsage(1_000_000_000);
    long nowSlot = dbManager.getChainBaseManager().getHeadSlot();
    receiverCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.setNetUsage(1_000_000_000);
    ownerCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(owner, ownerCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
            getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());
      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));
      DelegatedResourceCapsule lockedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, true));

      Assert.assertNull(delegatedResourceCapsule);
      Assert.assertNotNull(lockedResourceCapsule);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check
      DelegatedResourceCapsule delegatedResourceCapsule1 = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));
      DelegatedResourceCapsule lockedResourceCapsule1 = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, true));
      Assert.assertNull(delegatedResourceCapsule1);
      Assert.assertNull(lockedResourceCapsule1);
      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1000000000, ownerCapsule.getNetUsage());
      Assert.assertEquals(nowSlot, ownerCapsule.getLatestConsumeTime());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, receiverCapsule.getNetUsage());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testLockedAndUnlockUnDelegateForBandwidth() {
    delegateLockedBandwidthForOwner(Long.MAX_VALUE);
    delegateBandwidthForOwner();

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setNetUsage(1_000_000_000);
    long nowSlot = dbManager.getChainBaseManager().getHeadSlot();
    receiverCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.setNetUsage(1_000_000_000);
    ownerCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(owner, ownerCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
            getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(2 * delegateBalance,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(2 * delegateBalance,
              ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(2 * delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());
      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));
      DelegatedResourceCapsule lockedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, true));
      Assert.assertNotNull(delegatedResourceCapsule);
      Assert.assertNotNull(lockedResourceCapsule);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check DelegatedResource
      DelegatedResourceCapsule delegatedResourceCapsule1 = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));
      DelegatedResourceCapsule lockedResourceCapsule1 = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, true));
      Assert.assertNull(delegatedResourceCapsule1);
      Assert.assertNotNull(lockedResourceCapsule1);
      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(1000000000, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(2 * delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(750000000, ownerCapsule.getNetUsage());
      Assert.assertEquals(nowSlot, ownerCapsule.getLatestConsumeTime());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(1000000000,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(250000000, receiverCapsule.getNetUsage());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testLockedUnDelegateBalanceForBandwidthInsufficient() {
    delegateLockedBandwidthForOwner(Long.MAX_VALUE);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setNetUsage(1_000_000_000);
    long nowSlot = dbManager.getChainBaseManager().getHeadSlot();
    receiverCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.setNetUsage(1_000_000_000);
    ownerCapsule.setLatestConsumeTime(nowSlot - 14400);
    dbManager.getAccountStore().put(owner, ownerCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
            getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
              receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());
      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false));
      DelegatedResourceCapsule lockedResourceCapsule = dbManager.getDelegatedResourceStore()
              .get(DelegatedResourceCapsule.createDbKeyV2(owner, receiver, true));
      Assert.assertNull(delegatedResourceCapsule);
      Assert.assertNotNull(lockedResourceCapsule);

      actuator.validate();
      Assert.fail();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("insufficient delegatedFrozenBalance(BANDWIDTH), "
              + "request=1000000000, unlock_balance=0", e.getMessage());
    }
  }

  @Test
  public void testPartialUnDelegateForBandwidth() {
    delegateBandwidthForOwner();

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setNetUsage(1_000_000_000);
    receiverCapsule.setLatestConsumeTime(dbManager.getChainBaseManager().getHeadSlot());
    dbManager.getAccountStore().put(receiver, receiverCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance / 2));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance / 2,
          ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance / 2,
          ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1000000000 / 2, ownerCapsule.getNetUsage());
      Assert.assertEquals(dbManager.getChainBaseManager().getHeadSlot(),
          ownerCapsule.getLatestConsumeTime());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(delegateBalance / 2,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(1000000000 / 2, receiverCapsule.getNetUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedResourceCapsule =
          dbManager.getDelegatedResourceStore().get(key);
      Assert.assertEquals(delegateBalance / 2,
          delegatedResourceCapsule.getFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(1, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testUnDelegatedForBandwidthWithDeletedReceiver() {
    delegateBandwidthForOwner();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    dbManager.getAccountStore().delete(receiver);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getNetUsage());

      // check receiver
      Assert.assertNull(dbManager.getAccountStore().get(receiver));

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedResourceCapsule =
          dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedResourceCapsule);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void testUnDelegatedForBandwidthWithRecreatedReceiver() {
    delegateBandwidthForOwner();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    dbManager.getAccountStore().delete(receiver);

    AccountCapsule receiverCapsule = new AccountCapsule(ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)), AccountType.Normal,
        initBalance);
    receiverCapsule.setAcquiredDelegatedFrozenV2BalanceForBandwidth(10L);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    receiverCapsule = dbManager.getAccountStore().get(receiver);
    Assert.assertEquals(10, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getNetUsage());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void testUnDelegatedForCpu() {
    delegateCpuForOwner();

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    long nowSlot = dbManager.getChainBaseManager().getHeadSlot();
    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setEnergyUsage(1_000_000_000);
    receiverCapsule.setLatestConsumeTimeForEnergy(nowSlot - 14400);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.setEnergyUsage(1_000_000_000);
    ownerCapsule.setLatestConsumeTimeForEnergy(nowSlot - 14400);
    dbManager.getAccountStore().put(owner, ownerCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getEnergyUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(nowSlot, ownerCapsule.getLatestConsumeTimeForEnergy());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0, receiverCapsule.getEnergyUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testPartialUnDelegatedForCpu() {
    delegateCpuForOwner();

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.setEnergyUsage(1_000_000_000);
    receiverCapsule.setLatestConsumeTimeForEnergy(dbManager.getChainBaseManager().getHeadSlot());
    dbManager.getAccountStore().put(receiver, receiverCapsule);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(delegateBalance / 2));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getEnergyUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance / 2, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance / 2, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000 / 2, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(dbManager.getChainBaseManager().getHeadSlot(),
          ownerCapsule.getLatestConsumeTimeForEnergy());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(delegateBalance / 2,
          receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(1_000_000_000 / 2, receiverCapsule.getEnergyUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertEquals(delegateBalance / 2,
          delegatedCapsule.getFrozenBalanceForEnergy());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(1, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testUnDelegatedForCpuWithDeletedReceiver() {
    delegateCpuForOwner();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    dbManager.getAccountStore().delete(receiver);

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getEnergyUsage());

      // check receiver
      Assert.assertNull(dbManager.getAccountStore().get(receiver));

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void testUnDelegatedForCpuWithRecreatedReceiver() {
    delegateCpuForOwner();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    dbManager.getAccountStore().delete(receiver);

    AccountCapsule receiverCapsule = new AccountCapsule(ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)), AccountType.Normal,
        initBalance);
    receiverCapsule.setAcquiredDelegatedFrozenV2BalanceForEnergy(10L);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    receiverCapsule = dbManager.getAccountStore().get(receiver);
    Assert.assertEquals(10, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(code.SUCESS, ret.getInstance().getRet());

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getEnergyUsage());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver, false);
      DelegatedResourceCapsule delegatedResourceCapsule =
          dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedResourceCapsule);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(owner);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().getV2Index(receiver);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void invalidOwnerAddress() {
    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS_INVALID, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void invalidOwnerAccount() {
    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidth(OWNER_ACCOUNT_INVALID, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] does not exist",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void noDelegateBalance() {
    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));

    try {
      actuator.validate();
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegated Resource does not exist", e.getMessage());
    }

    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getDelegatedContractForCpu(delegateBalance));

    try {
      actuator.validate();
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegated Resource does not exist", e.getMessage());
    }
  }

  @Test
  public void commonErrorCheck() {
    delegateBandwidthForOwner();
    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error, expected type [UnDelegateResourceContract], real type[");
    actuatorTest.invalidContractType();

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    Assert.assertEquals(0, accountCapsule.getFrozenV2BalanceForBandwidth());
    Assert.assertEquals(delegateBalance, accountCapsule.getDelegatedFrozenV2BalanceForBandwidth());
    Assert.assertEquals(delegateBalance, accountCapsule.getTronPower());

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    actuatorTest.setContract(getDelegatedContractForBandwidth(OWNER_ADDRESS, delegateBalance));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }
}

