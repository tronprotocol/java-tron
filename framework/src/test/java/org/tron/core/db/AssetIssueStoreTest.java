package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.AssetIssueStore;
import org.tron.protos.contract.AssetIssueContractOuterClass;

public class AssetIssueStoreTest extends BaseTest {

  private static String dbDirectory = "db_AssetIssueStore_test";

  @Resource
  private AssetIssueStore assetIssueStore;

  private static String tokenId1 = "tokenId1";
  private static String tokenId2 = "tokenId2";
  private static AssetIssueContractOuterClass.AssetIssueContract assetIssueContract1 =
      AssetIssueContractOuterClass.AssetIssueContract.newBuilder().setId(String.valueOf(1))
          .setName(ByteString.copyFrom(tokenId1.getBytes())).setFreeAssetNetLimit(100)
          .setTrxNum(888).build();
  private static AssetIssueContractOuterClass.AssetIssueContract assetIssueContract2 =
      AssetIssueContractOuterClass.AssetIssueContract.newBuilder().setId(String.valueOf(2))
          .setName(ByteString.copyFrom(tokenId2.getBytes())).setFreeAssetNetLimit(1000)
          .setTrxNum(8880).build();

  static {
    dbPath = "output_AssetIssueStore_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath
        },
        Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    AssetIssueCapsule assetIssueCapsule1 = new AssetIssueCapsule(assetIssueContract1);
    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(assetIssueContract2);
    assetIssueStore.put(assetIssueCapsule1.getName().toByteArray(), assetIssueCapsule1);
    assetIssueStore.put(assetIssueCapsule2.getName().toByteArray(), assetIssueCapsule2);
  }

  @Test
  public void getTest() {
    AssetIssueCapsule assetIssueCapsule = assetIssueStore.get(tokenId1.getBytes());
    Assert.assertEquals(assetIssueCapsule.getId(), String.valueOf(1));
    Assert.assertEquals(assetIssueCapsule.getTrxNum(), 888);
    Assert.assertEquals(assetIssueCapsule.getFreeAssetNetLimit(), 100);
    Assert.assertEquals(assetIssueCapsule.getName(), ByteString.copyFrom(tokenId1.getBytes()));
    AssetIssueCapsule assetIssueCapsule2 = assetIssueStore.get(tokenId2.getBytes());
    Assert.assertEquals(assetIssueCapsule2.getId(), String.valueOf(2));
    Assert.assertEquals(assetIssueCapsule2.getTrxNum(), 8880);
    Assert.assertEquals(assetIssueCapsule2.getFreeAssetNetLimit(), 1000);
    Assert.assertEquals(assetIssueCapsule2.getName(), ByteString.copyFrom(tokenId2.getBytes()));
  }

  @Test
  public void getAllAssetIssuesTest() {
    List<AssetIssueCapsule> assetIssueCapsules = assetIssueStore.getAllAssetIssues();
    Assert.assertEquals(assetIssueCapsules.size(), 2);
  }

  @Test
  public void getAssetIssuesPaginatedTest() {
    List<AssetIssueCapsule> assetIssueCapsules = assetIssueStore.getAssetIssuesPaginated(
        1,1);
    Assert.assertEquals(assetIssueCapsules.size(), 1);
    AssetIssueCapsule assetIssueCapsule2 = assetIssueCapsules.get(0);
    Assert.assertEquals(assetIssueCapsule2.getId(), String.valueOf(2));
    Assert.assertEquals(assetIssueCapsule2.getTrxNum(), 8880);
    Assert.assertEquals(assetIssueCapsule2.getFreeAssetNetLimit(), 1000);
    Assert.assertEquals(assetIssueCapsule2.getName(), ByteString.copyFrom(tokenId2.getBytes()));
  }
}