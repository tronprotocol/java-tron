package org.tron.core.capsule;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.MarketAccountOrder;

@Slf4j(topic = "capsule")
public class MarketAccountOrderCapsule implements ProtoCapsule<MarketAccountOrder> {

  private MarketAccountOrder accountOrder;

  public MarketAccountOrderCapsule(final MarketAccountOrder accountOrder) {
    this.accountOrder = accountOrder;
  }

  public MarketAccountOrderCapsule(final byte[] data) {
    try {
      this.accountOrder = MarketAccountOrder.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MarketAccountOrderCapsule(ByteString address) {
    this.accountOrder = MarketAccountOrder.newBuilder()
        .setOwnerAddress(address)
        .build();
  }

  public MarketAccountOrderCapsule(ByteString address,
      List<ByteString> orders, long count) {
    this.accountOrder = MarketAccountOrder.newBuilder()
        .setOwnerAddress(address)
        .addAllOrders(orders)
        .setCount(count)
        .build();
  }


  public ByteString getOwnerAddress() {
    return this.accountOrder.getOwnerAddress();
  }

  public byte[] createDbKey() {
    return getOwnerAddress().toByteArray();
  }

  public List<ByteString> getOrdersList() {
    return this.accountOrder.getOrdersList();
  }

  public void addOrders(ByteString order) {
    this.accountOrder = this.accountOrder.toBuilder()
        .addOrders(order)
        .build();

  }

  public void removeOrder(ByteString orderId) {
    List<ByteString> orderList = Lists.newArrayList();
    orderList.addAll(this.getOrdersList());
    orderList.remove(orderId);

    this.accountOrder = this.accountOrder.toBuilder()
        .setCount(this.getCount() - 1)
        .clearOrders()
        .addAllOrders(orderList)
        .build();


  }

  public void setCount(long o) {
    this.accountOrder = this.accountOrder.toBuilder()
        .setCount(o)
        .build();
  }

  public long getCount() {
    return this.accountOrder.getCount();
  }

  public void setOwnerAddress(long count) {
    this.accountOrder = this.accountOrder.toBuilder()
        .setCount(count)
        .build();
  }

  public void setTotalCount(long o) {
    this.accountOrder = this.accountOrder.toBuilder()
        .setTotalCount(o)
        .build();
  }

  public long getTotalCount() {
    return this.accountOrder.getTotalCount();
  }


  @Override
  public byte[] getData() {
    return this.accountOrder.toByteArray();
  }

  @Override
  public MarketAccountOrder getInstance() {
    return this.accountOrder;
  }

}
