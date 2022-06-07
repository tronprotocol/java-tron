package org.tron.core.utils;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.core.ChainBaseManager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StableMarketStore;
import org.tron.protos.contract.StableMarketContractOuterClass.ExchangeResult;

@Component
public class StableMarketUtil {

  @Autowired
  private StableMarketStore stableMarketStore;
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;
  @Autowired
  private AssetIssueStore assetIssueStore;
  @Autowired
  private AssetIssueV2Store assetIssueV2Store;

  public void init(ChainBaseManager chainBaseManager) {
    this.stableMarketStore = chainBaseManager.getStableMarketStore();
    this.dynamicPropertiesStore = chainBaseManager.getDynamicPropertiesStore();
    this.assetIssueStore = chainBaseManager.getAssetIssueStore();
    this.assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
  }

  public ExchangeResult computeSwap(byte[] sourceToken, byte[] destToken, long offerAmount)
      throws ContractExeException {
    if (dynamicPropertiesStore.getAllowStableMarketOn() == 0) {
      throw new ContractExeException("Stable Market not open");
    }
    Dec offerRate = stableMarketStore.getOracleExchangeRate(sourceToken);
    Dec baseOfferAmount = Dec.newDec(offerAmount).mul(offerRate);
    if (offerRate == null || offerRate.eq(Dec.zeroDec())) {
      throw new ContractExeException("get exchange rate failed, tokenid:" + ByteArray.toStr(sourceToken));
    }
    Dec askRate = stableMarketStore.getOracleExchangeRate(destToken);
    if (askRate == null || askRate.eq(Dec.zeroDec())) {
      throw new ContractExeException("get ask rate failed, tokenid:" + ByteArray.toStr(destToken));
    }
    // get exchange rate
    Dec exchangeRate = offerRate.quo(askRate);
    Dec askAmount = Dec.newDec(offerAmount).mul(exchangeRate);

    ExchangeResult.Builder builder = ExchangeResult.newBuilder();
    builder.setSourceToken(ByteArray.toStr(sourceToken));
    builder.setDestToken(ByteArray.toStr(destToken));
    builder.setOfferAmount(offerAmount);
    builder.setAskAmount(askAmount.truncateLong());
    builder.setExchangeRate(exchangeRate.toString());

    Dec spread;
    if (!Arrays.equals(sourceToken, TRX_SYMBOL_BYTES) && !Arrays.equals(destToken, TRX_SYMBOL_BYTES)) {
      Dec sourceTobin = stableMarketStore.getTobinFee(sourceToken);
      Dec destTobin = stableMarketStore.getTobinFee(destToken);
      spread = Dec.maxDec(sourceTobin, destTobin);
      builder.setSpread(spread.toString());
      return builder.build();
    }

    Dec basePool = stableMarketStore.getBasePool();
    Dec delta = stableMarketStore.getPoolDelta();

    Dec cp = basePool.mul(basePool);
    Dec stablePool = basePool.add(delta);
    Dec trxPool = cp.quo(stablePool);
    Dec offerPool;
    Dec askPool;

    if (Arrays.equals(sourceToken, TRX_SYMBOL_BYTES)) {
      offerPool = trxPool;
      askPool = stablePool;
    } else {
      offerPool = stablePool;
      askPool = trxPool;
    }
    Dec askBaseAmount = askPool.sub(cp.quo(offerPool.add(baseOfferAmount)));
    spread = baseOfferAmount.sub(askBaseAmount).quo(baseOfferAmount);

    Dec minSpread = stableMarketStore.getMinSpread();
    if (spread.lt(minSpread)) {
      spread = minSpread;
    }
    builder.setSpread(spread.toString());

    return builder.build();
  }

  public void applySwapPool(byte[] sourceToken, byte[] destToken, long offerAmount, long askAmount)
      throws ContractExeException {
    if (!Arrays.equals(sourceToken, TRX_SYMBOL_BYTES) && !Arrays.equals(destToken, TRX_SYMBOL_BYTES)) {
      return;
    }

    Dec delta = stableMarketStore.getPoolDelta();
    if (!Arrays.equals(sourceToken, TRX_SYMBOL_BYTES) && Arrays.equals(destToken, TRX_SYMBOL_BYTES)) {
      Dec offerRate = stableMarketStore.getOracleExchangeRate(sourceToken);
      Dec baseOfferAmount = Dec.newDec(offerAmount).mul(offerRate);
      // todo check over flow
      delta = delta.add(baseOfferAmount);
    }

    if (Arrays.equals(sourceToken, TRX_SYMBOL_BYTES) && !Arrays.equals(destToken, TRX_SYMBOL_BYTES)) {
      Dec askRate = stableMarketStore.getOracleExchangeRate(destToken);
      Dec baseAskAmount = Dec.newDec(askAmount).mul(askRate);
      // todo check over flow
      delta = delta.sub(baseAskAmount);
    }
    Dec basePool = stableMarketStore.getBasePool();
    if (delta.gte(basePool)) {
      throw new ContractExeException("delta is greater than basePool");
    }
    stableMarketStore.setPoolDelta(delta);
  }

  public void adjustTrxExchangeTotalAmount(long amount) {
    long preAmount = stableMarketStore.getTrxExchangeAmount();
    stableMarketStore.setTrxExchangeAmount(preAmount + amount);
  }

  public boolean validateStable(byte[] tokenId) {
    if (Commons.getAssetIssueStoreFinal(dynamicPropertiesStore, assetIssueStore, assetIssueV2Store)
        .has(tokenId) && stableMarketStore.getStableCoinInfoById(tokenId) != null) {
      return true;
    }
    return Arrays.equals(tokenId, TRX_SYMBOL_BYTES);
  }
}
