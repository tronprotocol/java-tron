package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.MakerPriceList;
import org.tron.protos.Protocol.MakerPriceList;

@Slf4j(topic = "capsule")
public class MakerPriceListCapsule implements ProtoCapsule<MakerPriceList> {

  private MakerPriceList priceList;

  public MakerPriceListCapsule(final MakerPriceList priceList) {
    this.priceList = priceList;
  }

  public MakerPriceListCapsule(final byte[] data) {
    try {
      this.priceList = MakerPriceList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public MakerPriceListCapsule(byte[] sellTokenId, byte[] buyTokenId, List<ByteString> p) {
    this.priceList = MakerPriceList.newBuilder()
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .addAllPrices(p)
        .build();
  }

  public byte[] getSellTokenId() {
    return this.priceList.getSellTokenId().toByteArray();
  }

  public void setSellTokenId(byte[] id) {
    this.priceList = this.priceList.toBuilder()
        .setSellTokenId(ByteString.copyFrom(id))
        .build();
  }


  public byte[] getBuyTokenId() {
    return this.priceList.getBuyTokenId().toByteArray();
  }

  public void setBuyTokenId(byte[] id) {
    this.priceList = this.priceList.toBuilder()
        .setBuyTokenId(ByteString.copyFrom(id))
        .build();
  }

  public List<ByteString> getPricesList() {
    return this.priceList.getPricesList();
  }

  public void addPrices(ByteString p) {
    this.priceList = this.priceList.toBuilder()
        .addPrices(p)
        .build();
  }

  public void removePrice(ByteString p) {
    List<ByteString> pricesList = this.priceList.getPricesList();
    pricesList.remove(p);

    this.priceList = this.priceList.toBuilder()
        .clearPrices()
        .addAllPrices(pricesList)
        .build();
  }


  @Override
  public byte[] getData() {
    return this.priceList.toByteArray();
  }

  @Override
  public MakerPriceList getInstance() {
    return this.priceList;
  }

}
