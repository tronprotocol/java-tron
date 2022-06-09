package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.security.SecureRandom;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.OraclePrevoteCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.StableMarketStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.OracleVote;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.OracleContract;

@Slf4j
public class OracleExchangeRateVoteActuatorTest {
  private static final String dbPath = "output_OracleExchangeRateVote_test";
  private static final String ACCOUNT_NAME_SR1 = "ownerSR1";
  private static final String ACCOUNT_ADDRESS_SR1;
  private static final String ACCOUNT_NAME_NOT_SR = "ownerNotSR";
  private static final String ACCOUNT_ADDRESS_NOT_SR;
  private static final String URL1 = "https://tron.network1";
  private static TronApplicationContext context;
  private static Manager dbManager;
  private static StableMarketStore stableMarketStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    ACCOUNT_ADDRESS_SR1 =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ACCOUNT_ADDRESS_NOT_SR =
        Wallet.getAddressPreFixString() + "B56446E617E924805E4D6CA021D341FEF6E2013B";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    stableMarketStore = dbManager.getChainBaseManager().getStableMarketStore();

    // init account
    WitnessCapsule witnessCapsule1 =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1)),
            10_000_000L,
            URL1);

    AccountCapsule sr1AccountSrCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SR1),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1)),
            AccountType.Normal,
            300_000_000L);
    AccountCapsule notSrAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_NOT_SR),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_NOT_SR)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(sr1AccountSrCapsule.getAddress().toByteArray(), sr1AccountSrCapsule);
    dbManager.getAccountStore()
        .put(notSrAccountCapsule.getAddress().toByteArray(), notSrAccountCapsule);

    dbManager.getWitnessStore().put(sr1AccountSrCapsule.getAddress().toByteArray(),
        witnessCapsule1);


    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().setOracleVotePeriod(10);

    // init tobin
    createStableAsset(ACCOUNT_ADDRESS_SR1, "USD", 100000);
    createStableAsset(ACCOUNT_ADDRESS_SR1, "HKD", 100000);
    dbManager.getStableMarketStore().updateTobinTax(null);
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
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getStableMarketStore().clearPrevoteAndVotes(100, 10);
    dbManager.getStableMarketStore().clearAllOracleExchangeRates();
  }

  public static long createStableAsset(String owner, String assetName, long totalSupply) {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(owner));

    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
        AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(Objects.requireNonNull(ByteArray.fromString(assetName))))
            .setId(Long.toString(id))
            .setTotalSupply(totalSupply)
            .setPrecision(6)
            .setTrxNum(10)
            .setNum(1)
            .setStartTime(1)
            .setEndTime(2)
            .setVoteScore(2)
            .setDescription(
                ByteString.copyFrom(Objects.requireNonNull(ByteArray.fromString("usdd-test"))))
            .setUrl(ByteString.copyFrom(Objects.requireNonNull(ByteArray.fromString("https://usdd.io"))))
            .build();
    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(assetIssueContract);
    ownerCapsule.setAssetIssuedName(assetIssueCapsuleV2.createDbKey());
    ownerCapsule.setAssetIssuedID(assetIssueCapsuleV2.createDbV2Key());
    ownerCapsule.addAssetV2(assetIssueCapsuleV2.createDbV2Key(), totalSupply);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAssetIssueV2Store()
        .put(assetIssueCapsuleV2.createDbKeyFinal(
            dbManager.getDynamicPropertiesStore()), assetIssueCapsuleV2);

    dbManager.getStableMarketStore()
        .setStableCoin(ByteArray.fromString(String.valueOf(id)), 5);
    return id;
  }

  private Any getContract(String owner, String sr, byte[] preVoteHash,
                          String salt, String exchangeRates) {
    OracleContract.OracleExchangeRateVoteContract.Builder builder =
        OracleContract.OracleExchangeRateVoteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
        .setSrAddress(ByteString.copyFrom(ByteArray.fromHexString(sr)));
    if (preVoteHash != null) {
      builder.setPreVoteHash(ByteString.copyFrom(preVoteHash));
    }
    if (!exchangeRates.isEmpty()) {
      builder.setVote(
          OracleVote.newBuilder().setSalt(salt).setExchangeRates(exchangeRates).build());
    }
    return Any.pack(builder.build());
  }

  /**
   * Witness delegate feed consent to an account
   */
  public void witnessDelegate(String witness, String feeder) {
    DelegateFeedConsentActuator actuator = new DelegateFeedConsentActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(Any.pack(
        OracleContract.DelegateFeedConsentContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(witness)))
            .setFeederAddress(ByteString.copyFrom(ByteArray.fromHexString(feeder)))
            .build()));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    Assert.assertEquals(Protocol.Transaction.Result.code.SUCESS, ret.getInstance().getRet());

    byte[] dbFeeder = stableMarketStore.getFeeder(ByteArray.fromHexString(witness));
    if (feeder.isEmpty() || feeder.equals(witness)) {
      Assert.assertNull(dbFeeder);
    } else {
      Assert.assertArrayEquals(ByteArray.fromHexString(feeder), dbFeeder);
    }
  }


  /**
   * Witness submit exchange rates
   */
  @Test
  public void witnessSubmitExchangeRates() {
    byte[] saltBytes = new byte[2];
    new SecureRandom().nextBytes(saltBytes);
    String salt = Hex.toHexString(saltBytes);
    String exchangeRateStr = "1000002:0.08270,1000001:0.6488";

    byte[] voteData = (salt + exchangeRateStr + ACCOUNT_ADDRESS_SR1).getBytes();
    byte[] preVoteHash =
        Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), voteData);
    OracleExchangeRateVoteActuator actuator = new OracleExchangeRateVoteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, preVoteHash, "", ""));

    // 1 submit pre vote hash
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    Assert.assertEquals(Transaction.Result.code.SUCESS, ret.getInstance().getRet());
    OraclePrevoteCapsule preVote =
        stableMarketStore.getPrevote(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1));
    Assert.assertEquals(10, preVote.getInstance().getBlockNum());
    Assert.assertArrayEquals(preVoteHash, preVote.getInstance().getHash().toByteArray());

    // 2 submit vote
    OracleExchangeRateVoteActuator actuator2 = new OracleExchangeRateVoteActuator();
    actuator2.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, null, salt, exchangeRateStr));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(20);

    TransactionResultCapsule ret2 = new TransactionResultCapsule();
    try {
      actuator2.validate();
      actuator2.execute(ret2);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    Assert.assertEquals(Transaction.Result.code.SUCESS, ret.getInstance().getRet());
    OracleVote vote = stableMarketStore.getVote(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1));
    Assert.assertEquals(salt, vote.getSalt());
    Assert.assertEquals(exchangeRateStr, vote.getExchangeRates());
  }

  /**
   * Various cases of failure to submit exchange rate votes
   */
  @Test
  public void submitExchangeRatesFail() {
    byte[] saltBytes = new byte[2];
    new SecureRandom().nextBytes(saltBytes);
    String salt = Hex.toHexString(saltBytes);
    String exchangeRateStr = "1000002:0.08270,1000001:0.6488";

    byte[] voteData = (salt + exchangeRateStr + ACCOUNT_ADDRESS_NOT_SR).getBytes();
    byte[] preVoteHash =
        Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), voteData);
    OracleExchangeRateVoteActuator actuator = new OracleExchangeRateVoteActuator();

    // 1 Non-Witness submit exchange rates
    boolean catchException = false;
    TransactionResultCapsule ret = new TransactionResultCapsule();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_NOT_SR, ACCOUNT_ADDRESS_NOT_SR, preVoteHash, "", ""));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertEquals("Not existed witness", e.getMessage());
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }

    // 2 Non-feeder submit exchange rates
    catchException = false;
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_NOT_SR, ACCOUNT_ADDRESS_SR1, preVoteHash, "", ""));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertEquals("Invalid feeder address", e.getMessage());
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }

    // 3 invalid exchange rate string format
    catchException = false;
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getContract(ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1,
            preVoteHash, "", "1000002:0.08270.1000001:0.6488"));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertTrue(e.getMessage().contains("parse exchange rate string error:"));
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }

    // 4 unknown asset
    catchException = false;
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, preVoteHash, "", "1000000:0.08270"));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertEquals("unknown vote asset", e.getMessage());
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }

    // 5 vote with no pre vote hash
    catchException = false;
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, preVoteHash, "", "1000002:0.08270"));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertEquals("cannot find pre vote", e.getMessage());
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }

    // 6 vote
    catchException = false;
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, preVoteHash, "", ""));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, null, salt, exchangeRateStr));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertEquals("pre vote period is not current - 1", e.getMessage());
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(20);
    catchException = false;
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1, null, "salt", exchangeRateStr));
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.assertEquals("pre vote hash verification failed", e.getMessage());
      catchException = true;
    }
    if (!catchException) {
      Assert.fail();
    }
  }

  /**
   * Witness pre vote
   */
  @Test
  public void feederSubmitExchangeRates() {
    witnessDelegate(ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_NOT_SR);

    byte[] saltBytes = new byte[2];
    new SecureRandom().nextBytes(saltBytes);
    String salt = Hex.toHexString(saltBytes);
    String exchangeRateStr = "1000002:0.08270,1000001:0.6488";

    byte[] voteData = (salt + exchangeRateStr + ACCOUNT_ADDRESS_SR1).getBytes();
    byte[] preVoteHash =
        Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), voteData);
    OracleExchangeRateVoteActuator actuator = new OracleExchangeRateVoteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_NOT_SR, ACCOUNT_ADDRESS_SR1, preVoteHash, "", ""));

    // 1 submit pre vote hash
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    Assert.assertEquals(Transaction.Result.code.SUCESS, ret.getInstance().getRet());
    OraclePrevoteCapsule preVote =
        stableMarketStore.getPrevote(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1));
    Assert.assertEquals(10, preVote.getInstance().getBlockNum());
    Assert.assertArrayEquals(preVoteHash, preVote.getInstance().getHash().toByteArray());

    // 2 submit vote
    OracleExchangeRateVoteActuator actuator2 = new OracleExchangeRateVoteActuator();
    actuator2.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ACCOUNT_ADDRESS_NOT_SR, ACCOUNT_ADDRESS_SR1, null, salt, exchangeRateStr));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(20);

    TransactionResultCapsule ret2 = new TransactionResultCapsule();
    try {
      actuator2.validate();
      actuator2.execute(ret2);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail();
    }
    Assert.assertEquals(ret.getInstance().getRet(), Transaction.Result.code.SUCESS);
    OracleVote vote = stableMarketStore.getVote(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1));
    Assert.assertEquals(salt, vote.getSalt());
    Assert.assertEquals(exchangeRateStr, vote.getExchangeRates());

    witnessDelegate(ACCOUNT_ADDRESS_SR1, ACCOUNT_ADDRESS_SR1);
  }
}
