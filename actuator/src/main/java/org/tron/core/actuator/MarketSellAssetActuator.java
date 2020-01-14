/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.capsule.MarketPriceCapsule;
import org.tron.core.capsule.MarketPriceLinkedListCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.MarketAccountStore;
import org.tron.core.store.MarketOrderStore;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.core.store.MarketPriceStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketOrderPosition;
import org.tron.protos.Protocol.MarketPrice;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;

@Slf4j(topic = "actuator")
public class MarketSellAssetActuator extends AbstractActuator {

  private AccountStore accountStore;
  private DynamicPropertiesStore dynamicStore;
  private AssetIssueStore assetIssueStore;
  private AssetIssueV2Store assetIssueV2Store;

  private MarketAccountStore marketAccountStore;
  private MarketOrderStore orderStore;
  private MarketPairToPriceStore pairToPriceStore;
  private MarketPairPriceToOrderStore pairPriceToOrderStore;
  private MarketPriceStore marketPriceStore;

  private static final Integer MAX_SEARCH_NUM = 10;

  private byte[] sellTokenID = null;
  private byte[] buyTokenID = null;
  private long sellTokenQuantity;
  private long buyTokenQuantity;

  public MarketSellAssetActuator() {
    super(ContractType.MarketSellAssetContract, MarketSellAssetContract.class);
  }

  private void initStores() {
    accountStore = chainBaseManager.getAccountStore();
    dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    assetIssueStore = chainBaseManager.getAssetIssueStore();
    assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();

    marketAccountStore = chainBaseManager.getMarketAccountStore();
    orderStore = chainBaseManager.getMarketOrderStore();
    pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    pairPriceToOrderStore = chainBaseManager.getMarketPairPriceToOrderStore();
    marketPriceStore = chainBaseManager.getMarketPriceStore();
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {

    initStores();

    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException("TransactionResultCapsule is null");
    }
    long fee = calcFee();

    try {
      final MarketSellAssetContract contract = this.any
          .unpack(MarketSellAssetContract.class);

      AccountCapsule accountCapsule = accountStore
          .get(contract.getOwnerAddress().toByteArray());

      sellTokenID = contract.getSellTokenId().toByteArray();
      buyTokenID = contract.getBuyTokenId().toByteArray();
      sellTokenQuantity = contract.getSellTokenQuantity();
      buyTokenQuantity = contract.getBuyTokenQuantity();
      MarketPrice takerPrice = MarketPrice.newBuilder()
          .setSellTokenQuantity(sellTokenQuantity)
          .setBuyTokenQuantity(buyTokenQuantity).build();

      // fee
      accountCapsule.setBalance(accountCapsule.getBalance() - fee);
      // add to blackhole address
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);

      // 1. transfer of balance
      transferBalanceOrToken(accountCapsule);
      accountStore.put(accountCapsule.createDbKey(), accountCapsule);

      // 2. create and save order
      MarketOrderCapsule orderCapsule = createAndSaveOrder(accountCapsule, contract);

      // 3. match order
      matchOrder(orderCapsule, takerPrice);

      // 4. save remain order into order book
      if (orderCapsule.getSellTokenQuantityRemain() != 0) {
        ByteString prePriceKey = contract.getPrePriceKey();
        MarketOrderPosition position = MarketOrderPosition.newBuilder()
            .setPrePriceKey(prePriceKey).build();
        saveRemainOrder(orderCapsule, takerPrice, position);
      }

      orderStore.put(orderCapsule.getID().toByteArray(), orderCapsule);
      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException
        | InvalidProtocolBufferException
        | BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException("No contract!");
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or dynamic store!");
    }

    initStores();

