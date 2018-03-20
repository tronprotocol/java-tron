package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Discover;
import org.tron.protos.Discover.FindNeighbours;

public class FindNodeMessage extends Message {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] data) {
    super(data, Message.TYPE_FIND_PEER);
    unPack();
  }

  public FindNodeMessage(ByteString target) {
    this.data = this.findNeighbours.toByteArray();
    this.findNeighbours = FindNeighbours.newBuilder()
        .setTarget(target)
        .setTimestamp(System.currentTimeMillis())
        .build();
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

}
