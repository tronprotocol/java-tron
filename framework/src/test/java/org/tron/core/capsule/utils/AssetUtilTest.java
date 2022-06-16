package org.tron.core.capsule.utils;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.Manager;
import org.tron.core.store.AccountAssetStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

@Slf4j
public class AssetUtilTest {

  private static String dbPath = "output_AssetUtil_test";
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static ChainBaseManager chainBaseManager;

  static {
    Args.setParam(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }

  private static AccountCapsule createAccount2() {
    AccountAssetStore accountAssetStore = dbManager.getAccountAssetStore();
    com.google.protobuf.ByteString accountName =
        com.google.protobuf.ByteString.copyFrom(randomBytes(16));
    com.google.protobuf.ByteString address = ByteString.copyFrom(randomBytes(32));
    Protocol.AccountType accountType = Protocol.AccountType.forNumber(1);
    AccountCapsule accountCapsule = new AccountCapsule(accountName, address, accountType);
    return accountCapsule;
  }

  @Test
  public void tetGetFrozen() {
    AccountCapsule account = createAccount2();
    Protocol.Account build = account.getInstance().toBuilder()
        .addAllFrozenSupply(getFrozenList())
        .build();
    account.setInstance(build);
    Assert.assertNotNull(account.getFrozenSupplyList());
  }

  private static List<Protocol.Account.Frozen> getFrozenList() {
    List<Protocol.Account.Frozen> frozenList = Lists.newArrayList();
    for (int i = 0; i < 3; i++) {
      Protocol.Account.Frozen newFrozen = Protocol.Account.Frozen.newBuilder()
          .setFrozenBalance(i * 1000 + 1)
          .setExpireTime(1000)
          .build();
      frozenList.add(newFrozen);
    }
    return frozenList;
  }


  @Test
  public void testUpdateUsage() {
    List<AssetIssueCapsule> assetIssueCapsuleList = new ArrayList<>();

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("111")))
            .setId(Long.toString(1))
            .build();
    AssetIssueCapsule capsule1 = new AssetIssueCapsule(assetIssueContract);
    capsule1.setPublicFreeAssetNetUsage(60);
    capsule1.setPublicLatestFreeNetTime(14400L);

    assetIssueCapsuleList.add(capsule1);

    BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    assetIssueCapsuleList.forEach(
        issueCapsule -> {
          processor.updateUsage(issueCapsule, 28800);
          builder.addAssetIssue(issueCapsule.getInstance());
        }
    );

    Assert.assertEquals(30L,
        builder.build().getAssetIssue(0).getPublicFreeAssetNetUsage());
  }
}
