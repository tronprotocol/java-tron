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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.zksnark.MarketUtils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.capsule.MarketPriceListCapsule;
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
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketPriceList.MarketPrice;
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

  private byte[] sellTokenID = null;
  private byte[] buyTokenID = null;

  public MarketSellAssetActuator() {
    super(ContractType.MarketSellAssetContract, AssetIssueContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {

    accountStore = chainBaseManager.getAccountStore();
    dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    assetIssueStore = chainBaseManager.getAssetIssueStore();
    assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();

    marketAccountStore = chainBaseManager.getMarketAccountStore();
    orderStore = chainBaseManager.getMarketOrderStore();
    pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

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
      MarketPrice takerPrice = MarketPrice.newBuilder()
          .setSellTokenQuantity(contract.getSellTokenQuantity())
          .setBuyTokenQuantity(contract.getBuyTokenQuantity()).build();

      //fee
      accountCapsule.setBalance(accountCapsule.getBalance() - fee);
      // Add to blackhole address
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);

      // 1. Transfer of balance
      transferBalanceOrToken(accountCapsule, contract);

      //2. create and save order
      MarketOrderCapsule orderCapsule = createAndSaveOrder(accountCapsule, contract);

      //3. match order
      matchOrder(orderCapsule, takerPrice);

      //4. save remain order into order book
      if (orderCapsule.getSellTokenQuantityRemain() != 0) {
        saveRemainOrder(orderCapsule, takerPrice);
      }

      orderStore.put(orderCapsule.getID().toByteArray(), orderCapsule);
      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException | InvalidProtocolBufferException | BalanceInsufficientException e) {
      logger.error(e.getMessage(), e);
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

    accountStore = chainBaseManager.getAccountStore();
    dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    assetIssueStore = chainBaseManager.getAssetIssueStore();
    assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();

    marketAccountStore = chainBaseManager.getMarketAccountStore();
    orderStore = chainBaseManager.getMarketOrderStore();
    pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

    if (!this.any.is(MarketSellAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [MarketSellAssetContract],real type[" + any
              .getClass() + "]");
    }

    final MarketSellAssetContract contract;
    try {
      contract =
          this.any.unpack(MarketSellAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    //Parameters check
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    byte[] sellTokenID = contract.getSellTokenId().toByteArray();
    byte[] buyTokenID = contract.getBuyTokenId().toByteArray();
    long sellTokenQuantity = contract.getSellTokenQuantity();
    long buyTokenQuantity = contract.getBuyTokenQuantity();

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    //Whether the accountStore exist
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
      //Whether the balance is enough
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
        //Whether have the token
        AssetIssueCapsule assetIssueCapsule = Commons
            .getAssetIssueStoreFinal(dynamicStore, assetIssueStore, assetIssueV2Store)
            .get(buyTokenID);
        if (assetIssueCapsule == null) {
          throw new ContractValidateException("No buyTokenID !");
        }
      }

    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AssetIssueContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return dynamicStore.getMarketSellFee();
  }

  public boolean hasMatch(
      MarketPriceListCapsule buyPriceListCapsule, MarketPrice takerPrice) {
    List<MarketPrice> pricesList = buyPriceListCapsule.getPricesList();
    if (pricesList.size() == 0) {
      return false;
    }
    MarketPrice buyPrice = pricesList.get(0);
    return priceMatch(takerPrice, buyPrice);
  }

  public void matchOrder(MarketOrderCapsule takerCapsule, MarketPrice takerPrice)
      throws ItemNotFoundException {

    byte[] makerPair = MarketUtils.createPairKey(buyTokenID, sellTokenID);
    MarketPriceListCapsule priceListCapsule = pairToPriceStore
        .getUnchecked(makerPair);//if not exists
    if (priceListCapsule == null) {
      return;
    }

    boolean priceListChanged = false;
    //match different price
    while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
        hasMatch(priceListCapsule, takerPrice)) {
      //get lowest ordersList
      MarketPrice makerPrice = priceListCapsule.getPricesList().get(0);
      byte[] pairPriceKey = MarketUtils.createPairPriceKey(
          priceListCapsule.getSellTokenId(), priceListCapsule.getBuyTokenId(),
          makerPrice.getSellTokenQuantity(), makerPrice.getBuyTokenQuantity());
      MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore
          .get(pairPriceKey);//if not exists

      List<ByteString> ordersList = new ArrayList<>(orderIdListCapsule.getOrdersList());
      boolean ordersListChanged = false;

      //match different order same price
      while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
          !ordersList.isEmpty()) {
        ByteString orderId = ordersList.get(0);
        MarketOrderCapsule makerOrderCapsule = orderStore.get(orderId.toByteArray());
        matchSingleOrder(takerCapsule, makerOrderCapsule);

        if (makerOrderCapsule.getSellTokenQuantityRemain() == 0) {
          ordersList.remove(0);
          ordersListChanged = true;
        }
      }

      if (ordersList.isEmpty()) {
        pairPriceToOrderStore.delete(pairPriceKey);
        priceListCapsule.removeFirst();
        priceListChanged = true;
      } else if (ordersListChanged) {
        orderIdListCapsule.setOrdersList(ordersList);
        pairPriceToOrderStore.put(pairPriceKey, orderIdListCapsule);
      }
    }

    if (priceListChanged) {
      pairToPriceStore.put(makerPair, priceListCapsule);
    }

  }

  //return all match or not
  public void matchSingleOrder(MarketOrderCapsule takerOrderCapsule,
      MarketOrderCapsule makerOrderCapsule) throws ItemNotFoundException {

    // 根据maker的价格，计算taker的buy的量(成交量）,
    // for makerPrice,sellToken is A,buyToken is TRX.
    // for takerPrice,buyToken is A,sellToken is TRX.

    // makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX = takerBuyTokenQuantityCurrent_A/takerSellTokenQuantityRemain_TRX
    // => takerBuyTokenQuantityCurrent_A = takerSellTokenQuantityRemain_TRX * makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
    long takerBuyTokenQuantityRemain = Math.floorDiv(
        Math.multiplyExact(takerOrderCapsule.getSellTokenQuantityRemain(),
            makerOrderCapsule.getSellTokenQuantity()),
        makerOrderCapsule.getBuyTokenQuantity());

    if (takerBuyTokenQuantityRemain == 0) {
      //交易量过小，直接将剩余 sellToken 返回用户
      returnSellTokenRemain(takerOrderCapsule);
      takerOrderCapsule.setState(State.INACTIVE);
      return;
    }

    long takerBuyTokenQuantityReceive;//In this match, the token obtained by taker
    long makerBuyTokenQuantityReceive;// the token obtained by maker

    if (takerBuyTokenQuantityRemain == makerOrderCapsule.getSellTokenQuantityRemain()) {
      // taker == maker

      // makerSellTokenQuantityRemain_A/makerBuyTokenQuantityCurrent_TRX = makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
      makerBuyTokenQuantityReceive = Math
          .floorDiv(Math.multiplyExact(makerOrderCapsule.getSellTokenQuantityRemain(),
              makerOrderCapsule.getBuyTokenQuantity()), makerOrderCapsule.getSellTokenQuantity());
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
      // 当taker buy 的量小于 maker sell 的剩余量，所有taker的订单吃掉

      takerBuyTokenQuantityReceive = takerBuyTokenQuantityRemain;
      makerBuyTokenQuantityReceive = takerOrderCapsule.getSellTokenQuantityRemain();

      takerOrderCapsule.setSellTokenQuantityRemain(0);
      takerOrderCapsule.setState(State.INACTIVE);

      makerOrderCapsule.setSellTokenQuantityRemain(Math.subtractExact(
          makerOrderCapsule.getSellTokenQuantityRemain(), takerBuyTokenQuantityRemain));


    } else {
      // taker > maker
      takerBuyTokenQuantityReceive = makerOrderCapsule.getSellTokenQuantityRemain();

      // 当taker buy 的量大于 maker sell 的剩余量，吃到maker的订单
      // makerSellTokenQuantityRemain_A/makerBuyTokenQuantityCurrent_TRX = makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
      makerBuyTokenQuantityReceive = Math
          .floorDiv(Math.multiplyExact(makerOrderCapsule.getSellTokenQuantityRemain(),
              makerOrderCapsule.getBuyTokenQuantity()), makerOrderCapsule.getSellTokenQuantity());

      makerOrderCapsule.setState(State.INACTIVE);
      if (makerBuyTokenQuantityReceive == 0) {
        //交易量过小，直接将剩余 sellToken 返回 maker
        // 不会出现在这种情况情况。
        // 对maker，sellQuantity<buyQuantity时，sellRemain=1时都能兑换至少一个buyToken
        // 因此假设 sellQuantity=200，buyQuantity=100,出现sellRemain=1，需要满足以下条件：
        // makerOrderCapsule.getSellTokenQuantityRemain() - takerBuyTokenQuantityRemain = 1
        // 200 - 200/100 * X = 1 ===> X = 199/2，这与X是整数的条件不符。
        returnSellTokenRemain(makerOrderCapsule);
        return;
      } else {
        makerOrderCapsule.setSellTokenQuantityRemain(0);
        takerOrderCapsule.setSellTokenQuantityRemain(Math.subtractExact(
            takerOrderCapsule.getSellTokenQuantityRemain(), makerBuyTokenQuantityReceive));
      }

    }

    //save makerOrderCapsule
    orderStore.put(makerOrderCapsule.getID().toByteArray(), makerOrderCapsule);

    //add token into account
    addTrxOrToken(takerOrderCapsule, takerBuyTokenQuantityReceive);
    addTrxOrToken(makerOrderCapsule, makerBuyTokenQuantityReceive);

  }


  public MarketOrderCapsule createAndSaveOrder(AccountCapsule accountCapsule,
      MarketSellAssetContract contract)
      throws ItemNotFoundException {

    MarketAccountOrderCapsule marketAccountOrderCapsule = marketAccountStore
        .getUnchecked(contract.getOwnerAddress().toByteArray());
    if (marketAccountOrderCapsule == null) {
      marketAccountOrderCapsule = new MarketAccountOrderCapsule(contract.getOwnerAddress());
    }

    byte[] orderId = MarketUtils
        .calculateOrderId(contract.getOwnerAddress(), sellTokenID, buyTokenID,
            marketAccountOrderCapsule.getCount());
    MarketOrderCapsule orderCapsule = new MarketOrderCapsule(orderId, contract);

    marketAccountOrderCapsule.addOrders(orderCapsule.getID());
    marketAccountOrderCapsule.setCount(marketAccountOrderCapsule.getCount() + 1);
    marketAccountStore.put(accountCapsule.createDbKey(), marketAccountOrderCapsule);
    orderStore.put(orderId, orderCapsule);

    return orderCapsule;
  }


  public void transferBalanceOrToken(AccountCapsule accountCapsule,
      MarketSellAssetContract contract) {
    byte[] sellTokenID = contract.getSellTokenId().toByteArray();
    long sellTokenQuantity = contract.getSellTokenQuantity();

    if (Arrays.equals(sellTokenID, "_".getBytes())) {
      accountCapsule.setBalance(Math.subtractExact(accountCapsule.getBalance(), sellTokenQuantity));
    } else {
      accountCapsule
          .reduceAssetAmountV2(sellTokenID, sellTokenQuantity, dynamicStore, assetIssueStore);
    }
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);
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


  public boolean priceMatch(MarketPrice takerPrice, MarketPrice makerPrice) {

    // for takerPrice,buyToken is A,sellToken is TRX.
    // price_A_taker * buyQuantity_taker  = Price_TRX * sellQuantity_taker
    // ==> price_A_taker  = Price_TRX * sellQuantity_taker/buyQuantity_taker

    // price_A_taker must be greater or equal to price_A_maker
    // price_A_taker / price_A_maker >= 1
    // ==> Price_TRX * sellQuantity_taker/buyQuantity_taker >= Price_TRX * buyQuantity_maker/sellQuantity_maker
    // ==> sellQuantity_taker * sellQuantity_maker > buyQuantity_taker * buyQuantity_maker

    return Math.multiplyExact(takerPrice.getSellTokenQuantity(), makerPrice.getSellTokenQuantity())
        >= Math.multiplyExact(takerPrice.getBuyTokenQuantity(), makerPrice.getBuyTokenQuantity());
  }


  public void saveRemainOrder(MarketOrderCapsule orderCapsule, MarketPrice currentPrice)
      throws ItemNotFoundException {

    //add price into pricesList
    byte[] pair = MarketUtils.createPairKey(sellTokenID, buyTokenID);
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.getUnchecked(pair);
    if (priceListCapsule == null) {
      priceListCapsule = new MarketPriceListCapsule(sellTokenID, buyTokenID);
    }

    List<MarketPrice> pricesList = new ArrayList<>(priceListCapsule.getPricesList());
    int index = 0;
    boolean found = false;
    for (; index < pricesList.size(); index++) {
      if (isLowerPrice(currentPrice, pricesList.get(index))) {
        break;
      }
      if (isSamePrice(currentPrice, pricesList.get(index))) {
        found = true;
        break;
      }
    }

    if (!found) {
      //price not exists
      pricesList.add(index, currentPrice);
      priceListCapsule.setPricesList(pricesList);
      pairToPriceStore.put(pair, priceListCapsule);
    }

    //add order into orderList
    byte[] pairPriceKey = MarketUtils.createPairPriceKey(
        orderCapsule.getSellTokenId(), orderCapsule.getBuyTokenId(),
        orderCapsule.getSellTokenQuantity(), orderCapsule.getBuyTokenQuantity());

    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.getUnchecked(pairPriceKey);
    if (orderIdListCapsule == null) {
      orderIdListCapsule = new MarketOrderIdListCapsule();
    }

    orderIdListCapsule.addOrders(orderCapsule.getID());
    pairPriceToOrderStore.put(pairPriceKey, orderIdListCapsule);
  }

  private boolean isLowerPrice(MarketPrice price1, MarketPrice price2) {
    // ex.
    // for sellToken is A,buyToken is TRX.
    // price_A_maker * sellQuantity_maker = Price_TRX * buyQuantity_maker
    // ==> price_A_maker = Price_TRX * buyQuantity_maker/sellQuantity_maker

    // price_A_maker_1 < price_A_maker_2
    // ==> buyQuantity_maker_1/sellQuantity_maker_1 < buyQuantity_maker_2/sellQuantity_maker_2
    // ==> buyQuantity_maker_1*sellQuantity_maker_2 < buyQuantity_maker_2 * sellQuantity_maker_1
    return Math.multiplyExact(price1.getBuyTokenQuantity(), price2.getSellTokenQuantity())
        < Math.multiplyExact(price2.getBuyTokenQuantity(), price1.getSellTokenQuantity());
  }

  private boolean isSamePrice(MarketPrice price1, MarketPrice price2) {
    return Math.multiplyExact(price1.getBuyTokenQuantity(), price2.getSellTokenQuantity())
        == Math.multiplyExact(price2.getBuyTokenQuantity(), price1.getSellTokenQuantity());
  }


}
