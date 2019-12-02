package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Hash;
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


  public List<ByteString> getOrdersList() {
    return this.accountOrder.getOrdersList();
  }

  public void addOrders(ByteString order) {
    this.accountOrder = this.accountOrder.toBuilder()
        .addOrders(order)
//        .setCount(accountOrder.getCount() + 1)
        .build();

  }

  public void removeOrders(ByteString order) {
    List<ByteString> ordersList = this.accountOrder.getOrdersList();
    ordersList.remove(order);

    this.accountOrder = this.accountOrder.toBuilder()
        .clearOrders()
        .addAllOrders(ordersList)
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


  @Override
  public byte[] getData() {
    return this.accountOrder.toByteArray();
  }

  @Override
  public MarketAccountOrder getInstance() {
    return this.accountOrder;
  }

}
