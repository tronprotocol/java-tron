package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertThrows;
import static org.tron.core.Constant.CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE;
import static org.tron.core.Constant.CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE;
import static org.tron.core.config.Parameter.ChainConstant.ONE_YEAR_BLOCK_NUMBERS;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ForkController;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;

@Slf4j
public class ProposalCreateActuatorTest extends BaseTest {

  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://tron.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, Constant.TEST_CONF);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void initTest() {
    WitnessCapsule ownerWitnessFirstCapsule =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            10_000_000L,
            URL);
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            300_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    dbManager.getWitnessStore().put(ownerWitnessFirstCapsule.getAddress().toByteArray(),
        ownerWitnessFirstCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }

  private Any getContract(String address, HashMap<Long, Long> paras) {
    return Any.pack(
        ProposalCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .putAllParameters(paras)
            .build());
  }

  /**
   * first createProposal,result is success.
   */
  @Test
  public void successProposalCreate() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 1000000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      ProposalCapsule proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalCapsule);
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 1);
      Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
      Assert.assertEquals(proposalCapsule.getCreateTime(), 1000000);
      Assert.assertEquals(proposalCapsule.getExpirationTime(),
          261200000); // 2000000 + 3 * 4 * 21600000
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 10000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_INVALID, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 10000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_NOACCOUNT, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use WitnessStore not exists Address,result is failed,exception is "witness not exists".
   */
  @Test
  public void noWitness() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 10000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_SECOND, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witness[+OWNER_ADDRESS_NOWITNESS+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + OWNER_ADDRESS_SECOND + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use invalid parameter, result is failed, exception is "Bad chain parameter id".
   */
  @Test
  public void invalidPara() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(31L, 10000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Bad chain parameter id");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Bad chain parameter id",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    paras = new HashMap<>();
    paras.put(3L, 1 + 100_000_000_000_000_000L);
    actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Bad chain parameter value, valid range is [0,100_000_000_000_000_000L]");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Bad chain parameter value, valid range is [0,100000000000000000]",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    paras = new HashMap<>();
    paras.put(10L, -1L);
    actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(-1);
    try {
      actuator.validate();
      fail("This proposal has been executed before and is only allowed to be executed once");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "This proposal has been executed before and is only allowed to be executed once",
          e.getMessage());
    }

    paras.put(10L, -1L);
    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0);
    actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0);
    try {
      actuator.validate();
      fail("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
          e.getMessage());
    }
    // verify Proposal No. 78
    paras = new HashMap<>();
    paras.put(78L, 10L);
    actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    assertThrows(
        "Bad chain parameter id [MAX_DELEGATE_LOCK_PERIOD]",
        ContractValidateException.class, actuator::validate);

    actuator = new ProposalCreateActuator();
    ForkController forkController = Mockito.mock(ForkController.class);
    Mockito.when(forkController.pass(Mockito.any())).thenReturn(true);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(forkController)
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    dbManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(86400L);
    long maxDelegateLockPeriod = dbManager.getDynamicPropertiesStore().getMaxDelegateLockPeriod();
    assertThrows(
        "This value[MAX_DELEGATE_LOCK_PERIOD] is only allowed to be greater than "
            + maxDelegateLockPeriod + " and less than or equal to " + ONE_YEAR_BLOCK_NUMBERS + "!",
        ContractValidateException.class, actuator::validate);

    // verify Proposal No. 82
    paras = new HashMap<>();
    paras.put(82L, 0L);
    actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    assertThrows(
        "Bad chain parameter id [ALLOW_ENERGY_ADJUSTMENT]",
        ContractValidateException.class, actuator::validate);

    actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(forkController)
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    assertThrows(
        "This value[MAX_CREATE_ACCOUNT_TX_SIZE] is only allowed to be greater than or equal "
            + "to " + CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE + " and less than or equal to "
            + CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE + "!",
        ContractValidateException.class, actuator::validate);
  }

  /**
   * parameter size = 0 , result is failed, exception is "This proposal has no parameter.".
   */
  @Test
  public void emptyProposal() {
    HashMap<Long, Long> paras = new HashMap<>();
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("This proposal has no parameter");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This proposal has no parameter.",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void InvalidParaValue() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(10L, 1000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /*
   * two same proposal can work
   */
  @Test
  public void duplicateProposalCreateSame() {
    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0L);

    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 23 * 3600 * 1000L);
    paras.put(1L, 8_888_000_000L);
    paras.put(2L, 200_000L);
    paras.put(3L, 20L);
    paras.put(4L, 2048_000_000L);
    paras.put(5L, 64_000_000L);
    paras.put(6L, 64_000_000L);
    paras.put(7L, 64_000_000L);
    paras.put(8L, 64_000_000L);
    paras.put(9L, 1L);
    paras.put(10L, 1L);
    paras.put(11L, 64L);
    paras.put(12L, 64L);
    paras.put(13L, 64L);

    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    ProposalCreateActuator actuatorSecond = new ProposalCreateActuator();
    actuatorSecond.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    dbManager.getDynamicPropertiesStore().saveLatestProposalNum(0L);
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      actuatorSecond.validate();
      actuatorSecond.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 2L);
      ProposalCapsule proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(2L));
      Assert.assertNotNull(proposalCapsule);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  @Test
  public void commonErrorCheck() {

    ProposalCreateActuator actuator = new ProposalCreateActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [ProposalCreateContract],real type[");
    actuatorTest.invalidContractType();

    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 1000000L);
    actuatorTest.setContract(getContract(OWNER_ADDRESS_FIRST, paras));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No dbManager!");
    actuatorTest.nullDBManger();

  }


}
