package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.core.store.MarketPriceStore;
import org.tron.protos.Protocol.MarketOrderPosition;
import org.tron.protos.Protocol.MarketPriceLinkedList;
import org.tron.protos.Protocol.MarketPrice;

@Slf4j(topic = "capsule")
public class MarketPriceLinkedListCapsule implements ProtoCapsule<MarketPriceLinkedList> {

  private MarketPriceLinkedList priceList;

  public MarketPriceLinkedListCapsule(final MarketPriceLinkedList priceList) {
    this.priceList = priceList;
  }

  public MarketPriceLinkedListCapsule(final byte[] data) {
    try {
      this.priceList = MarketPriceLinkedList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MarketPriceLinkedListCapsule(byte[] sellTokenId, byte[] buyTokenId) {
    this.priceList = MarketPriceLinkedList.newBuilder()
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

  //for test only
  public int getPriceSize(MarketPriceStore marketPriceStore) throws ItemNotFoundException {
    MarketPriceCapsule head = new MarketPriceCapsule(this.getBestPrice());

    if (head.isNull()) {
      return 0;
    }

    int size = 1;
    while (!head.isNextNull()) {
      size++;
      head = marketPriceStore.get(head.getNext());
    }
    return size;
  }

  public MarketPriceCapsule getPriceByIndex(int index, MarketPriceStore marketPriceStore)
      throws ItemNotFoundException {
    MarketPriceCapsule current = new MarketPriceCapsule(this.getBestPrice());

    int count = 0;
    while (!current.isNull()) {
      if (count == index) {
        return current;
      }
      count++;

      if (current.isNextNull()) {
        return null;
      }
      current = marketPriceStore.get(current.getNext());
    }

    return null;
  }

  public List<MarketPrice> getPricesList(MarketPriceStore marketPriceStore)
      throws ItemNotFoundException {
    List<MarketPrice> marketPrices = new ArrayList<>();
    MarketPriceCapsule current = new MarketPriceCapsule(this.getBestPrice());

    while (!current.isNull()) {
      marketPrices.add(current.getInstance());

      if (!current.isNextNull()) {
        current = marketPriceStore.get(current.getNext());
      } else {
        break;
      }
    }

    return marketPrices;
  }


  public void setBestPrice(MarketPriceCapsule bestPrice) {
    this.priceList = this.priceList.toBuilder().setBestPrice(bestPrice.getInstance()).build();
  }


  /*
   * insert price by sort, if same, just return
   * store ops outside(itself)
   * return head, null if not changed
   * */
  public MarketPriceCapsule insertMarket(MarketPrice marketPrice, byte[] sellTokenID,
      byte[] buyTokenID, MarketPriceStore marketPriceStore, MarketOrderPosition position)
      throws ItemNotFoundException {

    MarketPriceCapsule head;
    //get the start position
    if (position.getPrePriceKey().isEmpty()) {
      head = new MarketPriceCapsule(this.getBestPrice());
    } else {
      head = marketPriceStore.get(position.getPrePriceKey().toByteArray());
    }

    // dummy.next = head
    MarketPriceCapsule dummy = new MarketPriceCapsule(0, 0);
    if (!head.isNull()) {
      dummy.setNext(head.getKey(sellTokenID, buyTokenID));
    }

    head = dummy;

    boolean found = false;
    while (!head.isNextNull()) {
      if (MarketUtils
          .isLowerPrice(marketPriceStore.get(head.getNext()).getInstance(), marketPrice)) {
        head = marketPriceStore.get(head.getNext());
      } else {
        if (MarketUtils
            .isSamePrice(marketPriceStore.get(head.getNext()).getInstance(), marketPrice)) {
          found = true;
        }
        break;
      }
    }

    if (!found) {
      // node.next = head.next
      // node.prev = head
      marketPrice = marketPrice.toBuilder()
          .setNext(ByteString.copyFrom(head.getNext()))
          .setPrev(ByteString.copyFrom(head.getKey(sellTokenID, buyTokenID)))
          .build();

      MarketPriceCapsule marketPriceCapsule = new MarketPriceCapsule(marketPrice);
      byte[] priceKey = marketPriceCapsule.getKey(sellTokenID, buyTokenID);

      // head.next.pre = node
      if (!head.isNextNull()) {
        MarketPriceCapsule next = marketPriceStore.get(head.getNext());
        next.setPrev(priceKey);
        marketPriceStore.put(next.getKey(sellTokenID, buyTokenID), next);
      }

      // head.next = node
      head.setNext(priceKey);
      marketPriceStore.put(head.getKey(sellTokenID, buyTokenID), head);

      marketPriceStore.put(priceKey, marketPriceCapsule);

      // dummy.next
      return marketPriceStore.get(dummy.getNext());
    }

    return null;
  }

  /*
   * delete current price, including head and other node
   * */
  public MarketPrice deleteCurrentPrice(MarketPrice currentPrice, byte[] pairPriceKey,
      MarketPriceStore marketPriceStore, byte[] makerPair, MarketPairToPriceStore pairToPriceStore)
      throws ItemNotFoundException {

    // delete price from store
    marketPriceStore.delete(pairPriceKey);

    MarketPrice nextPrice = null;
    MarketPriceCapsule currentPriceCapsule = new MarketPriceCapsule(currentPrice);

    if (currentPriceCapsule.isNextNull()) {
      if (currentPriceCapsule.isPrevNull()) {
        // set empty and delete from pairToPriceStore
        this.priceList = this.priceList.toBuilder()
            .setBestPrice(new MarketPriceCapsule().getInstance()).build();
        pairToPriceStore.delete(makerPair);
      } else {
        // current.pre.next = null
        MarketPriceCapsule prePriceCapsule = marketPriceStore
            .get(currentPrice.getPrev().toByteArray());
        prePriceCapsule.setNext(new byte[0]);
        marketPriceStore.put(prePriceCapsule.getKey(this.getSellTokenId(), this.getBuyTokenId()),
            prePriceCapsule);
      }
    } else {
      try {
        // node.val = node.next.val
        // node.next = node.next.next
        // node.next.next.pre = node.pre
        MarketPriceCapsule nextPriceCapsule = marketPriceStore
            .get(currentPrice.getNext().toByteArray());
        nextPriceCapsule.setPrev(currentPrice.getPrev().toByteArray());
        byte[] nextPriceKey = nextPriceCapsule.getKey(this.getSellTokenId(), this.getBuyTokenId());
        marketPriceStore.put(nextPriceKey, nextPriceCapsule);

        // check if first
        if (currentPriceCapsule.isPrevNull()) {
          nextPrice = nextPriceCapsule.getInstance();
          this.priceList = this.priceList.toBuilder().setBestPrice(nextPrice).build();
        } else {
          MarketPriceCapsule prePriceCapsule = marketPriceStore
              .get(currentPrice.getPrev().toByteArray());
          prePriceCapsule.setNext(nextPriceKey);
          marketPriceStore.put(prePriceCapsule.getKey(this.getSellTokenId(), this.getBuyTokenId()),
              prePriceCapsule);

          // update pre to preListCapsule, because itself has changed
          if (prePriceCapsule.isPrevNull()) {
            this.priceList = this.priceList.toBuilder().setBestPrice(prePriceCapsule.getInstance())
                .build();
          }
        }

        pairToPriceStore.put(makerPair, this);
      } catch (ItemNotFoundException e) {
        throw new ItemNotFoundException(e.getMessage());
      }
    }

    return nextPrice;
  }

  public MarketPrice getBestPrice() {
    return this.priceList.getBestPrice();
  }

  @Override
  public byte[] getData() {
    return this.priceList.toByteArray();
  }

  @Override
  public MarketPriceLinkedList getInstance() {
    return this.priceList;
  }

}
