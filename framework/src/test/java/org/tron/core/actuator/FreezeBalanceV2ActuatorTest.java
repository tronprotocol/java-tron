package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.tron.core.config.Parameter.ChainConstant.TRANSFER_FEE;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class FreezeBalanceV2ActuatorTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;

  static {
    dbPath = "output_freeze_balance_v2_test";
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
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);

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
      Assert.assertEquals(owner.getFrozenV2BalanceForBandwidth(), frozenBalance);
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
    chainBaseManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
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
    chainBaseManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
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
