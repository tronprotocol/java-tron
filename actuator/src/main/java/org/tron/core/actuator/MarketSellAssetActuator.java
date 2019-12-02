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
import org.tron.protos.Protocol.MarketPriceList.MarketPrice;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.MakerContract.MakerSellAssetContract;

@Slf4j(topic = "actuator")
public class MarketSellAssetActuator extends AbstractActuator {

  private AccountStore accountStore = chainBaseManager.getAccountStore();
  private DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
  private AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();

  private MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
  private MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();
  private MarketPairToPriceStore pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
  private MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
      .getMarketPairPriceToOrderStore();

  private byte[] sellTokenID = null;
  private byte[] buyTokenID = null;

  public MarketSellAssetActuator() {
    super(ContractType.MakerSellAssetContract, AssetIssueContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {

    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException("TransactionResultCapsule is null");
    }
    long fee = calcFee();

    try {
      final MakerSellAssetContract contract = this.any
          .unpack(MakerSellAssetContract.class);

      AccountCapsule accountCapsule = accountStore
          .get(contract.getOwnerAddress().toByteArray());

      sellTokenID = contract.getSellTokenId().toByteArray();
      buyTokenID = contract.getBuyTokenId().toByteArray();
      MarketPrice takerPrice = MarketPrice.newBuilder()
          .setSellTokenQuantity(contract.getSellTokenQuantity())
          .setBuyTokenQuantity(contract.getBuyTokenQuantity()).build();

      //fee
      accountCapsule.setBalance(accountCapsule.getBalance() - fee);

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
    } catch (ItemNotFoundException | InvalidProtocolBufferException e) {
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
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    if (!this.any.is(MakerSellAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [MakerSellAssetContract],real type[" + any
              .getClass() + "]");
    }

    final MakerSellAssetContract contract;
    try {
      contract =
          this.any.unpack(MakerSellAssetContract.class);
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
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (sellTokenQuantity <= 0) {
      throw new ContractValidateException("sellTokenQuantity must greater than 0!");
    }

    if (buyTokenQuantity <= 0) {
      throw new ContractValidateException("buyTokenQuantity must greater than 0!");
    }

    //Whether the accountStore exist
    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("Account does not exist!");
    }

    try {
      //Whether the balance is enough
      long fee = calcFee();

      if (Arrays.equals(sellTokenID, "_".getBytes())) {
        if (ownerAccount.getBalance() < Math.addExact(sellTokenQuantity, fee)) {
          throw new ContractValidateException("No enough balance !");
        }
      } else {
        AssetIssueCapsule assetIssueCapsule = Commons
            .getAssetIssueStoreFinal(dynamicStore, assetIssueStore, assetIssueV2Store)
            .get(sellTokenID);
        if (assetIssueCapsule == null) {
          throw new ContractValidateException("No sellTokenID : " + ByteArray.toStr(sellTokenID));
        }
        if (!ownerAccount.assetBalanceEnoughV2(sellTokenID, sellTokenQuantity,
            dynamicStore)) {
          throw new ContractValidateException("sellToken balance is not enough !");
        }
      }

      if (!Arrays.equals(sellTokenID, "_".getBytes())) {
        //Whether have the token
        AssetIssueCapsule assetIssueCapsule = Commons
            .getAssetIssueStoreFinal(dynamicStore, assetIssueStore, assetIssueV2Store)
            .get(buyTokenID);
        if (assetIssueCapsule == null) {
          throw new ContractValidateException("No buyTokenID : " + ByteArray.toStr(sellTokenID));
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
    return 0L;
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
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(makerPair);

    //match different price
    while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
        hasMatch(priceListCapsule, takerPrice)) {
      //get lowest ordersList
      MarketPrice makerPrice = priceListCapsule.getPricesList().get(0);
      byte[] pairPriceKey = MarketUtils.createPairPriceKey(
          priceListCapsule.getSellTokenId(), priceListCapsule.getBuyTokenId(),
          makerPrice.getSellTokenQuantity(), makerPrice.getBuyTokenQuantity());
      MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);
      List<ByteString> ordersList = orderIdListCapsule.getOrdersList();

      //match different order same price
      while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
          ordersList.size() != 0) {
        ByteString orderId = ordersList.get(0);
        MarketOrderCapsule makerOrderCapsule = orderStore.get(orderId.toByteArray());
        matchSingleOrder(takerCapsule, makerOrderCapsule);

        if (makerOrderCapsule.getSellTokenQuantityRemain() == 0) {
          ordersList.remove(0);
        }
      }

      orderIdListCapsule.setOrdersList(ordersList);
      pairPriceToOrderStore.put(pairPriceKey,orderIdListCapsule);

      if (ordersList.size() == 0) {
        priceListCapsule.removeFirst();
      }
    }

    pairToPriceStore.put(makerPair, priceListCapsule);
  }

  //return all match or not
  public void matchSingleOrder(MarketOrderCapsule takerOrderCapsule,
      MarketOrderCapsule makerOrderCapsule) throws ItemNotFoundException {

    // 根据maker的价格，计算taker的buy的量(成交量）,
    // for makerPrice,sellToken is A,buyToken is TRX.
    // for takerPrice,buyToken is A,sellToken is TRX.

    // makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX = takerBuyTokenQuantityCurrent_A/takerSellTokenQuantityRemain_TRX
    // => takerBuyTokenQuantityCurrent_A = takerSellTokenQuantityRemain_TRX * makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
    long takerBuyTokenQuantityRemain = takerOrderCapsule.getSellTokenQuantityRemain()
        * makerOrderCapsule.getSellTokenQuantity()
        / makerOrderCapsule.getBuyTokenQuantity();
    if (takerBuyTokenQuantityRemain == 0) {
      //交易量过小，直接将剩余 sellToken 返回用户
      returnSellTokenRemain(takerOrderCapsule);
      return;
    }

    long takerBuyTokenQuantityReceive = 0L;//In this match, the token obtained by taker
    long makerBuyTokenQuantityReceive = 0L;// the token obtained by maker

    if (takerBuyTokenQuantityRemain == makerOrderCapsule.getSellTokenQuantityRemain()) {
      // taker == maker
      takerOrderCapsule.setSellTokenQuantityRemain(0);
      makerOrderCapsule.setSellTokenQuantityRemain(0);

      takerBuyTokenQuantityReceive = makerOrderCapsule.getSellTokenQuantityRemain();
      makerBuyTokenQuantityReceive = takerOrderCapsule.getSellTokenQuantityRemain();

    } else if (takerBuyTokenQuantityRemain < makerOrderCapsule.getSellTokenQuantityRemain()) {
      // taker < maker
      // 当taker buy 的量小于 maker sell 的剩余量，所有taker的订单吃掉

      takerOrderCapsule.setSellTokenQuantityRemain(0);
      makerOrderCapsule.setSellTokenQuantityRemain(
          makerOrderCapsule.getSellTokenQuantityRemain() - takerBuyTokenQuantityRemain);

      takerBuyTokenQuantityReceive = takerBuyTokenQuantityRemain;
      makerBuyTokenQuantityReceive = takerOrderCapsule.getSellTokenQuantityRemain();

    } else {
      // taker > maker
      takerBuyTokenQuantityReceive = makerOrderCapsule.getSellTokenQuantityRemain();

      // 当taker buy 的量大于 maker sell 的剩余量，吃到maker的订单
      // makerSellTokenQuantityRemain_A/makerBuyTokenQuantityCurrent_TRX = makerSellTokenQuantity_A/makerBuyTokenQuantity_TRX
      makerBuyTokenQuantityReceive = makerOrderCapsule.getSellTokenQuantityRemain()
          * makerOrderCapsule.getBuyTokenQuantity()
          / makerOrderCapsule.getSellTokenQuantity();

      if (makerBuyTokenQuantityReceive == 0) {
        //交易量过小，直接将剩余 sellToken 返回 maker
        returnSellTokenRemain(makerOrderCapsule);
        return;
      } else {
        makerOrderCapsule.setSellTokenQuantityRemain(0);
        takerOrderCapsule.setSellTokenQuantityRemain(
            takerOrderCapsule.getSellTokenQuantityRemain() - makerBuyTokenQuantityReceive);
      }

    }

    //save makerOrderCapsule
    orderStore.put(makerOrderCapsule.getID().toByteArray(), makerOrderCapsule);

    //add token into account
    addTrxOrToken(takerOrderCapsule, takerBuyTokenQuantityReceive);
    addTrxOrToken(makerOrderCapsule, makerBuyTokenQuantityReceive);

  }


  public MarketOrderCapsule createAndSaveOrder(AccountCapsule accountCapsule,
      MakerSellAssetContract contract)
      throws ItemNotFoundException {

    MarketAccountOrderCapsule marketAccountOrderCapsule = marketAccountStore
        .get(contract.getOwnerAddress().toByteArray());
    if (marketAccountOrderCapsule == null) {
      marketAccountOrderCapsule = new MarketAccountOrderCapsule(contract.getOwnerAddress());
    }

    byte[] orderId = MarketUtils
        .calculateOrderId(contract.getOwnerAddress(), sellTokenID, buyTokenID,
            marketAccountOrderCapsule.getCount());
    MarketOrderCapsule orderCapsule = new MarketOrderCapsule(orderId, contract);

    marketAccountOrderCapsule.addOrders(orderCapsule.getID());
    marketAccountStore.put(accountCapsule.createDbKey(), marketAccountOrderCapsule);
    orderStore.put(orderId, orderCapsule);

    return orderCapsule;
  }


  public void transferBalanceOrToken(AccountCapsule accountCapsule,
      MakerSellAssetContract contract) {
    byte[] sellTokenID = contract.getSellTokenId().toByteArray();
    long sellTokenQuantity = contract.getSellTokenQuantity();

    if (Arrays.equals(sellTokenID, "_".getBytes())) {
      accountCapsule.setBalance(accountCapsule.getBalance() - sellTokenQuantity);
    } else {
      accountCapsule
          .reduceAssetAmountV2(sellTokenID, sellTokenQuantity, dynamicStore, assetIssueStore);
    }
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);
  }


  public void addTrxOrToken(MarketOrderCapsule orderCapsule, long num) {
    AccountCapsule makerAccountCapsule = accountStore
        .get(orderCapsule.getOwnerAddress().toByteArray());

    byte[] makerBuyTokenId = orderCapsule.getBuyTokenId();
    if (Arrays.equals(makerBuyTokenId, "_".getBytes())) {
      makerAccountCapsule.setBalance(
          makerAccountCapsule.getBalance() + num);
    } else {
      makerAccountCapsule
          .addAssetAmountV2(makerBuyTokenId, num, dynamicStore, assetIssueStore);
    }
  }

  public void returnSellTokenRemain(MarketOrderCapsule orderCapsule) {
    AccountCapsule makerAccountCapsule = accountStore
        .get(orderCapsule.getOwnerAddress().toByteArray());

    byte[] sellTokenId = orderCapsule.getSellTokenId();
    long sellTokenQuantityRemain = orderCapsule.getSellTokenQuantityRemain();
    if (Arrays.equals(sellTokenId, "_".getBytes())) {
      makerAccountCapsule.setBalance(
          makerAccountCapsule.getBalance() + sellTokenQuantityRemain);
    } else {
      makerAccountCapsule
          .addAssetAmountV2(sellTokenId, sellTokenQuantityRemain, dynamicStore, assetIssueStore);

    }
  }


  public boolean priceMatch(MarketPrice takerPrice, MarketPrice makerPrice) {

    // for takerPrice,buyToken is A,sellToken is TRX.
    // price_A_taker * buyQuantity_taker  = Price_TRX * sellQuantity_taker
    // ==> price_A_taker  = Price_TRX * sellQuantity_taker/buyQuantity_taker

    // price_A_taker must be greater or equal to price_A_maker
    // price_A_taker / price_A_maker >= 1
    // ==> Price_TRX * sellQuantity_taker/buyQuantity_taker > Price_TRX * buyQuantity_maker/sellQuantity_maker
    // ==> sellQuantity_taker * sellQuantity_maker > buyQuantity_taker * buyQuantity_maker

    return (takerPrice.getSellTokenQuantity() * makerPrice.getSellTokenQuantity()) >
        (takerPrice.getBuyTokenQuantity() * makerPrice.getBuyTokenQuantity());
  }


  public void saveRemainOrder(MarketOrderCapsule orderCapsule, MarketPrice currentPrice)
      throws ItemNotFoundException {

    //add price into pricesList
    byte[] pair = MarketUtils.createPairKey(sellTokenID, buyTokenID);
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(pair);
    if (priceListCapsule == null) {
      priceListCapsule = new MarketPriceListCapsule(sellTokenID, buyTokenID);
    }

    List<MarketPrice> pricesList = priceListCapsule.getPricesList();
    int index = 0;
    boolean found = false;
    for (int i = 0; i < pricesList.size(); i++) {
      index = i;
      if (isLowerPrice(currentPrice, pricesList.get(i))) {
        break;
      }
      if (isSamePrice(currentPrice, pricesList.get(i))) {
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

    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);
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
    return price1.getBuyTokenQuantity() * price2.getSellTokenQuantity()
        < price2.getBuyTokenQuantity() * price1.getSellTokenQuantity();
  }

  private boolean isSamePrice(MarketPrice price1, MarketPrice price2) {
    return price1.getBuyTokenQuantity() * price2.getSellTokenQuantity()
        == price2.getBuyTokenQuantity() * price1.getSellTokenQuantity();
  }


}
