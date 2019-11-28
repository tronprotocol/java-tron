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
import lombok.extern.slf4j.Slf4j;
import org.tron.common.zksnark.MakerUtils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.MakerAccountOrderCapsule;
import org.tron.core.capsule.MakerOrderCapsule;
import org.tron.core.capsule.MakerOrderIdListCapsule;
import org.tron.core.capsule.MakerPriceListCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.MakerAccountStore;
import org.tron.core.store.MakerOrderStore;
import org.tron.core.store.MakerPairPriceToOrderStore;
import org.tron.core.store.MakerPairToPriceStore;
import org.tron.protos.Protocol.MakerPriceList.MakerPrice;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

import java.util.List;
import java.util.Objects;
import org.tron.protos.contract.MakerContract.MakerSellAssetContract;

@Slf4j(topic = "actuator")
public class MakerSellAssetActuator extends AbstractActuator {

  private AccountStore accountStore = chainBaseManager.getAccountStore();
  private DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
  private AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();

  private MakerAccountStore makerAccountStore = chainBaseManager.getMakerAccountStore();
  private MakerOrderStore orderStore = chainBaseManager.getMakerOrderStore();
  private MakerPairToPriceStore pairToPriceStore = chainBaseManager.getMakerPairToPriceStore();
  private MakerPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
      .getMakerPairPriceToOrderStore();

  public MakerSellAssetActuator() {
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

      //fee
      long newBalance = accountCapsule.getBalance() - fee;
      accountCapsule.setBalance(newBalance);

      // 1. Transfer of balance
      byte[] sellTokenID = contract.getSellTokenId().toByteArray();
      long sellTokenQuantity = contract.getSellTokenQuantity();
      byte[] buyTokenID = contract.getBuyTokenId().toByteArray();
      long buyTokenQuantity = contract.getBuyTokenQuantity();

      if (Arrays.equals(sellTokenID, "_".getBytes())) {
        //deal with trx
        accountCapsule.setBalance(newBalance - sellTokenQuantity);
      } else {
        accountCapsule
            .reduceAssetAmountV2(sellTokenID, sellTokenQuantity, dynamicStore, assetIssueStore);
      }
      accountStore.put(accountCapsule.createDbKey(), accountCapsule);

      //2. create and save order
      MakerAccountOrderCapsule makerAccountOrderCapsule = makerAccountStore
          .get(contract.getOwnerAddress().toByteArray());
      if (makerAccountOrderCapsule == null) {
        makerAccountOrderCapsule = new MakerAccountOrderCapsule(contract.getOwnerAddress());
      }
      byte[] orderId = MakerUtils
          .calculateOrderId(contract.getOwnerAddress(), sellTokenID, buyTokenID,
              makerAccountOrderCapsule.getCount());
      MakerOrderCapsule orderCapsule = new MakerOrderCapsule(orderId, contract);

      orderStore.put(orderId, orderCapsule);
      makerAccountStore.put(accountCapsule.createDbKey(), makerAccountOrderCapsule);

      MakerPrice takerPrice = MakerPrice.newBuilder()
          .setSellTokenQuantity(orderCapsule.getSellTokenQuantity())
          .setBuyTokenQuantity(orderCapsule.getBuyTokenQuantity()).build();

      //3. match order
      byte[] makerPair = MakerUtils.createPairKey(buyTokenID, sellTokenID);
      MakerPriceListCapsule makerPriceListCapsule = pairToPriceStore.get(makerPair);

      matchOrder(orderCapsule, makerPriceListCapsule, takerPrice);

      //4. save remain order
      if (orderCapsule.getSellTokenQuantityRemain() != 0) {

        byte[] pair = MakerUtils.createPairKey(sellTokenID, buyTokenID);
        MakerPriceListCapsule priceListCapsule = pairToPriceStore.get(pair);
        saveRemainOrder(orderCapsule, priceListCapsule, takerPrice);
        orderStore.put(orderId, orderCapsule);
      }

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
      MakerPriceListCapsule buyPriceListCapsule, MakerPrice takerPrice) {
    List<MakerPrice> pricesList = buyPriceListCapsule.getPricesList();
    if (pricesList.size() == 0) {
      return false;
    }
    MakerPrice buyPrice = pricesList.get(0);
    return priceMatch(takerPrice, buyPrice);
  }


