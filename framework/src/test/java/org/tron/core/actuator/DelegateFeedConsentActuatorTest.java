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
import org.tron.core.store.StableMarketStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.OracleContract;

@Slf4j
public class DelegateFeedConsentActuatorTest {
  private static final String dbPath = "output_DelegateFeedConsent_test";
  private static final String ACCOUNT_NAME_SR = "ownerSR";
  private static final String ACCOUNT_ADDRESS_SR;
  private static final String ACCOUNT_NAME_FEEDER = "ownerFeeder";
  private static final String ACCOUNT_ADDRESS_FEEDER;
  private static final String URL = "https://tron.network";
  private static TronApplicationContext context;
  private static Manager dbManager;
  private static StableMarketStore stableMarketStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    ACCOUNT_ADDRESS_SR =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ACCOUNT_ADDRESS_FEEDER =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    stableMarketStore = dbManager.getChainBaseManager().getStableMarketStore();
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
  public void initTest() {
    WitnessCapsule witnessCapsule =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR)),
            10_000_000L,
            URL);
    AccountCapsule ownerAccountSrCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SR),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR)),
            Protocol.AccountType.Normal,
            300_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FEEDER),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_FEEDER)),
            Protocol.AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountSrCapsule.getAddress().toByteArray(), ownerAccountSrCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    dbManager.getWitnessStore().put(ownerAccountSrCapsule.getAddress().toByteArray(),
        witnessCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().setOracleVotePeriod(0);

  }

  private Any getContract(String sr, String feeder) {
    return Any.pack(
        OracleContract.DelegateFeedConsentContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(sr)))
            .setFeederAddress(ByteString.copyFrom(ByteArray.fromHexString(feeder)))
            .build());
  }

  /**
   * Witness delegate feed consent to an account
   */
  @Test
  public void witnessDelegate() {
    DelegateFeedConsentActuator actuator = new DelegateFeedConsentActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR, ACCOUNT_ADDRESS_FEEDER));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
    byte[] dbFeeder = stableMarketStore.getFeeder(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR));
    Assert.assertArrayEquals(ByteArray.fromHexString(ACCOUNT_ADDRESS_FEEDER), dbFeeder);
  }

  /**
   * Non-Witness delegate feed consent to an account,
   */
  @Test
  public void nonWitnessDelegate() {
    DelegateFeedConsentActuator actuator = new DelegateFeedConsentActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_FEEDER, ACCOUNT_ADDRESS_SR));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertEquals("Not existed witness:" + ACCOUNT_ADDRESS_FEEDER,
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  /**
   * Witnesses cancel the delegated feeder by not setting the feeder address
   */
  @Test
  public void WitnessDeleteDelegate1() {
    DelegateFeedConsentActuator actuator = new DelegateFeedConsentActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR, ACCOUNT_ADDRESS_FEEDER));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException ignored) {
      Assert.fail();
    }
    byte[] dbFeeder = stableMarketStore.getFeeder(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR));
    Assert.assertArrayEquals(ByteArray.fromHexString(ACCOUNT_ADDRESS_FEEDER), dbFeeder);
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(Any.pack(
        OracleContract.DelegateFeedConsentContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR)))
            .build()));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException ignored) {
      Assert.fail();
    }
    dbFeeder = stableMarketStore.getFeeder(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR));
    Assert.assertNull(dbFeeder);
  }

  /**
   * Witnesses cancel the delegated feeder by
   * setting the feeder address to the witness address
   */
  @Test
  public void WitnessDeleteDelegate2() {
    DelegateFeedConsentActuator actuator = new DelegateFeedConsentActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR, ACCOUNT_ADDRESS_FEEDER));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException ignored) {
      Assert.fail();
    }
    byte[] dbFeeder = stableMarketStore.getFeeder(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR));
    Assert.assertArrayEquals(ByteArray.fromHexString(ACCOUNT_ADDRESS_FEEDER), dbFeeder);
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR, ACCOUNT_ADDRESS_SR));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException ignored) {
      Assert.fail();
    }
    dbFeeder = stableMarketStore.getFeeder(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR));
    Assert.assertNull(dbFeeder);
  }
}
