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
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WitnessUpdateActuatorTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_WitnessUpdate_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static final String OWNER_ADDRESS_NOT_WITNESS;
  private static final String OWNER_ADDRESS_NOT_WITNESS_ACCOUNT_NAME = "test_account1";
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String URL = "https://tron.network";
  private static final String NewURL = "https://tron.org";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOT_WITNESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    // address in accountStore and witnessStore
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountCapsule);
    WitnessCapsule ownerCapsule = new WitnessCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 10_000_000L, URL);
    dbManager.getWitnessStore().put(ByteArray.fromHexString(OWNER_ADDRESS), ownerCapsule);

    // address exist in accountStore, but is not witness
    AccountCapsule accountNotWitnessCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_NOT_WITNESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_NOT_WITNESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore()
        .put(ByteArray.fromHexString(OWNER_ADDRESS_NOT_WITNESS), accountNotWitnessCapsule);
    dbManager.getWitnessStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOT_WITNESS));

    // address does not exist in accountStore
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST));
  }

  private Any getContract(String address, String url) {
    return Any.pack(
        Contract.WitnessUpdateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setUpdateUrl(ByteString.copyFrom(ByteArray.fromString(url)))
            .build());
  }

  private Any getContract(String address, ByteString url) {
    return Any.pack(
        Contract.WitnessUpdateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setUpdateUrl(url)
            .build());
  }

  /**
   * Update witness,result is success.
   */
  @Test
  public void rightUpdateWitness() {
    WitnessUpdateActuator actuator = new WitnessUpdateActuator(getContract(OWNER_ADDRESS, NewURL),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessCapsule witnessCapsule = dbManager.getWitnessStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(witnessCapsule);
      Assert.assertEquals(witnessCapsule.getUrl(), NewURL);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalid Address update witness,result is failed,exception is "Invalid address".
   */
  @Test
  public void InvalidAddress() {
    WitnessUpdateActuator actuator = new WitnessUpdateActuator(
        getContract(OWNER_ADDRESS_INVALID, NewURL), dbManager);
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
   * use Invalid url createWitness,result is failed,exception is "Invalid url".
   */
  @Test
  public void InvalidUrlTest() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    //Url cannot empty
    try {
      WitnessUpdateActuator actuator = new WitnessUpdateActuator(
          getContract(OWNER_ADDRESS, ByteString.EMPTY), dbManager);
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //256 bytes
    String url256Bytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    //Url length can not greater than 256
    try {
      WitnessUpdateActuator actuator = new WitnessUpdateActuator(
          getContract(OWNER_ADDRESS, ByteString.copyFromUtf8(url256Bytes + "0")), dbManager);
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 1 byte url is ok.
    try {
      WitnessUpdateActuator actuator = new WitnessUpdateActuator(getContract(OWNER_ADDRESS, "0"),
          dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessCapsule witnessCapsule = dbManager.getWitnessStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(witnessCapsule);
      Assert.assertEquals(witnessCapsule.getUrl(), "0");
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 256 bytes url is ok.
    try {
      WitnessUpdateActuator actuator = new WitnessUpdateActuator(
          getContract(OWNER_ADDRESS, url256Bytes), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessCapsule witnessCapsule = dbManager.getWitnessStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(witnessCapsule);
      Assert.assertEquals(witnessCapsule.getUrl(), url256Bytes);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists Address createWitness,result is failed,exception is "Witness does
   * not exist"
   */
  @Test
  public void notExistWitness() {
    WitnessUpdateActuator actuator = new WitnessUpdateActuator(
        getContract(OWNER_ADDRESS_NOT_WITNESS, URL), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witness [+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * if account does not exist in accountStore, the test will throw a Exception
   */
  @Test
  public void notExistAccount() {
    WitnessUpdateActuator actuator = new WitnessUpdateActuator(
        getContract(OWNER_ADDRESS_NOTEXIST, URL), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account does not exist");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }
}