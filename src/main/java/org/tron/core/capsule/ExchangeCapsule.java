package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Exchange;
import org.tron.core.capsule.utils.ExchangeProcessor;

@Slf4j
public class ExchangeCapsule implements ProtoCapsule<Exchange> {

  private Exchange exchange;

  private long supply = 1_000_000_000_000_000L;

  public ExchangeCapsule(final Exchange exchange) {
    this.exchange = exchange;
  }

  public ExchangeCapsule(final byte[] data) {
    try {
      this.exchange = Exchange.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ExchangeCapsule(ByteString address, final long id, long createTime,
      byte[] firstTokenID, byte[] secondTokenID) {
    this.exchange = Exchange.newBuilder()
        .setExchangeId(id)
        .setCreatorAddress(address)
        .setCreateTime(createTime)
        .setFirstTokenId(ByteString.copyFrom(firstTokenID))
        .setSecondTokenId(ByteString.copyFrom(secondTokenID))
        .build();
  }

  public long getID() {
    return this.exchange.getExchangeId();
  }

  public void setID(long id) {
    this.exchange = this.exchange.toBuilder()
        .setExchangeId(id)
        .build();
  }

  public ByteString getCreatorAddress() {
    return this.exchange.getCreatorAddress();
  }

  public void setExchangeAddress(ByteString address) {
    this.exchange = this.exchange.toBuilder()
        .setCreatorAddress(address)
        .build();
  }

  public void setBalance(long firstTokenBalance, long secondTokenBalance) {
    this.exchange = this.exchange.toBuilder()
        .setFirstTokenBalance(firstTokenBalance)
        .setSecondTokenBalance(secondTokenBalance)
        .build();
  }

  public long getCreateTime() {
    return this.exchange.getCreateTime();
  }

  public void setCreateTime(long time) {
    this.exchange = this.exchange.toBuilder()
        .setCreateTime(time)
        .build();
  }

  public byte[] createDbKey() {
    return calculateDbKey(getID());
  }

  public static byte[] calculateDbKey(long number) {
    return ByteArray.fromLong(number);
  }

  public long transaction(byte[] sellTokenID, long sellTokenQuant) {
    ExchangeProcessor processor = new ExchangeProcessor(this.supply);

    long buyTokenQuant = 0;
    long firstTokenBalance = this.exchange.getFirstTokenBalance();
    long secondTokenBalance = this.exchange.getSecondTokenBalance();

    if (this.exchange.getFirstTokenId().equals(ByteString.copyFrom(sellTokenID))) {
      buyTokenQuant = processor.exchange(firstTokenBalance,
          secondTokenBalance,
          sellTokenQuant);
      this.exchange = this.exchange.toBuilder()
          .setFirstTokenBalance(firstTokenBalance - sellTokenQuant)
          .setSecondTokenBalance(secondTokenBalance + buyTokenQuant)
          .build();
    } else {
      buyTokenQuant = processor.exchange(secondTokenBalance,
          firstTokenBalance,
          sellTokenQuant);
      this.exchange = this.exchange.toBuilder()
          .setFirstTokenBalance(firstTokenBalance + buyTokenQuant)
          .setSecondTokenBalance(secondTokenBalance - sellTokenQuant)
          .build();
    }

    return buyTokenQuant;
  }

  @Override
  public byte[] getData() {
    return this.exchange.toByteArray();
  }

  @Override
  public Exchange getInstance() {
    return this.exchange;
  }

}
