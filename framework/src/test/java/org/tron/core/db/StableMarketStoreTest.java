package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.store.StableMarketStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;


@Slf4j
public class StableMarketStoreTest {

  private static final String dbPath = "output-stablemarket-test";
  private static TronApplicationContext context;

  private static final String OWNER_ADDRESS;
  private static final byte[] USDD_SYMBOL = "usdd".getBytes();

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
  }

  Manager dbManager;
  StableMarketStore stableMarketStore;

  @Before
  public void init() {
    dbManager = context.getBean(Manager.class);
    stableMarketStore = context.getBean(StableMarketStore.class);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    AccountCapsule fromAccountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("fromAccount"),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore()
        .put(fromAccountCapsule.getAddress().toByteArray(), fromAccountCapsule);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  public long createStableAsset(String owner, String assetName, long totalSupply) {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(owner));

    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
        AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
            .setTotalSupply(totalSupply)
            .setPrecision(6)
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


  @Test
  public void testGetStableCoinList() {
    long tokenId = createStableAsset(OWNER_ADDRESS, "usdd", 100000);
    System.out.println(stableMarketStore.getStableCoinInfoById(Long.toString(tokenId).getBytes()));

    stableMarketStore.getStableCoinList();
    System.out.println(stableMarketStore.getStableCoinList());
  }
}
