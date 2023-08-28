package org.tron.core.db;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;

import java.util.Map;
import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.AccountStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;

public class AccountAssetStoreTest extends BaseTest {

  private static final byte[] ASSET_KEY = "20000".getBytes();
  private static AccountCapsule ownerCapsule;

  private static String OWNER_ADDRESS = Wallet.getAddressPreFixString()
          + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final long TOTAL_SUPPLY = 1000_000_000L;
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";

  @Resource
  private AccountAssetStore accountAssetStore;

  @Resource
  private AccountStore accountStore;

  static {
    dbPath = "db_AccountAssetStore_test";
    Args.setParam(
            new String[]{
                "--output-directory", dbPath,
            },
            Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    accountAssetStore.put(ASSET_KEY, Longs.toByteArray(200L));

    ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                    ByteString.copyFromUtf8("owner"),
                    Protocol.AccountType.AssetIssue);
  }


  private long createAsset(String tokenName) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
            AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                    .setName(ByteString.copyFrom(ByteArray.fromString(tokenName)))
                    .setId(Long.toString(id))
                    .setTotalSupply(TOTAL_SUPPLY)
                    .setTrxNum(TRX_NUM)
                    .setNum(NUM)
                    .setStartTime(START_TIME)
                    .setEndTime(END_TIME)
                    .setVoteScore(VOTE_SCORE)
                    .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
                    .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
                    .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    chainBaseManager.getAssetIssueV2Store()
            .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
    try {
      ownerCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } catch (Exception e) {
      e.printStackTrace();
    }
    accountStore.put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    return id;
  }

  @Test
  public void put() {
    byte[] key = "10000".getBytes();
    accountAssetStore.put(key, Longs.toByteArray(100L));
    byte[] bytes = accountAssetStore.get(key);
    Assert.assertEquals(100L, Longs.fromByteArray(bytes));
  }

  @Test
  public void get() {
    byte[] bytes = accountAssetStore.get(ASSET_KEY);
    Assert.assertEquals(200L, Longs.fromByteArray(bytes));
  }

  @Test
  public void getAccountAssets() {
    long assetKey = createAsset("testToken1");
    AccountCapsule accountCapsule = accountStore.get(ownerCapsule.getAddress().toByteArray());
    long assetValue = accountCapsule.getAssetV2(String.valueOf(assetKey));
    Assert.assertEquals(assetValue, TOTAL_SUPPLY);
  }

  @Test
  public void getAllAssets() {
    long assetKey1 = createAsset("testToken1");
    long assetKey2 = createAsset("testToken2");
    AccountCapsule accountCapsule = accountStore.get(ownerCapsule.getAddress().toByteArray());

    Map<String, Long> allAssets = accountAssetStore.getAllAssets(accountCapsule.getInstance());
    Long assetValue1 = allAssets.get(String.valueOf(assetKey1));
    Assert.assertNotNull(assetValue1);

    Long assetV1 = accountCapsule.getAssetV2(String.valueOf(assetKey1));
    Assert.assertEquals(assetValue1, assetV1);

    Long assetValue2 = allAssets.get(String.valueOf(assetKey2));
    Assert.assertNotNull(assetValue2);

    Long assetV2 = accountCapsule.getAssetV2(String.valueOf(assetKey2));
    Assert.assertEquals(assetValue1, assetV2);
  }

}
