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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.zksnark.MarketUtils;
import org.tron.core.capsule.AccountCapsule;
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
import org.tron.core.store.MarketPriceStore;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketPrice;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;

@Slf4j(topic = "actuator")
public class MarketCancelOrderActuator extends AbstractActuator {

  private AccountStore accountStore;
  private DynamicPropertiesStore dynamicStore;
  private AssetIssueStore assetIssueStore;
  private AssetIssueV2Store assetIssueV2Store;

  private MarketAccountStore marketAccountStore;
  private MarketOrderStore orderStore;
  private MarketPairToPriceStore pairToPriceStore;
  private MarketPairPriceToOrderStore pairPriceToOrderStore;
  private MarketPriceStore marketPriceStore;

  public MarketCancelOrderActuator() {
    super(ContractType.MarketCancelOrderContract, MarketCancelOrderContract.class);
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
    pairPriceToOrderStore = chainBaseManager.getMarketPairPriceToOrderStore();
    marketPriceStore = chainBaseManager.getMarketPriceStore();

    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException("TransactionResultCapsule is null");
    }
    long fee = calcFee();

    try {
      final MarketCancelOrderContract contract = this.any
          .unpack(MarketCancelOrderContract.class);

      AccountCapsule accountCapsule = accountStore
          .get(contract.getOwnerAddress().toByteArray());

      byte[] orderId = contract.getOrderId().toByteArray();
      MarketOrderCapsule orderCapsule = orderStore.get(orderId);

      // fee
      accountCapsule.setBalance(accountCapsule.getBalance() - fee);
      accountStore.put(contract.getOwnerAddress().toByteArray(), accountCapsule);

      // Add to blackHole address
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);

      // 1. return balance and token
      returnSellTokenRemain(orderCapsule);

      orderCapsule.setState(State.CANCELED);
      orderStore.put(orderCapsule.getID().toByteArray(), orderCapsule);

      //2. clear orderList
      byte[] pairPriceKey = MarketUtils.createPairPriceKey(
          orderCapsule.getSellTokenId(),
          orderCapsule.getBuyTokenId(),
          orderCapsule.getSellTokenQuantity(),
          orderCapsule.getBuyTokenQuantity()
      );
      MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);
      List<ByteString> ordersList = new ArrayList<>(orderIdListCapsule.getOrdersList());
      Iterator<ByteString> iterator = ordersList.iterator();
      boolean found = false;
      while (iterator.hasNext()) {
        ByteString next = iterator.next();
        if (Arrays.equals(next.toByteArray(), orderId)) {
          iterator.remove();
          found = true;
          break;
        }
      }
      if (!found) {
        throw new ItemNotFoundException("orderId not exists");//should not happen
      }

      if (!ordersList.isEmpty()) {
        orderIdListCapsule.setOrdersList(ordersList);
        pairPriceToOrderStore.put(pairPriceKey, orderIdListCapsule);
      } else {
        //if orderList is empty,delete
        pairPriceToOrderStore.delete(pairPriceKey);

        // 3. modify priceList
        byte[] makerPair = MarketUtils.createPairKey(
            orderCapsule.getSellTokenId(),
            orderCapsule.getBuyTokenId()
        );
        MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(makerPair);

        // delete price from priceList
        MarketPrice marketPrice = marketPriceStore.get(pairPriceKey).getInstance();
        if (priceListCapsule
            .deleteCurrentPrice(marketPrice, pairPriceKey, marketPriceStore, makerPair,
                pairToPriceStore) == null) {
          // pairPriceToOrderStore.put(pairPriceKey, orderIdListCapsule);
        }
      }

      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException | InvalidProtocolBufferException | BalanceInsufficientException e) {
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

    if (!this.any.is(MarketCancelOrderContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [MarketCancelOrderContract],real type[" + any
              .getClass() + "]");
    }

    final MarketCancelOrderContract contract;
    try {
      contract =
          this.any.unpack(MarketCancelOrderContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    //Parameters check
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    ByteString orderId = contract.getOrderId();

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    //Whether the account  exist
    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("Account does not exist!");
    }

    //Whether the order exist
    MarketOrderCapsule marketOrderCapsule;
    try {
      marketOrderCapsule = orderStore.get(orderId.toByteArray());
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException(
          "orderId not exists");
    }

    if (!marketOrderCapsule.isActive()) {
      throw new ContractValidateException("Order is not active!");
    }

    if (!marketOrderCapsule.getOwnerAddress().equals(ownerAccount.getAddress())) {
      throw new ContractValidateException("Order does not belong to the account!");
    }

    //Whether the balance is enough
    long fee = calcFee();
    if (ownerAccount.getBalance() < fee) {
      throw new ContractValidateException("No enough balance !");
    }

    return true;
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

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AssetIssueContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return dynamicStore.getMarketCancelFee();
  }

}
