package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class TransferActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final long AMOUNT = 100;
  private static final long OWNER_BALANCE = 9999999;
  private static final long TO_BALANCE = 100001;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String TO_ADDRESS_INVALID = "bbb";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final String OWNER_NO_BALANCE;
  private static final String To_ACCOUNT_INVALID;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_NO_BALANCE = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3433";
    To_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3422";
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
  public void createCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            OWNER_BALANCE);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private Any getTransparentOutContract(long outAmount) {
    return Any.pack(
        Contract.ShieldedTransferContract.newBuilder()
            .setTransparentFromAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setFromAmount(outAmount)
            .build());
  }

  private Any getTransparentToContract(long inAmount) {
    return Any.pack(
        Contract.ShieldedTransferContract.newBuilder()
            .setTransparentToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setToAmount(inAmount)
            .build());
  }

  private Any getTransparentOutToContract(long outAmount, long inAmount) {
    return Any.pack(
        Contract.ShieldedTransferContract.newBuilder()
            .setTransparentFromAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setFromAmount(outAmount)
            .setTransparentToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setToAmount(inAmount)
            .build());
  }

  private Any getNoTransparentContract() {
     return null;
  }

  @Test
  public void rightTransfer() {

  }

  @Test
  public void perfectTransfer() {

  }

  @Test
  public void moreTransfer() {

  }

  @Test
  public void iniviateOwnerAddress() {

  }

  @Test
  public void iniviateToAddress() {

  }

  @Test
  public void iniviateTrx() {

  }

  @Test
  public void noExitOwnerAccount() {

  }

  @Test
  /**
   * If to account not exit, create it.
   */
  public void noExitToAccount() {

  }

  @Test
  public void zeroAmountTest() {

  }

  @Test
  public void negativeAmountTest() {

  }

  @Test
  public void addOverflowTest() {

  }

  @Test
  public void insufficientFee() {

  }

}
