package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.core.store.MarketPriceStore;
import org.tron.protos.Protocol.MarketPriceList;
import org.tron.protos.Protocol.MarketPrice;

@Slf4j(topic = "capsule")
public class MarketPriceListCapsule implements ProtoCapsule<MarketPriceList> {

  private MarketPriceList priceList;

  public MarketPriceListCapsule(final MarketPriceList priceList) {
    this.priceList = priceList;
  }

  public MarketPriceListCapsule(final byte[] data) {
    try {
      this.priceList = MarketPriceList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MarketPriceListCapsule(byte[] sellTokenId, byte[] buyTokenId) {
    this.priceList = MarketPriceList.newBuilder()
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .build();
  }

  public byte[] getSellTokenId() {
    return this.priceList.getSellTokenId().toByteArray();
  }

  public void setSellTokenId(byte[] id) {
    this.priceList = this.priceList.toBuilder()
        .setSellTokenId(ByteString.copyFrom(id))
        .build();
  }


  public byte[] getBuyTokenId() {
    return this.priceList.getBuyTokenId().toByteArray();
  }

  public void setBuyTokenId(byte[] id) {
    this.priceList = this.priceList.toBuilder()
        .setBuyTokenId(ByteString.copyFrom(id))
        .build();
  }

  public void setPricesList(List<MarketPrice> pricesList) {
    this.priceList = this.priceList.toBuilder()
        .clearPrices()
        .addAllPrices(pricesList)
        .build();
  }


  public List<MarketPrice> getPricesList() {
    return this.priceList.getPricesList();
  }

  public int getPriceSize(MarketPriceStore marketPriceStore) throws ItemNotFoundException {
    MarketPriceCapsule head = new MarketPriceCapsule(this.getBestPrice());

    if (head.isNull()) {
      return 0;
    }

    int size = 1;
    while(!head.isNextNull()) {
      size ++;
      head = marketPriceStore.get(head.getNext());
    }
    return size;
  }

  public void setBestPrice(MarketPriceCapsule bestPrice) {
    this.priceList = this.priceList.toBuilder().setBestPrice(bestPrice.getInstance()).build();
  }


  // insert price by sort, if same, just return
  public MarketPriceCapsule insertMarket(MarketPrice marketPrice, byte[] sellTokenID,
      byte[] buyTokenID, MarketPriceStore marketPriceStore) throws ItemNotFoundException {

    MarketPriceCapsule head = new MarketPriceCapsule(this.getBestPrice());

    // dummy.next = head
    MarketPriceCapsule dummy = new MarketPriceCapsule(0, 0, new byte[0]);
    if (head.isNull()) {
      dummy.setNext(new byte[0]);
    } else {
      dummy.setNext(head.getKey(sellTokenID, buyTokenID));

    }

    head = dummy;

    boolean found = false;
    while (!head.isNextNull()) {
      if (isLowerPrice(marketPriceStore.get(head.getNext()).getInstance(), marketPrice)) {
        head = marketPriceStore.get(head.getNext());
      } else {
        if (isSamePrice(marketPriceStore.get(head.getNext()).getInstance(), marketPrice)) {
          found = true;
        }
        break;
      }
    }

    if (!found) {
      // node.next = head.next
      marketPrice = marketPrice.toBuilder().setNext(ByteString.copyFrom(head.getNext())).build();

      MarketPriceCapsule marketPriceCapsule = new MarketPriceCapsule(marketPrice);
      byte[] priceKey = marketPriceCapsule.getKey(sellTokenID, buyTokenID);

      // head.next = node
      head.setNext(priceKey);
      marketPriceStore.put(head.getKey(sellTokenID, buyTokenID), head);

      marketPriceStore.put(priceKey, marketPriceCapsule);

      // dummy.next
      return marketPriceStore.get(dummy.getNext());
    }

    return null;
  }

  public void addPrices(long s, long b) {
    MarketPrice build = MarketPrice.newBuilder().setSellTokenQuantity(s).setBuyTokenQuantity(b)
        .build();
    this.priceList = this.priceList.toBuilder()
        .addPrices(build)
        .build();
  }

  public boolean removePrice(long s, long b) {
    List<MarketPrice> pricesList = this.priceList.getPricesList();
    Iterator<MarketPrice> iterator = pricesList.iterator();
    boolean found = false;
    while (!found && iterator.hasNext()){
      MarketPrice next = iterator.next();
      if(next.getSellTokenQuantity() == s && next.getBuyTokenQuantity() == b){
        found = true;
        iterator.remove();
      }
    }

    this.priceList = this.priceList.toBuilder()
        .clearPrices()
        .addAllPrices(pricesList)
        .build();

    return found;
  }

  // update bestPrice, set the next
  // and should remove bestPrice from store after this
  // 1. delete bestPrice from store
  // 2. if best.next == empty, set priceList.best = empty, update priceList, delete pairToPriceStore
  // 3. else set priceList.best = best.next, update priceList
  public MarketPrice deleteBestPrice(MarketPrice bestPrice, byte[] pairPriceKey,
      MarketPriceStore marketPriceStore, byte[] makerPair, MarketPairToPriceStore pairToPriceStore)
      throws ItemNotFoundException {

    // delete price from store
    marketPriceStore.delete(pairPriceKey);

    MarketPrice nextPrice = null;
    // update makerPriceListCapsule, and need to delete from pairToPriceStore,
    if (bestPrice.getNext() == null) {
      // set empty and delete from pairToPriceStore
      this.priceList = this.priceList.toBuilder()
          .setBestPrice(new MarketPriceCapsule().getInstance()).build();
      pairToPriceStore.delete(makerPair);
    } else {
      try {
        // node.val = node.next.val
        // node.next = node.next.next
        nextPrice = marketPriceStore.get(bestPrice.getNext().toByteArray()).getInstance();
        bestPrice = new MarketPriceCapsule(nextPrice).getInstance();
        this.priceList = this.priceList.toBuilder().setBestPrice(bestPrice).build();

        // check
        pairToPriceStore.put(makerPair, this);
      } catch (ItemNotFoundException e) {
        throw new ItemNotFoundException(e.getMessage());
      }
    }


    return nextPrice;
  }


  public void removeFirst() {
    this.priceList = this.priceList.toBuilder()
        .removePrices(0)
        .build();
  }

  public MarketPrice getBestPrice() {
    return this.priceList.getBestPrice();
  }

  @Override
  public byte[] getData() {
    return this.priceList.toByteArray();
  }

  @Override
  public MarketPriceList getInstance() {
    return this.priceList;
  }

  public boolean isLowerPrice(MarketPrice price1, MarketPrice price2) {
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

  public boolean isSamePrice(MarketPrice price1, MarketPrice price2) {
    return Math.multiplyExact(price1.getBuyTokenQuantity(), price2.getSellTokenQuantity())
        == Math.multiplyExact(price2.getBuyTokenQuantity(), price1.getSellTokenQuantity());
  }
}
