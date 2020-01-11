package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class TransactionCapsuleTest {

  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application AppT;
  private static String dbPath = "output_transactioncapsule_test";
  private static String OWNER_ADDRESS;
  private static String OWNER_KEY =
      "bfa67cb3dc6609b3a0c98e717d66f38ed1a159b5b3421678dfab85961c40de2f";
  private static String TO_ADDRESS;
  private static String OWNER_ACCOUNT_NOT_Exist;
  private static String KEY_11 = "1111111111111111111111111111111111111111111111111111111111111111";
  private static String KEY_12 = "1212121212121212121212121212121212121212121212121212121212121212";
  private static String KEY_13 = "1313131313131313131313131313131313131313131313131313131313131313";
  private static String KEY_21 = "2121212121212121212121212121212121212121212121212121212121212121";
  private static String KEY_22 = "2222222222222222222222222222222222222222222222222222222222222222";
  private static String KEY_23 = "2323232323232323232323232323232323232323232323232323232323232323";
  private static String KEY_31 = "3131313131313131313131313131313131313131313131313131313131313131";
  private static String KEY_32 = "3232323232323232323232323232323232323232323232323232323232323232";
  private static String KEY_33 = "3333333333333333333333333333333333333333333333333333333333333333";

  private static String KEY_ADDRESS_11;
  private static String KEY_ADDRESS_12;
  private static String KEY_ADDRESS_13;
  private static String KEY_ADDRESS_21;
  private static String KEY_ADDRESS_22;
  private static String KEY_ADDRESS_23;
  private static String KEY_ADDRESS_31;
  private static String KEY_ADDRESS_32;
  private static String KEY_ADDRESS_33;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ACCOUNT_NOT_Exist =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    KEY_ADDRESS_11 = Wallet.getAddressPreFixString() + "19E7E376E7C213B7E7E7E46CC70A5DD086DAFF2A";
    KEY_ADDRESS_12 = Wallet.getAddressPreFixString() + "1C5A77D9FA7EF466951B2F01F724BCA3A5820B63";
    KEY_ADDRESS_13 = Wallet.getAddressPreFixString() + "03A1BBA60B5AA37094CF16123ADD674C01589488";

    KEY_ADDRESS_21 = Wallet.getAddressPreFixString() + "2BD0C9FE079C8FCA0E3352EB3D02839C371E5C41";
    KEY_ADDRESS_22 = Wallet.getAddressPreFixString() + "1563915E194D8CFBA1943570603F7606A3115508";
    KEY_ADDRESS_23 = Wallet.getAddressPreFixString() + "D3E442496EB66A4748912EC4A3B7A111D0B855D6";

    KEY_ADDRESS_31 = Wallet.getAddressPreFixString() + "77952CE83CA3CAD9F7ADCFABEDA85BD2F1F52008";
    KEY_ADDRESS_32 = Wallet.getAddressPreFixString() + "94622CC2A5B64A58C25A129D48A2BEEC4B65B779";
    KEY_ADDRESS_33 = Wallet.getAddressPreFixString() + "5CBDD86A2FA8DC4BDDD8A8F69DBA48572EEC07FB";
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        StringUtil.hexString2ByteString(OWNER_ADDRESS), AccountType.Normal, 10_000_000_000L);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }

}