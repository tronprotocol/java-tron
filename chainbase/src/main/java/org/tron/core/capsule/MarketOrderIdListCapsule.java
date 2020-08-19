package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.MarketOrderStore;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.protos.Protocol.MarketOrderIdList;

@Slf4j(topic = "capsule")
public class MarketOrderIdListCapsule implements ProtoCapsule<MarketOrderIdList> {

  private MarketOrderIdList orderIdList;

  public MarketOrderIdListCapsule(final MarketOrderIdList orderIdList) {
    this.orderIdList = orderIdList;
  }

  public MarketOrderIdListCapsule(final byte[] data) {
    try {
      this.orderIdList = MarketOrderIdList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MarketOrderIdListCapsule() {
    this.orderIdList = MarketOrderIdList.newBuilder()
        .setHead(ByteString.copyFrom(new byte[0]))
        .setTail(ByteString.copyFrom(new byte[0]))
        .build();
  }

  // just for test
  public MarketOrderIdListCapsule(byte[] head, byte[] tail) {
    this.orderIdList = MarketOrderIdList.newBuilder()
        .setHead(ByteString.copyFrom(head))
        .setTail(ByteString.copyFrom(tail))
        .build();
  }

  public boolean isOrderExists(byte[] orderId, MarketOrderStore orderStore)
      throws ItemNotFoundException {
    if (orderId.length == 0) {
      return false;
    }

    if (this.isOrderEmpty()) {
      return false;
    }

    boolean found = false;
    byte[] currentOrderId = this.getHead();

    while (currentOrderId.length != 0) {
      if (Arrays.equals(orderId, currentOrderId)) {
        found = true;
        break;
      }

      MarketOrderCapsule currentCapsule = orderStore.get(orderId);
      if (!currentCapsule.isNextNull()) {
        currentOrderId = currentCapsule.getNext();
      } else {
        break;
      }
    }

    return found;
  }

  // need to delete when all empty
  public void removeOrder(MarketOrderCapsule currentCapsule, MarketOrderStore marketOrderStore,
      byte[] pairPriceKey, MarketPairPriceToOrderStore pairPriceToOrderStore)
      throws ItemNotFoundException {
    MarketOrderCapsule preCapsule = currentCapsule.getPrevCapsule(marketOrderStore);
    MarketOrderCapsule nextCapsule = currentCapsule.getNextCapsule(marketOrderStore);

    // pre.next = current.next
    // current.next.prev = current.prev
    if (preCapsule != null) {
      if (nextCapsule != null) {
        preCapsule.setNext(currentCapsule.getNext());
      } else {
        preCapsule.setNext(new byte[0]);
      }

      marketOrderStore.put(preCapsule.getID().toByteArray(), preCapsule);
    } else {
      // current is head
      // head = current.next
      if (nextCapsule != null) {
        this.setHead(currentCapsule.getNext());
      } else {
        // need to delete, outside
        this.setHead(new byte[0]);
      }

      // head changed
      pairPriceToOrderStore.put(pairPriceKey, this);
    }

    if (nextCapsule != null) {
      if (preCapsule != null) {
        nextCapsule.setPrev(currentCapsule.getPrev());
      } else {
        nextCapsule.setPrev(new byte[0]);
      }

      marketOrderStore.put(nextCapsule.getID().toByteArray(), nextCapsule);
    } else {
      // current is tail
      // this.tail = pre
      if (preCapsule != null) {
        this.setTail(currentCapsule.getPrev());
      } else {
        this.setTail(new byte[0]);
      }

      // tail changed
      pairPriceToOrderStore.put(pairPriceKey, this);
    }

    // update current
    currentCapsule.setPrev(new byte[0]);
    currentCapsule.setNext(new byte[0]);
    marketOrderStore.put(currentCapsule.getID().toByteArray(), currentCapsule);
  }

  public void setHead(byte[] head) {
    this.orderIdList = this.orderIdList.toBuilder()
        .setHead(ByteString.copyFrom(head))
        .build();
  }

  public byte[] getHead() {
    return this.orderIdList.getHead().toByteArray();
  }

  public byte[] getTail() {
    return this.orderIdList.getTail().toByteArray();
  }

  public void setTail(byte[] tail) {
    this.orderIdList = this.orderIdList.toBuilder()
        .setTail(ByteString.copyFrom(tail))
        .build();
  }

  public boolean isOrderEmpty() {
    return this.getHead() == null || this.getHead().length == 0;
  }

  // add order to linked list
  public void addOrder(MarketOrderCapsule currentCapsule, MarketOrderStore orderStore)
      throws ItemNotFoundException {
    byte[] orderId = currentCapsule.getID().toByteArray();

    if (this.isOrderEmpty()) {
      this.setHead(orderId);
      this.setTail(orderId);
    } else {
      // tail.next = order
      // order.pre = tail
      // this.tail = order
      byte[] tailId = this.getTail();
      MarketOrderCapsule tailCapsule = orderStore.get(tailId);
      tailCapsule.setNext(orderId);
      orderStore.put(tailId, tailCapsule);

      currentCapsule.setPrev(tailId);
      orderStore.put(orderId, currentCapsule);

      this.setTail(orderId);
    }
  }

  public MarketOrderCapsule getHeadOrder(MarketOrderStore marketOrderStore)
      throws ItemNotFoundException {
    if (this.isOrderEmpty()) {
      return null;
    }

    return marketOrderStore.get(this.getHead());
  }

  // just for test
  public MarketOrderCapsule getOrderByIndex(int index, MarketOrderStore marketOrderStore)
      throws ItemNotFoundException {
    if (this.isOrderEmpty()) {
      return null;
    }

    MarketOrderCapsule current = this.getHeadOrder(marketOrderStore);

    int count = 0;
    while (current != null) {
      if (count == index) {
        return current;
      }
      count++;

      if (current.isNextNull()) {
        return null;
      }
      current = marketOrderStore.get(current.getNext());
    }

    return null;
  }

  // just for test
  public int getOrderSize(MarketOrderStore marketOrderStore) throws ItemNotFoundException {
    if (this.isOrderEmpty()) {
      return 0;
    }

    MarketOrderCapsule head = marketOrderStore.get(this.getHead());

    int size = 1;
    while (!head.isNextNull()) {
      size++;
      head = marketOrderStore.get(head.getNext());
    }

    return size;
  }

  @Override
  public byte[] getData() {
    return this.orderIdList.toByteArray();
  }

  @Override
  public MarketOrderIdList getInstance() {
    return this.orderIdList;
  }

  public List<MarketOrderCapsule> getAllOrder(MarketOrderStore orderStore, long limit)
      throws ItemNotFoundException {

    List<MarketOrderCapsule> result = new ArrayList<>();

    long count = 0;
    byte[] orderId = this.getHead();
    if (!ByteArray.isEmpty(orderId)) {
      MarketOrderCapsule makerOrderCapsule = orderStore.getUnchecked(orderId);
      while (makerOrderCapsule != null) {
        result.add(makerOrderCapsule);
        makerOrderCapsule = makerOrderCapsule.getNextCapsule(orderStore);
        count++;
        if (count > limit) {
          break;
        }
      }
    }
    return result;
  }


}
