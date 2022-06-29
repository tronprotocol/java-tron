package org.tron.core.consensus;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.consensus.dpos.OracleManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.store.StableMarketStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;

@Slf4j
public class OracleManagerTest {
  private static final String dbPath = "output_OracleManager_test";
  private static final String ACCOUNT_NAME_SR1 = "ownerSR1";
  private static final String ACCOUNT_ADDRESS_SR1;
  private static final String ACCOUNT_NAME_SR2 = "ownerSR2";
  private static final String ACCOUNT_ADDRESS_SR2;
  private static final String ACCOUNT_NAME_SR3 = "ownerSR3";
  private static final String ACCOUNT_ADDRESS_SR3;
  private static final String ACCOUNT_NAME_NOT_SR = "ownerNotSR";
  private static final String ACCOUNT_ADDRESS_NOT_SR;
  private static final String URL1 = "https://tron.network1";
  private static final String URL2 = "https://tron.network2";
  private static final String URL3 = "https://tron.network3";
  private static TronApplicationContext context;
  private static Manager dbManager;
  private static StableMarketStore stableMarketStore;

  private static OracleManager oracleManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    ACCOUNT_ADDRESS_SR1 =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ACCOUNT_ADDRESS_SR2 =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    ACCOUNT_ADDRESS_SR3 =
        Wallet.getAddressPreFixString() + "4948c2e8a756d9437037dcd8c7e0c73d560ca38d";
    ACCOUNT_ADDRESS_NOT_SR =
        Wallet.getAddressPreFixString() + "B56446E617E924805E4D6CA021D341FEF6E2013B";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    oracleManager = context.getBean(OracleManager.class);
    stableMarketStore = dbManager.getChainBaseManager().getStableMarketStore();

    // init account
    WitnessCapsule witnessCapsule1 =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1)),
            10_000_000L,
            URL1);
    WitnessCapsule witnessCapsule2 =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR2)),
            10_000_000L,
            URL2);
    WitnessCapsule witnessCapsule3 =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR3)),
            10_000_000L,
            URL3);

    AccountCapsule sr1AccountSrCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SR1),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1)),
            Protocol.AccountType.Normal,
            300_000_000L);
    AccountCapsule sr2AccountSrCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SR2),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR2)),
            Protocol.AccountType.Normal,
            200_000_000_000L);
    AccountCapsule sr3AccountSrCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SR3),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR3)),
            Protocol.AccountType.Normal,
            200_000_000_000L);
    AccountCapsule notSrAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_NOT_SR),
            ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_NOT_SR)),
            Protocol.AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(sr1AccountSrCapsule.getAddress().toByteArray(), sr1AccountSrCapsule);
    dbManager.getAccountStore()
        .put(sr2AccountSrCapsule.getAddress().toByteArray(), sr2AccountSrCapsule);
    dbManager.getAccountStore()
        .put(sr3AccountSrCapsule.getAddress().toByteArray(), sr3AccountSrCapsule);
    dbManager.getAccountStore()
        .put(notSrAccountCapsule.getAddress().toByteArray(), notSrAccountCapsule);

    dbManager.getWitnessStore().put(sr1AccountSrCapsule.getAddress().toByteArray(),
        witnessCapsule1);
    dbManager.getWitnessStore().put(sr2AccountSrCapsule.getAddress().toByteArray(),
        witnessCapsule2);
    dbManager.getWitnessStore().put(sr3AccountSrCapsule.getAddress().toByteArray(),
        witnessCapsule3);

    List<ByteString> list = new ArrayList<>();
    list.add(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR1)));
    list.add(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR2)));
    list.add(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ADDRESS_SR3)));
    dbManager.getWitnessScheduleStore().saveActiveWitnesses(list);


    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().setOracleVotePeriod(10);

    // init tobin
    createStableAsset(ACCOUNT_ADDRESS_SR1, "USD", 100000);
    createStableAsset(ACCOUNT_ADDRESS_SR2, "HKD", 100000);
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

  /**
   * oracle exchange update at the end of vote period
   */
  @Test
  public void oracleExchangeUpdate() {
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR1),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.07,1000002:0.7").build());
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR2),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.08,1000002:0.8").build());
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR3),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.06,1000002:0.6").build());
    BlockCapsule block = new BlockCapsule(1000000,ByteString.EMPTY,19,new ArrayList<>());
    oracleManager.applyBlock(block);
    Assert.assertEquals(Dec.newDec("0.07"),
        stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000001")));
    Assert.assertEquals(Dec.newDec("0.7"),
        stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000002")));

    block = new BlockCapsule(1000000,ByteString.EMPTY,29,new ArrayList<>());
    oracleManager.applyBlock(block);
    Assert.assertNull(stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000001")));
    Assert.assertNull(stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000002")));
  }

  /**
   * oracle insufficient witness vote
   */
  @Test
  public void oracleWithInsufficientVote() {
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR1),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.07,1000002:0.7").build());
    BlockCapsule block = new BlockCapsule(1000000,ByteString.EMPTY,19,new ArrayList<>());
    oracleManager.applyBlock(block);
    Assert.assertNull(stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000001")));
    Assert.assertNull(stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000002")));
  }

  /**
   * witness abstain from submitting voting by setting the ExchangeRate to 0
   */
  @Test
  public void oracleWithInsufficientVote2() {
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR1),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.0,1000002:0").build());
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR2),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.0,1000002:0").build());
    stableMarketStore.setVote(Hex.decode(ACCOUNT_ADDRESS_SR3),
        Protocol.OracleVote.newBuilder().setExchangeRates("1000001:0.0,1000002:0").build());
    BlockCapsule block = new BlockCapsule(1000000,ByteString.EMPTY,19,new ArrayList<>());
    oracleManager.applyBlock(block);
    Assert.assertNull(stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000001")));
    Assert.assertNull(stableMarketStore.getOracleExchangeRate(ByteArray.fromString("1000002")));
  }
}
