package org.tron.core.utils;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.core.ChainBaseManager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StableMarketStore;
import org.tron.protos.contract.StableMarketContractOuterClass.ExchangeResult;

@Slf4j
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
    Dec baseOfferAmount = null;
    Dec offerRate = stableMarketStore.getOracleExchangeRate(sourceToken);
    try {
      baseOfferAmount = computeInternalSwap(sourceToken, getSDRTokenId(), offerAmount);
    } catch (ItemNotFoundException e) {
      throw new ContractExeException("computeInternalSwap source token failed, tokenid:" + ByteArray.toStr(sourceToken));
    }
    Dec askAmount = null;
    Dec askRate = stableMarketStore.getOracleExchangeRate(destToken);
    try {
      askAmount = computeInternalSwap(getSDRTokenId(), destToken, baseOfferAmount.truncateLong());
    } catch (ItemNotFoundException e) {
      throw new ContractExeException("computeInternalSwap dest token failed, tokenid:" + ByteArray.toStr(destToken));

    }
    // get exchange rate
    Dec exchangeRate = askRate.quo(offerRate);

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
      Dec baseOfferAmount = null;
      try {
        baseOfferAmount = computeInternalSwap(sourceToken, getSDRTokenId(), offerAmount);
      } catch (ItemNotFoundException e) {
        throw new ContractExeException("applySwapPool: source token computeInternalSwap failed");
      }
      // todo check over flow, remove log
      logger.info("update pool delta, delta: " + delta + ", baseOfferAmount: " + baseOfferAmount);
      delta = delta.add(baseOfferAmount);
    }

    if (Arrays.equals(sourceToken, TRX_SYMBOL_BYTES) && !Arrays.equals(destToken, TRX_SYMBOL_BYTES)) {
      Dec baseAskAmount = null;
      try {
        baseAskAmount = computeInternalSwap(destToken, getSDRTokenId(), askAmount);
      } catch (ItemNotFoundException e) {
        throw new ContractExeException("applySwapPool: dest token computeInternalSwap failed");
      }
      // todo check over flow, remove log
      logger.info("update pool delta, delta: " + delta + ", baseAskAmount: " + baseAskAmount);
      delta = delta.sub(baseAskAmount);
    }
    Dec basePool = stableMarketStore.getBasePool();
    if (delta.gte(basePool)) {
      throw new ContractExeException("delta is greater than basePool");
    }
    stableMarketStore.setPoolDelta(delta);
  }

  public Dec computeInternalSwap(byte[] sourceToken, byte[] destToken, long amount) throws ItemNotFoundException {
    if(Arrays.equals(sourceToken, destToken)) {
      return Dec.newDec(amount);
    }
    Dec offerRate = stableMarketStore.getOracleExchangeRate(sourceToken);
    if (offerRate == null) {
      throw new ItemNotFoundException("get offer exchange rate failed, tokenid: " + ByteArray.toStr(sourceToken));
    }
    Dec askRate = stableMarketStore.getOracleExchangeRate(destToken);
    if (askRate == null) {
      throw new ItemNotFoundException("get ask exchange rate failed, tokenid: " + ByteArray.toStr(destToken));
    }
    return Dec.newDec(amount).mul(askRate).quo(offerRate);
  }

  public byte[] getSDRTokenId() {
    return stableMarketStore.getSDRTokenId();
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
