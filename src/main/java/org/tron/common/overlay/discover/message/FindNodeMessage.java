package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.FindNeighbours;

public class FindNodeMessage extends DiscoverMessage {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] data) {
    super(data, MessageTypes.DISCOVER_FIND_PEER);
    unPack();
  }

  @Override
  public byte[] getData() {
    return this.data;
  }

  public FindNodeMessage(ByteString target) {
    this.findNeighbours = FindNeighbours.newBuilder()
        .setTarget(target)
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.findNeighbours.toByteArray();
  }

  private void unPack() {
    try {
      this.findNeighbours = Discover.FindNeighbours.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
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
