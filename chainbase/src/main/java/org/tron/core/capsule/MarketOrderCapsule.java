package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.MarketOrderStore;
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
        .setPrev(ByteString.copyFrom(new byte[0]))
        .setNext(ByteString.copyFrom(new byte[0]))
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

  public long getSellTokenQuantityReturn() {
    return this.order.getSellTokenQuantityReturn();
  }

  public void setSellTokenQuantityReturn() {
    this.order = this.order.toBuilder()
        .setSellTokenQuantityReturn(this.order.getSellTokenQuantityRemain())
        .build();
  }

  public void setSellTokenQuantityReturn(long sellTokenQuantityReturn) {
    this.order = this.order.toBuilder()
        .setSellTokenQuantityReturn(sellTokenQuantityReturn)
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

  public byte[] getNext() {
    return this.order.getNext().toByteArray();
  }

  public void setNext(byte[] next) {
    this.order = this.order.toBuilder()
        .setNext(ByteString.copyFrom(next))
        .build();
  }

  public byte[] getPrev() {
    return this.order.getPrev().toByteArray();
  }

  public void setPrev(byte[] prev) {
    this.order = this.order.toBuilder()
        .setPrev(ByteString.copyFrom(prev))
        .build();
  }

  public boolean isPreNull() {
    return this.getPrev() == null || (this.getPrev().length == 0);
  }

  public boolean isNextNull() {
    return this.getNext() == null || (this.getNext().length == 0);
  }

  public MarketOrderCapsule getPrevCapsule(MarketOrderStore orderStore) throws ItemNotFoundException {
    if (this.isPreNull()) {
      return null;
    } else {
      return orderStore.get(this.getPrev());
    }
  }

  public MarketOrderCapsule getNextCapsule(MarketOrderStore orderStore) throws ItemNotFoundException {
    if (this.isNextNull()) {
      return null;
    } else {
      return orderStore.get(this.getNext());
    }
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
