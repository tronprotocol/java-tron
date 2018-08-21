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
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.db.Manager;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class EnergyProcessorTest {

  private static Manager dbManager;
  private static final String dbPath = "EnergyProcessorTest";
  private static AnnotationConfigApplicationContext context;
  private static final String ASSET_NAME;
  private static final String CONTRACT_PROVIDER_ADDRESS;
  private static final String USER_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    CONTRACT_PROVIDER_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    USER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
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
    AccountCapsule contractProvierCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_PROVIDER_ADDRESS)),
            AccountType.Normal,
            0L);
    contractProvierCapsule.addAsset(ASSET_NAME.getBytes(), 100L);

    AccountCapsule userCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(USER_ADDRESS)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    dbManager.getAccountStore().reset();
    dbManager.getAccountStore()
        .put(contractProvierCapsule.getAddress().toByteArray(), contractProvierCapsule);
    dbManager.getAccountStore().put(userCapsule.getAddress().toByteArray(), userCapsule);

  }


  //todo ,replaced by smartContract later
  private AssetIssueContract getAssetIssueContract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(USER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  @Test
  public void testUseContractCreatorEnergy() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(10_000_000L);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(CONTRACT_PROVIDER_ADDRESS));
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    EnergyProcessor processor = new EnergyProcessor(dbManager);
    long energy = 10000;
    long now = 1526647838000L;

    boolean result = processor.useEnergy(ownerCapsule, energy, now);
    Assert.assertEquals(false, result);

    ownerCapsule.setFrozenForEnergy(10_000_000L, 0L);
    result = processor.useEnergy(ownerCapsule, energy, now);
    Assert.assertEquals(true, result);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(CONTRACT_PROVIDER_ADDRESS));

    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(1526647838000L,
        ownerCapsuleNew.getAccountResource().getLatestConsumeTimeForEnergy());
    Assert.assertEquals(10000L, ownerCapsuleNew.getAccountResource().getEnergyUsage());

  }


}
