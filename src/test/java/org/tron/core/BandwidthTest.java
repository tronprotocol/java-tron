package org.tron.core;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class BandwidthTest {

  private static Manager dbManager;
  private static final String dbPath = "bandwidth_test";
  private static AnnotationConfigApplicationContext context;
  private static final String ASSET_NAME;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_ADDRESS;
  private static final String TO_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ASSET_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
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
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
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
            0L);
    ownerCapsule.addAsset(ASSET_NAME, 100L);

    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            0L);

    AccountCapsule assetCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)),
            AccountType.AssetIssue,
            ChainConstant.ASSET_ISSUE_FEE);

    dbManager.getAccountStore().reset();
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    dbManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);

    dbManager
        .getAssetIssueStore()
        .put(
            ByteArray.fromString(ASSET_NAME),
            new AssetIssueCapsule(getAssetIssueContract()));

  }

  private TransferAssetContract getTransferAssetContract() {
    return Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private AssetIssueContract getAssetIssueContract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .build();
  }


  /**
   * Last trading time exceeded 10s
   */
  @Test
  public void testFree() throws Exception {

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setLatestOperationTime(1526647820000L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.consumeBandwidth(trx);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
  }


  @Test
  public void testConsumeIssue() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setLatestOperationTime(1526647830000L);
    ownerCapsule.setLatestAssetOperationTimeMap(ASSET_NAME, 1526647820000L);

    AccountCapsule assetCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));
    assetCapsule.setBandwidth(200000L);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);

    dbManager.consumeBandwidth(trx);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    AccountCapsule assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(1526647838000L,
        (long) ownerCapsuleNew.getLatestAssetOperationTimeMap().get(ASSET_NAME));
    Assert.assertEquals(100000L, assetCapsuleNew.getBandwidth());

  }

  @Test
  public void testConsumeOwner() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setLatestOperationTime(1526647830000L);
    ownerCapsule.setLatestAssetOperationTimeMap(ASSET_NAME, 1526647838000L);
    ownerCapsule.setBandwidth(200000L);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.consumeBandwidth(trx);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    AccountCapsule assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(1526647838000L,
        (long) ownerCapsuleNew.getLatestAssetOperationTimeMap().get(ASSET_NAME));
    Assert.assertEquals(100000L, ownerCapsuleNew.getBandwidth());

  }

}
