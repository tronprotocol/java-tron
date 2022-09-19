package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.tron.core.config.Parameter.ChainConstant.TRANSFER_FEE;

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
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.Common.ResourceCode;



@Slf4j
public class FreezeBalanceV2ActuatorTest {

  private static final String dbPath = "output_freeze_balance_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
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
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);
  }

  private Any getContractV2ForBandwidth(String ownerAddress, long frozenBalance) {
    return Any.pack(
            BalanceContract.FreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setResource(ResourceCode.BANDWIDTH)
            .build());
  }

  private Any getContractForCpuV2(String ownerAddress, long frozenBalance) {
    return Any.pack(
            BalanceContract.FreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setResource(ResourceCode.ENERGY)
            .build());
  }


  private Any getContractForTronPowerV2(String ownerAddress, long frozenBalance) {
    return Any.pack(
            BalanceContract.FreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setResource(ResourceCode.TRON_POWER)
            .build());
  }

  private Any getDelegatedContractForBandwidthV2(String ownerAddress,
      long frozenBalance) {
    return Any.pack(
        BalanceContract.FreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .setFrozenBalance(frozenBalance)
            .build());
  }

  private Any getDelegatedContractForCpuV2(String ownerAddress,
      long frozenBalance) {
    return Any.pack(
        BalanceContract.FreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .setFrozenBalance(frozenBalance)
            .setResource(ResourceCode.ENERGY)
            .build());
  }

  @Test
  public void testFreezeBalanceForBandwidth() {
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ADDRESS, frozenBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(owner.getFrozenBalanceV2(), frozenBalance);
      Assert.assertEquals(frozenBalance, owner.getTronPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeBalanceForEnergy() {
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getAllFrozenBalanceForEnergy());
      Assert.assertEquals(frozenBalance, owner.getTronPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testFreezeDelegatedBalanceForBandwidthWithContractAddress() {
    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            AccountType.Contract,
            initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
            getDelegatedContractForBandwidthV2(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);

      AccountCapsule owner =
              dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
              - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForEnergy());
      Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getTronPower());

    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(), "Do not allow delegate resources to contract addresses");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }


  }

  @Test
  public void testFreezeDelegatedBalanceForBandwidth() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidthV2(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForEnergy());
      Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getTronPower());

      AccountCapsule receiver =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(frozenBalance, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiver.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule
              .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                  ByteArray.fromHexString(RECEIVER_ADDRESS)));

      Assert.assertEquals(frozenBalance, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore + frozenBalance / 1000_000L, totalNetWeightAfter);
      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner =
              dbManager.getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(OWNER_ADDRESS)));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(1, delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(RECEIVER_ADDRESS)));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert
          .assertEquals(1,
              delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));


    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeDelegatedBalanceForCpuSameNameTokenActive() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
            getDelegatedContractForCpuV2(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(frozenBalance, owner.getTronPower());

      AccountCapsule receiver =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiver.getTronPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule
              .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
                  ByteArray.fromHexString(RECEIVER_ADDRESS)));

      Assert.assertEquals(0L, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, delegatedResourceCapsule.getFrozenBalanceForEnergy());

      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore + frozenBalance / 1000_000L,
          totalEnergyWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(OWNER_ADDRESS)));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(1, delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(RECEIVER_ADDRESS)));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert
          .assertEquals(1,
              delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeDelegatedBalanceForCpuSameNameTokenClose() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForCpuV2(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getAllFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getAllFrozenBalanceForEnergy());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForEnergy());


      AccountCapsule receiver =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiver.getTronPower());

      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore + frozenBalance / 1000_000L,
          totalEnergyWeightAfter);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void freezeLessThanZero() {
    long frozenBalance = -1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ADDRESS, frozenBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be positive", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void freezeMoreThanBalance() {
    long frozenBalance = 11_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be less than accountBalance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ADDRESS_INVALID, frozenBalance));

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
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ACCOUNT_INVALID, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void lessThan1TrxTest() {
    long frozenBalance = 1;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be more than 1TRX", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  //@Test
  public void moreThanFrozenNumber() {
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractV2ForBandwidth(OWNER_ADDRESS, frozenBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      long maxFrozenNumber = ChainConstant.MAX_FROZEN_NUMBER;
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("max frozen number is: " + maxFrozenNumber, e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void commonErrorCheck() {
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [FreezeBalanceV2Contract],real type[");
    actuatorTest.invalidContractType();

    long frozenBalance = 1_000_000_000L;
    actuatorTest.setContract(getContractV2ForBandwidth(OWNER_ADDRESS, frozenBalance));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }


  @Test
  public void testFreezeBalanceForEnergyWithoutOldTronPowerAfterNewResourceModel() {
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractForCpuV2(OWNER_ADDRESS, frozenBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(-1L, owner.getInstance().getOldTronPower());
      Assert.assertEquals(0L, owner.getAllTronPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testFreezeBalanceForEnergyWithOldTronPowerAfterNewResourceModel() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractForCpuV2(OWNER_ADDRESS, frozenBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    AccountCapsule owner =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setFrozenForEnergy(100L,0L);
    chainBaseManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS),owner);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(100L, owner.getInstance().getOldTronPower());
      Assert.assertEquals(100L, owner.getAllTronPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testFreezeBalanceForTronPowerWithOldTronPowerAfterNewResourceModel() {
    long frozenBalance = 1_000_000_000L;
    FreezeBalanceV2Actuator actuator = new FreezeBalanceV2Actuator();
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractForTronPowerV2(OWNER_ADDRESS, frozenBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    AccountCapsule owner =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setFrozenForEnergy(100L,0L);
    chainBaseManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS),owner);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(100L, owner.getInstance().getOldTronPower());
      Assert.assertEquals(100L, owner.getTronPower());
      Assert.assertEquals(frozenBalance + 100, owner.getAllTronPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


}
