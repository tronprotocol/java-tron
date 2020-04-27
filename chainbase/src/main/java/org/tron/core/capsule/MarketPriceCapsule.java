package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.utils.MarketUtils;
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

  @Override
  public byte[] getData() {
    return this.price.toByteArray();
  }

  @Override
  public MarketPrice getInstance() {
    return this.price;
  }



}
