package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

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
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
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
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class UnfreezeBalanceActuatorTest {

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
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
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

  private Any getContractForBandwidth(String ownerAddress) {
    return Any.pack(UnfreezeBalanceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress))).build());
  }

  private Any getContractForCpu(String ownerAddress) {
    return Any.pack(UnfreezeBalanceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setResource(ResourceCode.ENERGY).build());
  }

  private Any getDelegatedContractForBandwidth(String ownerAddress, String receiverAddress) {
    return Any.pack(UnfreezeBalanceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress))).build());
  }

  private Any getDelegatedContractForCpu(String ownerAddress, String receiverAddress) {
    return Any.pack(UnfreezeBalanceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
        .setResource(ResourceCode.ENERGY).build());
  }

  private Any getContract(String ownerAddress, ResourceCode resourceCode) {
    return Any.pack(UnfreezeBalanceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setResource(resourceCode).build());
  }


  @Test
  public void testUnfreezeBalanceForBandwidth() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(frozenBalance, now);
    Assert.assertEquals(accountCapsule.getFrozenBalance(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getFrozenBalance(), 0);
      Assert.assertEquals(owner.getTronPower(), 0L);

      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore,
          totalNetWeightAfter + frozenBalance / 1000_000L);

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

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozenForEnergy(frozenBalance, now);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForEnergy(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpu(OWNER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getEnergyFrozenBalance(), 0);
      Assert.assertEquals(owner.getTronPower(), 0L);
      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore,
          totalEnergyWeightAfter + frozenBalance / 1000_000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForBandwidth() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      AccountCapsule receiverResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(RECEIVER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, receiverResult.getAllFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForBandwidthWithDeletedReceiver() {

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "Receiver Account[a0abd4b9367799eaa3197fecb144eb71de1e049150] does not exist");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert
          .assertEquals(0,
              delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());

    } catch (ContractValidateException e) {
      logger.error("", e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void testUnfreezeDelegatedBalanceForBandwidthWithRecreatedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getAccountStore().delete(receiver.createDbKey());
    receiver = new AccountCapsule(receiver.getAddress(), ByteString.EMPTY, AccountType.Normal);
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(10L);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);
    receiver = dbManager.getAccountStore().get(receiver.createDbKey());
    Assert.assertEquals(10, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(0);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "AcquiredDelegatedFrozenBalanceForBandwidth[10] < delegatedBandwidth[1000000000]");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());

      receiver = dbManager.getAccountStore().get(receiver.createDbKey());
      Assert.assertEquals(0, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());

    } catch (ContractValidateException e) {
      logger.error("", e);
      Assert.fail();
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  /**
   * when SameTokenName close,delegate balance frozen, unfreoze show error
   */
  @Test
  public void testUnfreezeDelegatedBalanceForBandwidthSameTokenNameClose() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance(BANDWIDTH)", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpu() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForEnergy(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      AccountCapsule receiverResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(RECEIVER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiverResult.getAllFrozenBalanceForEnergy());
    } catch (ContractValidateException e) {
      logger.error("", e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpuWithDeletedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForEnergy(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "Receiver Account[a0abd4b9367799eaa3197fecb144eb71de1e049150] does not exist");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForEnergy());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpuWithRecreatedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceCapsule.setFrozenBalanceForEnergy(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(0);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());
    receiver = new AccountCapsule(receiver.getAddress(), ByteString.EMPTY, AccountType.Normal);
    receiver.setAcquiredDelegatedFrozenBalanceForEnergy(10L);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);
    receiver = dbManager.getAccountStore().get(receiver.createDbKey());
    Assert.assertEquals(10, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "AcquiredDelegatedFrozenBalanceForEnergy[10] < delegatedEnergy[1000000000]");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForEnergy());
      receiver = dbManager.getAccountStore().get(receiver.createDbKey());
      Assert.assertEquals(0, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
    } catch (ContractValidateException e) {
      Assert.fail();
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS_INVALID));
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

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ACCOUNT_INVALID));
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
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS));
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
  public void notTimeToUnfreeze() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now + 60000);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("It's not time to unfreeze(BANDWIDTH).", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testClearVotes() {
    byte[] ownerAddressBytes = ByteArray.fromHexString(OWNER_ADDRESS);
    ByteString ownerAddress = ByteString.copyFrom(ownerAddressBytes);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddressBytes);
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS));
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
    votesCapsule.addNewVotes(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        100);
    dbManager.getVotesStore().put(ByteArray.fromHexString(OWNER_ADDRESS), votesCapsule);
    accountCapsule.setFrozen(1_000_000_000L, now);
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

  /*@Test
  public void InvalidTotalNetWeight(){
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(smallTatalResource);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(frozenBalance, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    Assert.assertTrue(frozenBalance/1000_000L > smallTatalResource );
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator(
            getContract(OWNER_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      Assert.assertTrue(dbManager.getDynamicPropertiesStore().getTotalNetWeight() >= 0);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void InvalidTotalEnergyWeight(){
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(smallTatalResource);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozenForEnergy(frozenBalance, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    Assert.assertTrue(frozenBalance/1000_000L > smallTatalResource );
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator(
            getContract(OWNER_ADDRESS, Contract.ResourceCode.ENERGY), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      Assert.assertTrue(dbManager.getDynamicPropertiesStore().getTotalEnergyWeight() >= 0);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }*/


  @Test
  public void commonErrorCheck() {
    UnfreezeBalanceActuator actuator = new UnfreezeBalanceActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error, expected type [UnfreezeBalanceContract], real type[");
    actuatorTest.invalidContractType();

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(frozenBalance, now);
    Assert.assertEquals(accountCapsule.getFrozenBalance(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    actuatorTest.setContract(getContractForBandwidth(OWNER_ADDRESS));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }

}

