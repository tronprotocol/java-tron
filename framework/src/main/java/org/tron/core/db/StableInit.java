package org.tron.core.db;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import com.google.protobuf.ByteString;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.protos.contract.AssetIssueContractOuterClass;


/**
 * This class is just for test to initialize the stable market, need to be delete on pro env.
 */
// todo: removed
public class StableInit {
  private static final String OWNER_ADDRESS = "415A523B449890854C8FC460AB602DF9F31FE4293F";
  private static final String TO_ADDRESS = "414948C2E8A756D9437037DCD8C7E0C73D560CA38D";
  private static final long TOBIN_FEE = 5;  // 0.5%
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "stable-test";
  private static final String URL = "https://tron.network";
  private static final String SOURCE_TOKEN = "source_token";
  private static final String DEST_TOKEN = "dest_token";

  private ChainBaseManager chainBaseManager;

  public StableInit(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void init() {
    chainBaseManager.getStableMarketStore().setOracleExchangeRate(TRX_SYMBOL_BYTES, Dec.oneDec());
    // prepare param
    long sourceTotalSupply = 100_000_000;
    long destTotalSupply = 500_000_000;
    Dec sourceExchangeRate = Dec.newDec("1.3");
    Dec destExchangeRate = Dec.newDec("1.5");
    // set trx balance
    initToken(OWNER_ADDRESS, SOURCE_TOKEN, sourceTotalSupply, sourceExchangeRate);
    initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate);
  }

  public long initToken(String owner, String tokenName, long totalSupply, Dec exchangeRate) {
    long token = createStableAsset(owner, tokenName, totalSupply);
    if (token == 0) {
      return 0;
    }
    chainBaseManager.getStableMarketStore().setOracleExchangeRate(
        ByteArray.fromString(String.valueOf(token)), exchangeRate);
    return token;
  }

  public long createStableAsset(String owner, String assetName, long totalSupply) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AccountCapsule ownerCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(owner));
    if (ownerCapsule == null) {
      return 0;
    }

    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
        AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
            .setTotalSupply(totalSupply)
            .setPrecision(6)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(assetIssueContract);
    ownerCapsule.setAssetIssuedName(assetIssueCapsuleV2.createDbKey());
    ownerCapsule.setAssetIssuedID(assetIssueCapsuleV2.createDbV2Key());
    ownerCapsule.addAssetV2(assetIssueCapsuleV2.createDbV2Key(), totalSupply);

    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    chainBaseManager.getAssetIssueV2Store()
        .put(assetIssueCapsuleV2.createDbKeyFinal(
            chainBaseManager.getDynamicPropertiesStore()), assetIssueCapsuleV2);

    chainBaseManager.getStableMarketStore()
        .setStableCoin(ByteArray.fromString(String.valueOf(id)), TOBIN_FEE);
    return id;
  }

}
