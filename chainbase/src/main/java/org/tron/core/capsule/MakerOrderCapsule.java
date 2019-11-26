package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Hash;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.MakerOrder;

import java.util.Arrays;

@Slf4j(topic = "capsule")
public class MakerOrderCapsule implements ProtoCapsule<MakerOrder> {

  private MakerOrder order;

  public MakerOrderCapsule(final MakerOrder order) {
    this.order = order;
  }

  public MakerOrderCapsule(final byte[] data) {
    try {
      this.order = MakerOrder.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MakerOrderCapsule(final long id, ByteString address, long createTime,
      byte[] sellTokenId, long sellTokenRatio,
      byte[] buyTokenId, long buyTokenRatio,
      long sellTokenQuantity, long sellTokenQuantityRemain) {
    this.order = MakerOrder.newBuilder()
        .setOrderId(id)
        .setOwnerAddress(address)
        .setCreateTime(createTime)
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setSellTokenQuantity(sellTokenRatio)
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .setBuyTokenQuantity(buyTokenRatio)
        .setSellTokenQuantityRemain(sellTokenQuantityRemain)
        .build();
  }

  public static byte[] calculateOrderId(ByteString address, byte[] sellTokenId,
      byte[] buyTokenId, long count) {

    byte[] addressByteArray = address.toByteArray();
    byte[] countByteArray = ByteArray.fromLong(count);

    byte[] result = new byte[addressByteArray.length + sellTokenId.length
        + buyTokenId.length + countByteArray.length];

    System.arraycopy(addressByteArray, 0, result, 0, addressByteArray.length);
    System.arraycopy(sellTokenId, 0, result, addressByteArray.length, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, addressByteArray.length + sellTokenId.length,
        buyTokenId.length);
    System.arraycopy(countByteArray, 0, result, addressByteArray.length
        + sellTokenId.length + buyTokenId.length, countByteArray.length);

//    return Hash.sha3(result);
    return result;
  }

  public long getID() {
    return this.order.getOrderId();
  }

  public void setID(long id) {
    this.order = this.order.toBuilder()
        .setOrderId(id)
        .build();
  }

  public ByteString getOwnerAddress() {
    return this.order.getOwnerAddress();
  }

  public void setOwnerAddress(ByteString address) {
    this.order = this.order.toBuilder()
        .setOwnerAddress(address)
        .build();
  }


  public long getCreateTime() {
    return this.order.getCreateTime();
  }

  public void setCreateTime(long time) {
    this.order = this.order.toBuilder()
        .setCreateTime(time)
        .build();
  }

  public byte[] getSellTokenId() {
    return this.order.getSellTokenId().toByteArray();
  }

  public void setSellTokenId(byte[] id) {
    this.order = this.order.toBuilder()
        .setSellTokenId(ByteString.copyFrom(id))
        .build();
  }


  public long getSellTokenQuantity() {
    return this.order.getSellTokenQuantity();
  }

  public void setSellTokenQuantity(long sellTokenQuantity) {
    this.order = this.order.toBuilder()
        .setSellTokenQuantity(sellTokenQuantity)
        .build();
  }


  public long getSellTokenQuantityRemain() {
    return this.order.getSellTokenQuantityRemain();
  }

  public void setSellTokenQuantityRemain(long sellTokenQuantityRemain) {
    this.order = this.order.toBuilder()
        .setSellTokenQuantityRemain(sellTokenQuantityRemain)
        .build();
  }

  public byte[] getBuyTokenId() {
    return this.order.getBuyTokenId().toByteArray();
  }

  public void setBuyTokenId(byte[] id) {
    this.order = this.order.toBuilder()
        .setBuyTokenId(ByteString.copyFrom(id))
        .build();
  }

  public long getBuyTokenQuantity() {
    return this.order.getBuyTokenQuantity();
  }

  public void setBuyTokenQuantity(long buyTokenRatio) {
    this.order = this.order.toBuilder()
        .setBuyTokenQuantity(buyTokenRatio)
        .build();
  }


  @Override
  public byte[] getData() {
    return this.order.toByteArray();
  }

  @Override
  public MakerOrder getInstance() {
    return this.order;
  }

}
