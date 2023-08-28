package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.protos.contract.AssetIssueContractOuterClass;


public class AssetIssueV2StoreTest extends BaseTest {

  static {
    dbPath = "db_AssetIssueV2StoreTest_test";
    Args.setParam(
          new String[]{
              "--output-directory", dbPath,
          },
          Constant.TEST_CONF
    );
  }

  private AssetIssueCapsule assetIssueCapsule;

  @Resource
  private AssetIssueV2Store assetIssueV2Store;

  @Before
  public void init() {
    String firstTokenId = "abc";
    assetIssueCapsule =
            new AssetIssueCapsule(
                    AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                            .build());
    assetIssueCapsule.setId(String.valueOf(1L));
    assetIssueV2Store
            .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
  }

  @Test
  public void put() {
    String firstTokenId = "efg";
    assetIssueCapsule =
            new AssetIssueCapsule(
                    AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                            .build());
    assetIssueCapsule.setId(String.valueOf(2L));
    assetIssueV2Store
            .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
    AssetIssueCapsule assetIssueCapsule =
            assetIssueV2Store.get(this.assetIssueCapsule.createDbV2Key());
    Assert.assertNotNull(assetIssueCapsule);
    String assetName = new String(assetIssueCapsule.getName().toByteArray());
    Assert.assertEquals(firstTokenId, assetName);
  }

  @Test
  public void get() {
    AssetIssueCapsule assetIssueCapsule1 = assetIssueV2Store.get(assetIssueCapsule.createDbV2Key());
    Assert.assertNotNull(assetIssueCapsule1);
    String assetName = new String(assetIssueCapsule1.getName().toByteArray());
    Assert.assertEquals("abc", assetName);
  }

  @Test
  public void delete() {
    String firstTokenId = "hij";
    assetIssueCapsule =
            new AssetIssueCapsule(
                    AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                            .build());
    assetIssueCapsule.setId(String.valueOf(2L));
    assetIssueV2Store
            .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
    AssetIssueCapsule assetIssueCapsule =
            assetIssueV2Store.get(this.assetIssueCapsule.createDbV2Key());
    Assert.assertNotNull(assetIssueCapsule);

    assetIssueV2Store.delete(assetIssueCapsule.createDbV2Key());
    AssetIssueCapsule assetIssueCapsule1 =
            assetIssueV2Store.get(this.assetIssueCapsule.createDbV2Key());
    Assert.assertNull(assetIssueCapsule1);
  }
}
