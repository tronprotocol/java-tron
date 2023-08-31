package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.AssetIssueStore;
import org.tron.protos.contract.AssetIssueContractOuterClass;

public class AssetIssueStoreTest extends BaseTest {

  private static final String NAME = "test-asset";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final int TRX_NUM = 10000;
  private static final int NUM = 100000;
  private static final String DESCRIPTION = "myCoin";
  private static final String URL = "tron.network";

  @Resource
  private AssetIssueStore assetIssueStore;

  static {
    dbPath = "db_AssetIssueStoreTest_test";
    Args.setParam(
            new String[]{
                "--output-directory", dbPath,
            },
            Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    AssetIssueCapsule assetIssueCapsule = createAssetIssue(id, NAME);
    assetIssueStore.put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
  }

  private AssetIssueCapsule createAssetIssue(long id, String name) {
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
            AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom(ByteArray.fromString(name))).setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM).setNum(NUM).setStartTime(1).setEndTime(100).setVoteScore(2)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL))).build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    return assetIssueCapsule;
  }

  @Test
  public void testPut() {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    String issueName = "test-asset2";
    AssetIssueCapsule assetIssueCapsule = createAssetIssue(id, issueName);
    assetIssueStore.put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    AssetIssueCapsule assetIssueCapsule1 = assetIssueStore.get(ByteArray.fromString(issueName));

    Assert.assertNotNull(assetIssueCapsule1);
    Assert.assertEquals(issueName, new String(assetIssueCapsule1.getName().toByteArray()));
  }

  @Test
  public void testGet() {
    AssetIssueCapsule assetIssueCapsule = assetIssueStore.get(ByteArray.fromString(NAME));
    Assert.assertNotNull(assetIssueCapsule);
    Assert.assertEquals(NAME, new String(assetIssueCapsule.getName().toByteArray()));
    Assert.assertEquals(TOTAL_SUPPLY, assetIssueCapsule.getInstance().getTotalSupply());
  }

  @Test
  public void testDelete() {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    String issueName = "test-asset-delete";
    AssetIssueCapsule assetIssueCapsule = createAssetIssue(id, issueName);
    assetIssueStore.put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    AssetIssueCapsule assetIssueCapsule1 = assetIssueStore.get(ByteArray.fromString(issueName));
    Assert.assertNotNull(assetIssueCapsule1);
    assetIssueStore.delete(assetIssueCapsule1.createDbKey());
    AssetIssueCapsule assetIssueCapsule2 = assetIssueStore.get(ByteArray.fromString(issueName));
    Assert.assertNull(assetIssueCapsule2);

  }
}