  public void matchOrder(MakerOrderCapsule takerCapsule,
      MakerPriceListCapsule makerPriceListCapsule, MakerPrice takerPrice)
      throws ItemNotFoundException {


    //match different price
    while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
        hasMatch(makerPriceListCapsule, takerPrice)) {
      MakerPrice makerPrice = makerPriceListCapsule.getPricesList().get(0);
      makerPriceListCapsule.removeFirst();
      byte[] pairPriceKey = MakerUtils.createPairPriceKey(
          makerPriceListCapsule.getSellTokenId(), makerPriceListCapsule.getBuyTokenId(),
          makerPrice.getSellTokenQuantity(), makerPrice.getBuyTokenQuantity());

      MakerOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);
      List<ByteString> ordersList = orderIdListCapsule.getOrdersList();

      //match different order same price
      while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
          ordersList.size() != 0) {
        ByteString orderId = ordersList.get(0);
        ordersList.remove(0);
        MakerOrderCapsule makerOrderCapsule = orderStore.get(orderId.toByteArray());
        matchSingleOrder(takerCapsule, makerOrderCapsule);
      }
    }
  }

  //return all match or not
  public void matchSingleOrder(MakerOrderCapsule takerOrderCapsule,
      MakerOrderCapsule makerOrderCapsule) throws ItemNotFoundException {

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
    addTrxOrTokenIntoAccount(takerOrderCapsule, takerBuyTokenQuantityReceive);
    addTrxOrTokenIntoAccount(makerOrderCapsule, makerBuyTokenQuantityReceive);

  }


  public void addTrxOrTokenIntoAccount(MakerOrderCapsule orderCapsule, long num) {
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

  public void returnSellTokenRemain(MakerOrderCapsule orderCapsule) {
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


  public boolean priceMatch(MakerPrice takerPrice, MakerPrice makerPrice) {

    // for takerPrice,buyToken is A,sellToken is TRX.
    // price_A_taker * buyQuantity_taker  = Price_TRX * sellQuantity_taker
    // ==> price_A_taker  = Price_TRX * sellQuantity_taker/buyQuantity_taker

    // price_A_taker must be greater or equal to price_A_maker
    // price_A_taker / price_A_maker >= 1
    // ==> Price_TRX * sellQuantity_taker/buyQuantity_taker > Price_TRX * buyQuantity_maker/sellQuantity_maker
    // ==> sellQuantity_taker * sellQuantity_maker > buyQuantity_taker * buyQuantity_maker

    return (takerPrice.getSellTokenQuantity() * makerPrice.getSellTokenQuantity()) /
        (takerPrice.getBuyTokenQuantity() * makerPrice.getBuyTokenQuantity()) > 1;
  }


  public void saveRemainOrder(MakerOrderCapsule orderCapsule,
      MakerPriceListCapsule priceListCapsule, MakerPrice currentPrice)
      throws ItemNotFoundException {

    MakerPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMakerPairPriceToOrderStore();

    byte[] pairPriceKey = MakerUtils.createPairPriceKey(
        orderCapsule.getSellTokenId(), orderCapsule.getBuyTokenId(),
        orderCapsule.getSellTokenQuantity(), orderCapsule.getBuyTokenQuantity());

    MakerOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);
    if (orderIdListCapsule != null) {
      //存在价格
      List<ByteString> ordersList = orderIdListCapsule.getOrdersList();
      ordersList.add(orderCapsule.getID());
    } else {
      //不存在价格，创建
      List<MakerPrice> pricesList = priceListCapsule.getPricesList();
      int index = 0;
      for (int i = 0; i < pricesList.size(); i++) {
        if (isLowerPrice(currentPrice, pricesList.get(i))) {
          index = i;
          break;
        }
      }
      pricesList.add(index, currentPrice);
    }


  }

  private boolean isLowerPrice(MakerPrice price1, MakerPrice price2) {
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

}
