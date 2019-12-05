package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Hash;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.MarketOrder;

import java.util.Arrays;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;

@Slf4j(topic = "capsule")
public class MarketOrderCapsule implements ProtoCapsule<MarketOrder> {

  private MarketOrder order;

  public MarketOrderCapsule(final MarketOrder order) {
    this.order = order;
  }

  public MarketOrderCapsule(final byte[] data) {
    try {
      this.order = MarketOrder.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MarketOrderCapsule(byte[] id, MarketSellAssetContract contract) {

    this.order = MarketOrder.newBuilder()
        .setOrderId(ByteString.copyFrom(id))
        .setOwnerAddress(contract.getOwnerAddress())
        .setSellTokenId(contract.getSellTokenId())
        .setSellTokenQuantity(contract.getSellTokenQuantity())
        .setBuyTokenId(contract.getBuyTokenId())
        .setBuyTokenQuantity(contract.getBuyTokenQuantity())
        .setSellTokenQuantityRemain(contract.getSellTokenQuantity())
        .setState(State.ACTIVE)
        .build();
  }


  public MarketOrderCapsule(final byte[] id, ByteString address, long createTime,
      byte[] sellTokenId, long sellTokenQuantity,
      byte[] buyTokenId, long buyTokenQuantity, long sellTokenQuantityRemain) {
    this.order = MarketOrder.newBuilder()
        .setOrderId(ByteString.copyFrom(id))
        .setOwnerAddress(address)
        .setCreateTime(createTime)
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .setBuyTokenQuantity(buyTokenQuantity)
        .setSellTokenQuantityRemain(sellTokenQuantityRemain)
        .build();
  }



  public ByteString getID() {
    return this.order.getOrderId();
  }

  public void setID(ByteString id) {
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

  public void setBuyTokenQuantity(long value) {
    this.order = this.order.toBuilder()
        .setBuyTokenQuantity(value)
        .build();
  }


  public MarketOrder.State getSt() {
    return this.order.getState();
  }

  public void setState(MarketOrder.State value) {
    this.order = this.order.toBuilder()
        .setState(value)
        .build();
  }

  public boolean isActive(){
    return this.order.getState() == State.ACTIVE;
  }


  @Override
  public byte[] getData() {
    return this.order.toByteArray();
  }

  @Override
  public MarketOrder getInstance() {
    return this.order;
  }

}
