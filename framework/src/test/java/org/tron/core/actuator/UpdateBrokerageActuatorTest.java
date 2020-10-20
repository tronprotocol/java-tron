package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DelegationStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;

@Slf4j(topic = "actuator")
public class UpdateBrokerageActuatorTest {

  private static final String dbPath = "output_updatebrokerageactuator_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String OWNER_ADDRESS_INVALID;
  private static final int BROKEN_AGE = 10;
  private static TronApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "1234b9367799eaa3197fecb144eb71de1e049123";
    OWNER_ADDRESS_INVALID =
        Wallet.getAddressPreFixString() + "354394500882809695a8a687866e7";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {

    dbManager = context.getBean(Manager.class);
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

  @Before
  /**
   * set witness store, account store, dynamic store
   */
  public void initDB() {
    // allow dynamic store
    dbManager.getDynamicPropertiesStore().saveChangeDelegation(1);
    // set witness store
    WitnessCapsule witness =
        new WitnessCapsule(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    dbManager.getWitnessStore().put(ByteArray.fromHexString(OWNER_ADDRESS), witness);
    // set account store
    AccountCapsule account = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal);
    dbManager.getAccountStore()
        .put(ByteArray.fromHexString(OWNER_ADDRESS), account);

  }


  private Any getContract(String ownerAddress, int brokerage) {
    return Any.pack(UpdateBrokerageContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setBrokerage(brokerage)
        .build());
  }

  private void processAndCheckInvalid(UpdateBrokerageActuator actuator,
      TransactionResultCapsule ret,
      String failMsg,
      String expectedMsg) {
    try {
      actuator.validate();
      actuator.execute(ret);
      fail(failMsg);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (RuntimeException e) {
      Assert.assertTrue(e instanceof RuntimeException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    }
  }

  /**
   * success update broken age
   */
  @Test
  public void successUpdate() {

    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    Any contract = getContract(OWNER_ADDRESS, BROKEN_AGE);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(contract);
    byte[] OwnerAddress = ByteArray.fromHexString(OWNER_ADDRESS);

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      DelegationStore delegationStore = dbManager.getDelegationStore();
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(BROKEN_AGE, delegationStore.getBrokerage(OwnerAddress));
    } catch (ContractValidateException e) {
      logger.info(e.getMessage() + "   type" + contract.getClass());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * invalid owner address
   */
  @Test
  public void invalidOwnerAddress() {
    // initDBStore();
    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    Any contract = getContract(OWNER_ADDRESS_INVALID, BROKEN_AGE);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(contract);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "Invalid ownerAddress",
        "Invalid ownerAddress");
  }


  /**
   * invalid brokerage,too much
   */
  @Test
  public void invalidBrokerageUP() {

    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    Any contract = getContract(OWNER_ADDRESS, 101);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(contract);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "Invalid brokerage",
        "Invalid brokerage");
  }

  /**
   * invalid brokerage, too less
   */
  @Test
  public void invalidBrokerageBottom() {

    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    Any contract = getContract(OWNER_ADDRESS, -1);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(contract);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "Invalid brokerage",
        "Invalid brokerage");
  }

  /**
   * witness not exit in witnessStore
   */
  @Test
  public void noExistWitness() {

    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    Any contract = getContract(OWNER_ADDRESS_NOTEXIST, BROKEN_AGE);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(contract);
    byte[] ownerAddressNoExitDB = ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Not existed witness:");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not existed witness:" + Hex.toHexString(ownerAddressNoExitDB),
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      logger.info("final test execute ");
      // set witness store and make sure next test case will not throw this error again
      WitnessCapsule witness =
          new WitnessCapsule(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST)));
      dbManager.getWitnessStore().put(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST), witness);
      logger.info("after final test execute ");
    }

  }

  /**
   * account address not exit in DB
   */
  @Test
  public void noExistAccount() {

    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    Any contract = getContract(OWNER_ADDRESS_NOTEXIST, BROKEN_AGE);
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(contract);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Account does not exist");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      // set account store and make sure next test case will not throw this error again
      AccountCapsule account = new AccountCapsule(ByteString.copyFromUtf8("noExitOwner"),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST)), AccountType.Normal);
      dbManager.getAccountStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST), account);
    }

  }

  @Test
  public void nullDBManger() {
    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    actuator.setChainBaseManager(null)
        .setAny(getContract(OWNER_ADDRESS, BROKEN_AGE));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "No account store or dynamic store!",
        "No account store or dynamic store!");
  }

  @Test
  public void noContract() {

    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(null);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  @Test
  public void invalidContractType() {
    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    // create AssetIssueContract, not a valid ClearABI contract , which will throw e expectipon
    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(invalidContractTypes);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "contract type error",
        "contract type error, expected type [UpdateBrokerageContract], real type["
            + invalidContractTypes.getClass() + "]");
  }

  @Test
  public void nullTransationResult() {
    UpdateBrokerageActuator actuator = new UpdateBrokerageActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, BROKEN_AGE));
    TransactionResultCapsule ret = null;
    processAndCheckInvalid(actuator, ret, "TransactionResultCapsule is null",
        "TransactionResultCapsule is null");
  }

}
