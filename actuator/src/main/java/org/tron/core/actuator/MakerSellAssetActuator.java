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
import org.tron.common.utils.ByteArray;
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

    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();

    MakerAccountStore makerAccountStore = chainBaseManager.getMakerAccountStore();
    MakerOrderStore makerOrderStore = chainBaseManager.getMakerOrderStore();
    MakerPairToPriceStore makerPairToPriceStore = chainBaseManager.getMakerPairToPriceStore();
    MakerPairPriceToOrderStore makerPairPriceToOrderStore = chainBaseManager
        .getMakerPairPriceToOrderStore();

    try {
      final MakerSellAssetContract contract = this.any
          .unpack(MakerSellAssetContract.class);

      AccountCapsule accountCapsule = accountStore
          .get(contract.getOwnerAddress().toByteArray());

      //fee
      long newBalance = accountCapsule.getBalance() - fee;
      accountCapsule.setBalance(newBalance);

      //Transfer of balance
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

      //create and save order
      MakerAccountOrderCapsule makerAccountOrderCapsule = makerAccountStore
          .get(contract.getOwnerAddress().toByteArray());
      if (makerAccountOrderCapsule == null) {
        makerAccountOrderCapsule = new MakerAccountOrderCapsule(contract.getOwnerAddress());
      }
      byte[] orderId = MakerUtils
          .calculateOrderId(contract.getOwnerAddress(), sellTokenID, buyTokenID,
              makerAccountOrderCapsule.getCount());
      MakerOrderCapsule orderCapsule = new MakerOrderCapsule(orderId, contract);

      makerOrderStore.put(orderId, orderCapsule);
      makerAccountStore.put(accountCapsule.createDbKey(), makerAccountOrderCapsule);

      //match order
//      byte[] takerPair = createPairKey(sellTokenID, buyTokenID);
      byte[] makerPair = MakerUtils.createPairKey(buyTokenID, sellTokenID);
      MakerPriceListCapsule makerPriceListCapsule = makerPairToPriceStore.get(makerPair);

      matchOrder(orderCapsule, makerPriceListCapsule);

      if (orderCapsule.getSellTokenQuantityRemain() != 0) {
//        sellPriceListCapsule;
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


  public  boolean hasMatch(MakerOrderCapsule orderCapsule,
      MakerPriceListCapsule buyPriceListCapsule) {
    List<MakerPrice> pricesList = buyPriceListCapsule.getPricesList();
    if (pricesList.size() == 0) {
      return false;
    }
    MakerPrice buyPrice = pricesList.get(0);

    MakerPrice sellPrice = MakerPrice.newBuilder()
        .setSellTokenQuantity(orderCapsule.getSellTokenQuantity())
        .setBuyTokenQuantity(orderCapsule.getBuyTokenQuantity()).build();

    return priceMatch(sellPrice, buyPrice);
  }

  public void matchOrder(MakerOrderCapsule takerCapsule,
      MakerPriceListCapsule makerPriceListCapsule) throws ItemNotFoundException {

    MakerAccountStore makerAccountStore = chainBaseManager.getMakerAccountStore();
    MakerOrderStore orderStore = chainBaseManager.getMakerOrderStore();
    MakerPairToPriceStore pairToPriceStore = chainBaseManager.getMakerPairToPriceStore();
    MakerPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMakerPairPriceToOrderStore();

    //match different price
    while (takerCapsule.getSellTokenQuantityRemain() != 0 &&
        hasMatch(takerCapsule, makerPriceListCapsule)) {
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

    // 根据maker的价格，计算taker的buy的量,
    // 当taker sell的量小于 maker buy的量，所有taker的订单吃掉，返回true
    // 当taker sell的量大于 maker buy的量，吃到maker的订单，并且返回false

    long takerTokenQuantityReceive = takerOrderCapsule.getSellTokenQuantityRemain()
        * makerOrderCapsule.getBuyTokenQuantity()
        / makerOrderCapsule.getSellTokenQuantity();
    long makerTokenQuantityReceive = 0;

    //todo 要计算剩余需求
    if (takerOrderCapsule.getSellTokenQuantity() <= makerOrderCapsule.getBuyTokenQuantity()) {
      takerTokenQuantityReceive = takerOrderCapsule.getBuyTokenQuantity();
      makerTokenQuantityReceive = takerOrderCapsule.getSellTokenQuantity();
    } else {

    }

//    makerOrderCapsule.setSellTokenQuantityRemain();

//    long takerSellTokenQuantity = makerOrderCapsule.setSellTokenQuantityRemain();

  }


  public boolean priceMatch(MakerPrice takerPrice, MakerPrice makerPrice) {

    // ex.
    // for makerPrice,sellToken is A,buyToken is TRX.
    // price_A_maker * sellQuantity_maker = Price_TRX * buyQuantity_maker
    // ==> price_A_maker = Price_TRX * buyQuantity_maker/sellQuantity_maker

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


}