    if (!this.any.is(MarketSellAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [MarketSellAssetContract],real type[" + any
              .getClass() + "]");
    }

    if (!dynamicStore.supportAllowMarketTransaction()) {
      throw new ContractValidateException("Not support Market Transaction, need to be opened by"
          + " the committee");
    }

    final MarketSellAssetContract contract;
    try {
      contract =
          this.any.unpack(MarketSellAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    // Parameters check
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    sellTokenID = contract.getSellTokenId().toByteArray();
    buyTokenID = contract.getBuyTokenId().toByteArray();
    sellTokenQuantity = contract.getSellTokenQuantity();
    buyTokenQuantity = contract.getBuyTokenQuantity();

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    // Whether the accountStore exist
    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("Account does not exist!");
    }

    if (!Arrays.equals(sellTokenID, "_".getBytes()) && !TransactionUtil.isNumber(sellTokenID)) {
      throw new ContractValidateException("sellTokenID is not a valid number");
    }
    if (!Arrays.equals(buyTokenID, "_".getBytes()) && !TransactionUtil
        .isNumber(buyTokenID)) {
      throw new ContractValidateException("buyTokenID is not a valid number");
    }

    if (Arrays.equals(sellTokenID, buyTokenID)) {
      throw new ContractValidateException("cannot exchange same tokens");
    }

    if (sellTokenQuantity <= 0 || buyTokenQuantity <= 0) {
      throw new ContractValidateException("token quantity must greater than zero");
    }

    long quantityLimit = dynamicStore.getMarketQuantityLimit();
    if (sellTokenQuantity > quantityLimit || buyTokenQuantity > quantityLimit) {
      throw new ContractValidateException("token quantity must less than " + quantityLimit);
    }

    try {
      // Whether the balance is enough
      long fee = calcFee();

      if (Arrays.equals(sellTokenID, "_".getBytes())) {
        if (ownerAccount.getBalance() < Math.addExact(sellTokenQuantity, fee)) {
          throw new ContractValidateException("No enough balance !");
        }
      } else {
        if (ownerAccount.getBalance() < fee) {
          throw new ContractValidateException("No enough balance !");
        }

        AssetIssueCapsule assetIssueCapsule = Commons
            .getAssetIssueStoreFinal(dynamicStore, assetIssueStore, assetIssueV2Store)
            .get(sellTokenID);
        if (assetIssueCapsule == null) {
          throw new ContractValidateException("No sellTokenID !");
        }
        if (!ownerAccount.assetBalanceEnoughV2(sellTokenID, sellTokenQuantity,
            dynamicStore)) {
          throw new ContractValidateException("SellToken balance is not enough !");
        }
      }

      if (!Arrays.equals(buyTokenID, "_".getBytes())) {
        // Whether have the token
        AssetIssueCapsule assetIssueCapsule = Commons
            .getAssetIssueStoreFinal(dynamicStore, assetIssueStore, assetIssueV2Store)
            .get(buyTokenID);
        if (assetIssueCapsule == null) {
          throw new ContractValidateException("No buyTokenID !");
        }
      }

      byte[] prePriceKey = contract.getPrePriceKey().toByteArray();
      checkPosition(prePriceKey);

    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  private void checkPosition(byte[] prePriceKey)
      throws ContractValidateException {

    MarketPrice newPrice = MarketPrice.newBuilder().setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenQuantity(buyTokenQuantity).build();

    // check position info
    if (prePriceKey.length != 0) {
      MarketPriceCapsule prePriceCapsule = marketPriceStore.getUnchecked(prePriceKey);
      if (prePriceCapsule == null) {
        throw new ContractValidateException("prePriceKey not exists");
      }

      // pre price should be less than current price
      if (!MarketUtils.isLowerPrice(prePriceCapsule.getInstance(), newPrice)) {
        throw new ContractValidateException("pre price should be less than current price");
      }
    }

    byte[] newPairPriceKey = MarketUtils
        .createPairPriceKey(sellTokenID, buyTokenID, sellTokenQuantity, buyTokenQuantity);
    MarketPriceCapsule newPriceCapsule = marketPriceStore.getUnchecked(newPairPriceKey);

    if (newPriceCapsule != null) {
      // if price exists, no need to use position info
      return;
    }

    // get the start position
    MarketPriceCapsule head = null;
    if (prePriceKey.length == 0) {
      // search from the bestPrice
      // check if price list or bestPrice exists
      MarketPriceCapsule bestPrice = null;
      byte[] makerPair = MarketUtils.createPairKey(sellTokenID, buyTokenID);
      MarketPriceLinkedListCapsule priceListCapsule = pairToPriceStore.getUnchecked(makerPair);
      if (priceListCapsule != null) {
        bestPrice = new MarketPriceCapsule(priceListCapsule.getBestPrice());
      }
      if (bestPrice == null || bestPrice.isNull()) {
        // if price list is empty, no need to search
        return;
      }
      head = bestPrice;
    } else {
      // search from the prePrice
      // has checked prePrice exist before
      MarketPriceCapsule prePriceCapsule = marketPriceStore.getUnchecked(prePriceKey);
      head = prePriceCapsule;
    }

    // check how many times need to find the correct position
    MarketPriceCapsule dummy = new MarketPriceCapsule(0, 0);
    if (!head.isNull()) {
      dummy.setNext(head.getKey(sellTokenID, buyTokenID));
    }
    head = dummy;
    Integer count = 0;
    while (count <= MAX_SEARCH_NUM && !head.isNextNull()) {
      if (MarketUtils
          .isLowerPrice(marketPriceStore.getUnchecked(head.getNext()).getInstance(), newPrice)) {
        head = marketPriceStore.getUnchecked(head.getNext());
      } else {
        break;
      }
      count++;
    }

    if (count > MAX_SEARCH_NUM) {
      throw new ContractValidateException("Maximum number of queries exceeded，10");
    }

  }


  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AssetIssueContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return dynamicStore.getMarketSellFee();
  }

  public boolean hasMatch(MarketPriceLinkedListCapsule makerPriceListCapsule,
      MarketPrice takerPrice) {
    MarketPrice bestPrice = makerPriceListCapsule.getBestPrice();
    if (new MarketPriceCapsule(bestPrice).isNull()) {
      return false;
    }

    return MarketUtils.priceMatch(takerPrice, bestPrice);
  }

  public void matchOrder(MarketOrderCapsule takerCapsule, MarketPrice takerPrice)
      throws ItemNotFoundException {

    byte[] makerPair = MarketUtils.createPairKey(buyTokenID, sellTokenID);
    MarketPriceLinkedListCapsule makerPriceListCapsule = pairToPriceStore.getUnchecked(makerPair);

    // if not exists
    if (makerPriceListCapsule == null) {
      return;
    }

    // match different price
    while (takerCapsule.getSellTokenQuantityRemain() != 0
        && hasMatch(makerPriceListCapsule, takerPrice)) {
      // get lowest ordersList
      MarketPrice makerPrice = makerPriceListCapsule.getBestPrice();

      byte[] pairPriceKey = MarketUtils.createPairPriceKey(
          makerPriceListCapsule.getSellTokenId(),
          makerPriceListCapsule.getBuyTokenId(),
          makerPrice.getSellTokenQuantity(),
          makerPrice.getBuyTokenQuantity()
      );

      // if not exists
      MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);

      // match different order same price
      while (takerCapsule.getSellTokenQuantityRemain() != 0
          && !orderIdListCapsule.isOrderEmpty()) {
        byte[] orderId = orderIdListCapsule.getHead();
        MarketOrderCapsule makerOrderCapsule = orderStore.get(orderId);
        matchSingleOrder(takerCapsule, makerOrderCapsule);

        // remove order
        if (makerOrderCapsule.getSellTokenQuantityRemain() == 0) {
          orderIdListCapsule.removeOrder(makerOrderCapsule, orderStore,
              pairPriceKey, pairPriceToOrderStore);
        }
      }

      // makerPrice all consumed
      if (orderIdListCapsule.isOrderEmpty()) {
        pairPriceToOrderStore.delete(pairPriceKey);

        // need to delete marketPair
        if (makerPriceListCapsule
            .deleteCurrentPrice(makerPrice, pairPriceKey, marketPriceStore, makerPair,
                pairToPriceStore) == null) {
          break;
        }

      }
    } // end while
  }

  // return all match or not
  public void matchSingleOrder(MarketOrderCapsule takerOrderCapsule,
      MarketOrderCapsule makerOrderCapsule) {

    BigInteger takerSellRemainQuantity = BigInteger
        .valueOf(takerOrderCapsule.getSellTokenQuantityRemain());
    BigInteger makerSellQuantity = BigInteger.valueOf(makerOrderCapsule.getSellTokenQuantity());
    BigInteger makerBuyQuantity = BigInteger.valueOf(makerOrderCapsule.getBuyTokenQuantity());
    BigInteger makerSellRemainQuantity = BigInteger
        .valueOf(makerOrderCapsule.getSellTokenQuantityRemain());

    // according to the price of maker, calculate the quantity of taker can buy
    // for makerPrice,sellToken is A,buyToken is TRX.
    // for takerPrice,buyToken is A,sellToken is TRX.

    // makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX = takerBuyTokenQuantityCurrent_A/takerSellTokenQuantityRemain_TRX
    // => takerBuyTokenQuantityCurrent_A = takerSellTokenQuantityRemain_TRX * makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
    long takerBuyTokenQuantityRemain = takerSellRemainQuantity.multiply(makerSellQuantity)
        .divide(makerBuyQuantity).longValue();

    if (takerBuyTokenQuantityRemain == 0) {
      // quantity too small, return sellToken to user
      returnSellTokenRemain(takerOrderCapsule);
      takerOrderCapsule.setState(State.INACTIVE);
      return;
    }

    long takerBuyTokenQuantityReceive; // In this match, the token obtained by taker
    long makerBuyTokenQuantityReceive; // the token obtained by maker

    if (takerBuyTokenQuantityRemain == makerOrderCapsule.getSellTokenQuantityRemain()) {
      // taker == maker

      // makerSellTokenQuantityRemain_A/makerBuyTokenQuantityCurrent_TRX = makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
      // => makerBuyTokenQuantityCurrent_TRX = makerSellTokenQuantityRemain_A * makerBuyTokenQuantity_TRX / makerSellTokenQuantity_A

      makerBuyTokenQuantityReceive = makerSellRemainQuantity.multiply(makerBuyQuantity)
          .divide(makerSellQuantity).longValue();
      takerBuyTokenQuantityReceive = makerOrderCapsule.getSellTokenQuantityRemain();

      long takerSellTokenLeft =
          takerOrderCapsule.getSellTokenQuantityRemain() - makerBuyTokenQuantityReceive;
      takerOrderCapsule.setSellTokenQuantityRemain(takerSellTokenLeft);
      makerOrderCapsule.setSellTokenQuantityRemain(0);

      if (takerSellTokenLeft == 0) {
        takerOrderCapsule.setState(State.INACTIVE);
      }
      makerOrderCapsule.setState(State.INACTIVE);


    } else if (takerBuyTokenQuantityRemain < makerOrderCapsule.getSellTokenQuantityRemain()) {
      // taker < maker
      // if the quantity of taker want to buy is smaller than the remain of maker want to sell,
      // consume the order of the taker

      takerBuyTokenQuantityReceive = takerBuyTokenQuantityRemain;
      makerBuyTokenQuantityReceive = takerOrderCapsule.getSellTokenQuantityRemain();

      takerOrderCapsule.setSellTokenQuantityRemain(0);
      takerOrderCapsule.setState(State.INACTIVE);

      makerOrderCapsule.setSellTokenQuantityRemain(Math.subtractExact(
          makerOrderCapsule.getSellTokenQuantityRemain(), takerBuyTokenQuantityRemain));


    } else {
      // taker > maker
      takerBuyTokenQuantityReceive = makerOrderCapsule.getSellTokenQuantityRemain();

      // if the quantity of taker want to buy is bigger than the remain of maker want to sell,
      // consume the order of maker
      // makerSellTokenQuantityRemain_A/makerBuyTokenQuantityCurrent_TRX = makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
//      makerBuyTokenQuantityReceive = Math
//          .floorDiv(Math.multiplyExact(makerOrderCapsule.getSellTokenQuantityRemain(),
//              makerOrderCapsule.getBuyTokenQuantity()), makerOrderCapsule.getSellTokenQuantity());
      makerBuyTokenQuantityReceive = makerSellRemainQuantity.multiply(makerBuyQuantity)
          .divide(makerSellQuantity).longValue();

      makerOrderCapsule.setState(State.INACTIVE);
      if (makerBuyTokenQuantityReceive == 0) {
        // the quantity is too small, return the remain of sellToken to maker
        // it would not happen here
        // for the maker, when sellQuantity < buyQuantity, it will get at least one buyToken
        // even when sellRemain = 1.
        // so if sellQuantity=200，buyQuantity=100, when sellRemain=1, it needs to be satisfied
        // the following conditions:
        // makerOrderCapsule.getSellTokenQuantityRemain() - takerBuyTokenQuantityRemain = 1
        // 200 - 200/100 * X = 1 ===> X = 199/2，and this comports with the fact that X is integer.
        returnSellTokenRemain(makerOrderCapsule);
        return;
      } else {
        makerOrderCapsule.setSellTokenQuantityRemain(0);
        takerOrderCapsule.setSellTokenQuantityRemain(Math.subtractExact(
            takerOrderCapsule.getSellTokenQuantityRemain(), makerBuyTokenQuantityReceive));
      }

    }

    // save makerOrderCapsule
    orderStore.put(makerOrderCapsule.getID().toByteArray(), makerOrderCapsule);

    // add token into account
    addTrxOrToken(takerOrderCapsule, takerBuyTokenQuantityReceive);
    addTrxOrToken(makerOrderCapsule, makerBuyTokenQuantityReceive);

  }


  public MarketOrderCapsule createAndSaveOrder(AccountCapsule accountCapsule,
      MarketSellAssetContract contract) {

    MarketAccountOrderCapsule marketAccountOrderCapsule = marketAccountStore
        .getUnchecked(contract.getOwnerAddress().toByteArray());
    if (marketAccountOrderCapsule == null) {
      marketAccountOrderCapsule = new MarketAccountOrderCapsule(contract.getOwnerAddress());
    }

    byte[] orderId = MarketUtils
        .calculateOrderId(contract.getOwnerAddress(), sellTokenID, buyTokenID,
            marketAccountOrderCapsule.getCount());
    MarketOrderCapsule orderCapsule = new MarketOrderCapsule(orderId, contract);

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    orderCapsule.setCreateTime(now);

    marketAccountOrderCapsule.addOrders(orderCapsule.getID());
    marketAccountOrderCapsule.setCount(marketAccountOrderCapsule.getCount() + 1);
    marketAccountStore.put(accountCapsule.createDbKey(), marketAccountOrderCapsule);
    orderStore.put(orderId, orderCapsule);

    return orderCapsule;
  }


  public void transferBalanceOrToken(AccountCapsule accountCapsule) {

    if (Arrays.equals(sellTokenID, "_".getBytes())) {
      accountCapsule.setBalance(Math.subtractExact(accountCapsule.getBalance(), sellTokenQuantity));
    } else {
      accountCapsule
          .reduceAssetAmountV2(sellTokenID, sellTokenQuantity, dynamicStore, assetIssueStore);
    }

  }


  public void addTrxOrToken(MarketOrderCapsule orderCapsule, long num) {
    AccountCapsule accountCapsule = accountStore
        .get(orderCapsule.getOwnerAddress().toByteArray());

    byte[] buyTokenId = orderCapsule.getBuyTokenId();
    if (Arrays.equals(buyTokenId, "_".getBytes())) {
      accountCapsule.setBalance(Math.addExact(accountCapsule.getBalance(), num));
    } else {
      accountCapsule
          .addAssetAmountV2(buyTokenId, num, dynamicStore, assetIssueStore);
    }
    accountStore.put(orderCapsule.getOwnerAddress().toByteArray(), accountCapsule);
  }

  public void returnSellTokenRemain(MarketOrderCapsule orderCapsule) {
    AccountCapsule accountCapsule = accountStore
        .get(orderCapsule.getOwnerAddress().toByteArray());

    byte[] sellTokenId = orderCapsule.getSellTokenId();
    long sellTokenQuantityRemain = orderCapsule.getSellTokenQuantityRemain();
    if (Arrays.equals(sellTokenId, "_".getBytes())) {
      accountCapsule.setBalance(Math.addExact(
          accountCapsule.getBalance(), sellTokenQuantityRemain));
    } else {
      accountCapsule
          .addAssetAmountV2(sellTokenId, sellTokenQuantityRemain, dynamicStore, assetIssueStore);
    }
    accountStore.put(orderCapsule.getOwnerAddress().toByteArray(), accountCapsule);
    orderCapsule.setSellTokenQuantityRemain(0L);

  }




  public void saveRemainOrder(MarketOrderCapsule orderCapsule, MarketPrice currentPrice,
      MarketOrderPosition position)
      throws ItemNotFoundException {

    // add price into pricesList
    byte[] pairKey = MarketUtils.createPairKey(sellTokenID, buyTokenID);
    MarketPriceLinkedListCapsule priceListCapsule = pairToPriceStore.getUnchecked(pairKey);
    if (priceListCapsule == null) {
      priceListCapsule = new MarketPriceLinkedListCapsule(sellTokenID, buyTokenID);
    }

    MarketPriceCapsule headPriceCapsule = priceListCapsule
        .insertMarket(currentPrice, sellTokenID, buyTokenID, marketPriceStore, position);
    if (headPriceCapsule != null) {
      priceListCapsule.setBestPrice(headPriceCapsule);
      pairToPriceStore.put(pairKey, priceListCapsule);
    }

    // add order into orderList
    byte[] pairPriceKey = MarketUtils.createPairPriceKey(
        orderCapsule.getSellTokenId(),
        orderCapsule.getBuyTokenId(),
        orderCapsule.getSellTokenQuantity(),
        orderCapsule.getBuyTokenQuantity()
    );

    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.getUnchecked(pairPriceKey);
    if (orderIdListCapsule == null) {
      orderIdListCapsule = new MarketOrderIdListCapsule();
    }

    orderIdListCapsule.addOrder(orderCapsule, orderStore);
    pairPriceToOrderStore.put(pairPriceKey, orderIdListCapsule);
  }

}
