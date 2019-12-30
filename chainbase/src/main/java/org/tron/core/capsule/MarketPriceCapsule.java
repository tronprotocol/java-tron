package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.zksnark.MarketUtils;
import org.tron.protos.Protocol.MarketPrice;

@Slf4j(topic = "capsule")
public class MarketPriceCapsule implements ProtoCapsule<MarketPrice> {

  private MarketPrice price;

  public MarketPriceCapsule() {
    this.price = MarketPrice.newBuilder()
        .setSellTokenQuantity(0)
        .setBuyTokenQuantity(0)
        .build();
  }

  public MarketPriceCapsule(final MarketPrice price) {
    this.price = price;
  }

  public MarketPriceCapsule(final byte[] data) {
    try {
      this.price = MarketPrice.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MarketPriceCapsule(long sellTokenQuantity, long buyTokenQuantity) {
    this.price = MarketPrice.newBuilder()
        .setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenQuantity(buyTokenQuantity)
        .setPrev(ByteString.copyFrom(new byte[0]))
        .setNext(ByteString.copyFrom(new byte[0]))
        .build();
  }

  public MarketPriceCapsule(long sellTokenQuantity, long buyTokenQuantity, byte[] next) {
    this.price = MarketPrice.newBuilder()
        .setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenQuantity(buyTokenQuantity)
        .setNext(ByteString.copyFrom(next))
        .build();
  }

  public long getSellTokenQuantity() {
    return this.price.getSellTokenQuantity();
  }

  public void setSellTokenQuantity(long sellTokenQuantity) {
    this.price = this.price.toBuilder()
        .setSellTokenQuantity(sellTokenQuantity)
        .build();
  }

  public long getBuyTokenQuantity() {
    return this.price.getBuyTokenQuantity();
  }

  public void setBuyTokenQuantity(long value) {
    this.price = this.price.toBuilder()
        .setBuyTokenQuantity(value)
        .build();
  }

  public byte[] getNext() {
    return this.price.getNext().toByteArray();
  }

  public void setNext(byte[] next) {
    this.price = this.price.toBuilder()
        .setNext(ByteString.copyFrom(next))
        .build();
  }

  public byte[] getPrev() {
    return this.price.getPrev().toByteArray();
  }

  public void setPrev(byte[] prev) {
    this.price = this.price.toBuilder()
        .setPrev(ByteString.copyFrom(prev))
        .build();
  }

  public byte[] getKey(byte[] sellTokenId, byte[] buyTokenId) {
    if (this.isNull()) {
      return new byte[0];
    }

    return MarketUtils.createPairPriceKey(
        sellTokenId,
        buyTokenId,
        this.getSellTokenQuantity(),
        this.getBuyTokenQuantity()
    );
  }

  public boolean isNull () {
    return this.getSellTokenQuantity() == 0 && this.getBuyTokenQuantity() == 0;
  }

  public boolean isNextNull() {
    return this.getNext() == null || (this.getNext().length == 0);
  }

  public boolean isPrevNull() {
    return this.getPrev() == null || (this.getPrev().length == 0);
  }

  @Override
  public byte[] getData() {
    return this.price.toByteArray();
  }

  @Override
  public MarketPrice getInstance() {
    return this.price;
  }



}
