package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.MakerPriceList;
import org.tron.protos.Protocol.MakerPriceList;
import org.tron.protos.Protocol.MakerPriceList.MakerPrice;

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

  public MakerPriceListCapsule(byte[] sellTokenId, byte[] buyTokenId) {
    this.priceList = MakerPriceList.newBuilder()
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
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

  public List<MakerPrice> getPricesList() {
    return this.priceList.getPricesList();
  }

  public void addPrices(long s, long b) {
    MakerPrice build = MakerPrice.newBuilder().setSellTokenQuantity(s).setBuyTokenQuantity(b)
        .build();
    this.priceList = this.priceList.toBuilder()
        .addPrices(build)
        .build();
  }

  public void removePrice(long s, long b) {
    List<MakerPrice> pricesList = this.priceList.getPricesList();
//    pricesList.remove(p);//todo

    this.priceList = this.priceList.toBuilder()
        .clearPrices()
        .addAllPrices(pricesList)
        .build();
  }

  public void removeFirst() {
    List<MakerPrice> pricesList = this.priceList.getPricesList();
    pricesList.remove(0);

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
