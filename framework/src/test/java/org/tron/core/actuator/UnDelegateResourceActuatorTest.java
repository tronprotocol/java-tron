package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
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

  private static final String dbPath = "output_undelegate_resource_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long delegateBalance = 1_000_000_000L;
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

    // clear delegate
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    dbManager.getDelegatedResourceStore().delete(DelegatedResourceCapsule.createDbKeyV2(
        owner, receiver));
    dbManager.getDelegatedResourceAccountIndexStore().delete(
        DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner));
    dbManager.getDelegatedResourceAccountIndexStore().delete(
        DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver));

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
  }

  public void delegateBandwidthForOwner() {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.addDelegatedFrozenBalanceForBandwidth(delegateBalance);
    dbManager.getAccountStore().put(owner, ownerCapsule);
    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(delegateBalance);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(delegateBalance / TRX_PRECISION);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        ByteString.copyFrom(owner),
        ByteString.copyFrom(receiver));
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(delegateBalance, 0);
    dbManager.getDelegatedResourceStore().put(
        DelegatedResourceCapsule.createDbKeyV2(owner, receiver), delegatedResourceCapsule);


    byte[] ownerKey = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule =
        new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(owner));

    List<ByteString> toAccountsList = delegatedResourceAccountIndexCapsule.getToAccountsList();
    if (!toAccountsList.contains(ByteString.copyFrom(receiver))) {
      delegatedResourceAccountIndexCapsule.addToAccount(ByteString.copyFrom(receiver));
    }
    dbManager.getDelegatedResourceAccountIndexStore()
        .put(ownerKey, delegatedResourceAccountIndexCapsule);


    byte[] receiverKey = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
    delegatedResourceAccountIndexCapsule = new DelegatedResourceAccountIndexCapsule(
        ByteString.copyFrom(receiver));

    List<ByteString> fromAccountsList = delegatedResourceAccountIndexCapsule
        .getFromAccountsList();
    if (!fromAccountsList.contains(ByteString.copyFrom(owner))) {
      delegatedResourceAccountIndexCapsule.addFromAccount(ByteString.copyFrom(owner));
    }
    dbManager.getDelegatedResourceAccountIndexStore()
        .put(receiverKey, delegatedResourceAccountIndexCapsule);
  }

  public void delegateCpuForOwner() {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] receiver = ByteArray.fromHexString(RECEIVER_ADDRESS);
    AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
    ownerCapsule.addDelegatedFrozenBalanceForEnergy(delegateBalance);
    dbManager.getAccountStore().put(owner, ownerCapsule);
    AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiver);
    receiverCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(delegateBalance);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    dbManager.getDynamicPropertiesStore().addTotalEnergyWeight(delegateBalance / TRX_PRECISION);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        ByteString.copyFrom(owner),
        ByteString.copyFrom(receiver));
    delegatedResourceCapsule.setFrozenBalanceForEnergy(delegateBalance, 0);
    dbManager.getDelegatedResourceStore().put(
        DelegatedResourceCapsule.createDbKeyV2(owner, receiver), delegatedResourceCapsule);


    byte[] ownerKey = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule =
        new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(owner));

    List<ByteString> toAccountsList = delegatedResourceAccountIndexCapsule.getToAccountsList();
    if (!toAccountsList.contains(ByteString.copyFrom(receiver))) {
      delegatedResourceAccountIndexCapsule.addToAccount(ByteString.copyFrom(receiver));
    }
    dbManager.getDelegatedResourceAccountIndexStore()
        .put(ownerKey, delegatedResourceAccountIndexCapsule);


    byte[] receiverKey = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
    delegatedResourceAccountIndexCapsule = new DelegatedResourceAccountIndexCapsule(
        ByteString.copyFrom(receiver));

    List<ByteString> fromAccountsList = delegatedResourceAccountIndexCapsule
        .getFromAccountsList();
    if (!fromAccountsList.contains(ByteString.copyFrom(owner))) {
      delegatedResourceAccountIndexCapsule.addFromAccount(ByteString.copyFrom(owner));
    }
    dbManager.getDelegatedResourceAccountIndexStore()
        .put(receiverKey, delegatedResourceAccountIndexCapsule);
  }

  private Any getDelegatedContractForBandwidth(
      String ownerAddress, String receiverAddress, long balance) {
    return Any.pack(UnDelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
        .setBalance(balance).build());
  }

  private Any getDelegatedContractForCpu(
      String ownerAddress, String receiverAddress, long balance) {
    return Any.pack(UnDelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
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
        getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(1000000000, ownerCapsule.getNetUsage());
      Assert.assertEquals(nowSlot, ownerCapsule.getLatestConsumeTime());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0, receiverCapsule.getNetUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, receiverIndexCapsule.getToAccountsList().size());
      Assert.assertEquals(0, receiverIndexCapsule.getFromAccountsList().size());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
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
        getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance / 2));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getNetUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getNetUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance / 2,
          ownerCapsule.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance / 2,
          ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(1000000000 / 2, ownerCapsule.getNetUsage());
      Assert.assertEquals(dbManager.getChainBaseManager().getHeadSlot(),
          ownerCapsule.getLatestConsumeTime());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(delegateBalance / 2,
          receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(1000000000 / 2, receiverCapsule.getNetUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedResourceCapsule =
          dbManager.getDelegatedResourceStore().get(key);
      Assert.assertEquals(delegateBalance / 2,
          delegatedResourceCapsule.getFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
        getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(0, ownerCapsule.getNetUsage());

      // check receiver
      Assert.assertNull(dbManager.getAccountStore().get(receiver));

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedResourceCapsule =
          dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedResourceCapsule);

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
    receiverCapsule.setAcquiredDelegatedFrozenBalanceForBandwidth(10L);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    receiverCapsule = dbManager.getAccountStore().get(receiver);
    Assert.assertEquals(10, receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth());

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(0, ownerCapsule.getNetUsage());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(1_000_000_000, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getEnergyUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(1_000_000_000, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(nowSlot, ownerCapsule.getLatestConsumeTimeForEnergy());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0, receiverCapsule.getEnergyUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance / 2));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance,
          receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getTronPower());
      Assert.assertEquals(0, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(1_000_000_000, receiverCapsule.getEnergyUsage());

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(delegateBalance / 2, ownerCapsule.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(delegateBalance / 2, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(1_000_000_000 / 2, ownerCapsule.getEnergyUsage());
      Assert.assertEquals(dbManager.getChainBaseManager().getHeadSlot(),
          ownerCapsule.getLatestConsumeTimeForEnergy());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(delegateBalance / 2,
          receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(1_000_000_000 / 2, receiverCapsule.getEnergyUsage());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertEquals(delegateBalance / 2,
          delegatedCapsule.getFrozenBalanceForEnergy());

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(1, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(0, ownerCapsule.getEnergyUsage());

      // check receiver
      Assert.assertNull(dbManager.getAccountStore().get(receiver));

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedCapsule = dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedCapsule);

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
    receiverCapsule.setAcquiredDelegatedFrozenBalanceForEnergy(10L);
    dbManager.getAccountStore().put(receiver, receiverCapsule);
    receiverCapsule = dbManager.getAccountStore().get(receiver);
    Assert.assertEquals(10, receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy());

    UnDelegateResourceActuator actuator = new UnDelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // check owner
      AccountCapsule ownerCapsule = dbManager.getAccountStore().get(owner);
      Assert.assertEquals(0, ownerCapsule.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(delegateBalance, ownerCapsule.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(ownerCapsule.getTronPower(), delegateBalance);
      Assert.assertEquals(0, ownerCapsule.getEnergyUsage());

      // check receiver
      receiverCapsule = dbManager.getAccountStore().get(receiver);
      Assert.assertEquals(0, receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy());

      //check DelegatedResource
      byte[] key = DelegatedResourceCapsule.createDbKeyV2(owner, receiver);
      DelegatedResourceCapsule delegatedResourceCapsule =
          dbManager.getDelegatedResourceStore().get(key);
      Assert.assertNull(delegatedResourceCapsule);

      //check DelegatedResourceAccountIndex
      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(owner);
      DelegatedResourceAccountIndexCapsule ownerIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
      Assert.assertEquals(0, ownerIndexCapsule.getFromAccountsList().size());
      Assert.assertEquals(0, ownerIndexCapsule.getToAccountsList().size());

      key = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiver);
      DelegatedResourceAccountIndexCapsule receiverIndexCapsule = dbManager
          .getDelegatedResourceAccountIndexStore().get(key);
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
        .setAny(getDelegatedContractForBandwidth(
            OWNER_ADDRESS_INVALID, RECEIVER_ADDRESS, delegateBalance));
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
        getDelegatedContractForBandwidth(OWNER_ACCOUNT_INVALID, RECEIVER_ADDRESS, delegateBalance));
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
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegated Resource does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
    }

    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertEquals("delegated Resource does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail(e.getMessage());
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
    Assert.assertEquals(delegateBalance, accountCapsule.getDelegatedFrozenBalanceForBandwidth());
    Assert.assertEquals(delegateBalance, accountCapsule.getTronPower());

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    actuatorTest.setContract(getDelegatedContractForBandwidth(
        OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }
}

