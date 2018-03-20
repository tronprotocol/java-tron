package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.FindNeighbours;

public class FindNodeMessage extends DiscoverMessage {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] rawData) {
    super(MessageTypes.DISCOVER_FIND_PEER.asByte(), rawData);
    unPack();
  }

  @Override
  public byte[] getRawData() {
    return this.rawData;
  }

  public create(byte[] targetId) {
//    this.findNeighbours = FindNeighbours.newBuilder()
//        .setTarget(target)
//        .setTimestamp(System.currentTimeMillis())
//        .build();
//    this.rawData = this.findNeighbours.toByteArray();
  }

  private void unPack() {
    try {
      this.findNeighbours = Discover.FindNeighbours.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public byte[] getTargetId(){
      return null;
  }

  @Override
  public String toString() {

    return String.format("[FindNodeMessage] \n");

  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}
