package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

  public MarketOrderIdListCapsule( List<ByteString> o) {
    this.orderIdList = MarketOrderIdList.newBuilder()
        .addAllOrders(o)
        .build();
  }

  public List<ByteString> getOrdersList() {
    return this.orderIdList.getOrdersList();
  }

  public void addOrders(ByteString v) {
    this.orderIdList = this.orderIdList.toBuilder()
        .addOrders(v)
        .build();
  }

  public void removePrice(ByteString v) {
    List<ByteString> orderList = this.orderIdList.getOrdersList();
    orderList.remove(v);

    this.orderIdList = this.orderIdList.toBuilder()
        .clearOrders()
        .addAllOrders(orderList)
        .build();
  }


  @Override
  public byte[] getData() {
    return this.orderIdList.toByteArray();
  }

  @Override
  public MarketOrderIdList getInstance() {
    return this.orderIdList;
  }

}
