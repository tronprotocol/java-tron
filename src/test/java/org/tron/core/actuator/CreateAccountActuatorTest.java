package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class CreateAccountActuatorTest {
  private static Manager dbManager;
  private static Any contract;
  private static final String dbPath = "output_CreateAccountTest";

  private static final String ACCOUNT_NAME_FRIST = "ownerF";
  private static final String OWNER_ADDRESS_FRIST = "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND = "548794500882809695a8a687866e76d4271a1abc";

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Configuration.getByPath("config-junit.conf"));
    dbManager = new Manager();
    dbManager.init();
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
        ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND), AccountType.AssetIssue);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_FRIST));
  }

  private Any getContract(String name, String address) {
    return Any.pack(
        Contract.AccountCreateContract
            .newBuilder()
            .setAccountName(ByteString.copyFromUtf8(name))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }

  /**
   * Unit test.
   */
  @Test
  public void firstCreateAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator(
        getContract(ACCOUNT_NAME_FRIST, OWNER_ADDRESS_FRIST), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS_FRIST));
      Assert.assertNotNull(accountCapsule);
      Assert.assertEquals(accountCapsule.getInstance().getAccountName(),
          ByteString.copyFromUtf8(ACCOUNT_NAME_FRIST));
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Unit test.
   */
  @Test
  public void secondCreateAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator(
        getContract(ACCOUNT_NAME_SECOND, OWNER_ADDRESS_SECOND), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS_SECOND));
      Assert.assertNotNull(accountCapsule);
      Assert.assertEquals(accountCapsule.getInstance().getAccountName(),
          ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND));
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
  }
}