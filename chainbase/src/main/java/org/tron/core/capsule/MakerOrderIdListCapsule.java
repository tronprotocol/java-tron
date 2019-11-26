package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.MakerOrderIdList;
import org.tron.protos.Protocol.MakerOrderIdList;

@Slf4j(topic = "capsule")
public class MakerOrderIdListCapsule implements ProtoCapsule<MakerOrderIdList> {

  private MakerOrderIdList orderIdList;

  public MakerOrderIdListCapsule(final MakerOrderIdList orderIdList) {
    this.orderIdList = orderIdList;
  }

  public MakerOrderIdListCapsule(final byte[] data) {
    try {
      this.orderIdList = MakerOrderIdList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MakerOrderIdListCapsule( List<ByteString> o) {
    this.orderIdList = MakerOrderIdList.newBuilder()
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
  public MakerOrderIdList getInstance() {
    return this.orderIdList;
  }

}
