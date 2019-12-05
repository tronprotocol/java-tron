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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
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
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketPriceList.MarketPrice;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;

@Slf4j(topic = "actuator")
public class MarketCancelOrderActuator extends AbstractActuator {

  private AccountStore accountStore = chainBaseManager.getAccountStore();
  private DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
  private AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();

  private MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
  private MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();
  private MarketPairToPriceStore pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
  private MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
      .getMarketPairPriceToOrderStore();

  public MarketCancelOrderActuator() {
    super(ContractType.MarketCancelOrderContract, AssetIssueContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {

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

      //fee
      accountCapsule.setBalance(accountCapsule.getBalance() - fee);
      // Add to blackHole address
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);

      // 1. return balance and token
      returnSellTokenRemain(orderCapsule);

      orderCapsule.setState(State.CANCELED);
      orderStore.put(orderCapsule.getID().toByteArray(), orderCapsule);

      //2. clear orderList
      byte[] pairPriceKey = MarketUtils.createPairPriceKey(
          orderCapsule.getSellTokenId(), orderCapsule.getBuyTokenId(),
          orderCapsule.getSellTokenQuantity(), orderCapsule.getBuyTokenQuantity());
      MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore.get(pairPriceKey);
      List<ByteString> ordersList = orderIdListCapsule.getOrdersList();
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

      if (ordersList.size() != 0) {
        orderIdListCapsule.setOrdersList(ordersList);
        pairPriceToOrderStore.put(pairPriceKey, orderIdListCapsule);
      } else {
        //if orderList is empty,delete
        pairPriceToOrderStore.delete(pairPriceKey);

        // 3. modify priceList
        byte[] makerPair = MarketUtils
            .createPairKey(orderCapsule.getBuyTokenId(), orderCapsule.getSellTokenId());
        MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(makerPair);
        List<MarketPrice> pricesList = priceListCapsule.getPricesList();
        Iterator<MarketPrice> iterator1 = pricesList.iterator();
        found = false;
        while (iterator1.hasNext()) {
          MarketPrice next = iterator1.next();
          if (next.getSellTokenQuantity() == orderCapsule.getSellTokenQuantity()
              && next.getBuyTokenQuantity() == orderCapsule.getBuyTokenQuantity()) {
            iterator1.remove();
            found = true;
            break;
          }
        }
        if (!found) {
          throw new ItemNotFoundException("orderIdList not exists");//should not happen
        }

        if (pricesList.size() != 0) {
          priceListCapsule.setPricesList(pricesList);
          pairToPriceStore.put(makerPair, priceListCapsule);
        } else {
          //if orderList is empty,delete
          pairToPriceStore.delete(makerPair);
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

    AccountStore accountStore = chainBaseManager.getAccountStore();

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
      throw new ContractValidateException("Invalid ownerAddress");
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
          "orderId[" + ByteArray.toHexString(orderId.toByteArray()) + "] not exists");
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
    AccountCapsule makerAccountCapsule = accountStore
        .get(orderCapsule.getOwnerAddress().toByteArray());

    byte[] sellTokenId = orderCapsule.getSellTokenId();
    long sellTokenQuantityRemain = orderCapsule.getSellTokenQuantityRemain();
    if (Arrays.equals(sellTokenId, "_".getBytes())) {
      makerAccountCapsule.setBalance(Math.addExact(
          makerAccountCapsule.getBalance(), sellTokenQuantityRemain));
    } else {
      makerAccountCapsule
          .addAssetAmountV2(sellTokenId, sellTokenQuantityRemain, dynamicStore, assetIssueStore);

    }
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AssetIssueContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0L;
  }

}
