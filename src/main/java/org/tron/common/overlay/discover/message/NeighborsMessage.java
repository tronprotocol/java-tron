package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message;

public class NeighborsMessage extends DiscoverMessage {

  private Message.Neighbours neighbours;

  public NeighborsMessage(byte[] data) {
    super(data);
  }

  private void unPack() {
    try {
      this.neighbours = Message.Neighbours.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.neighbours.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  @Override
  public String toString() {

    String out = String
        .format("[NeighborsMessage] \n");

    return out;
  }

  @Override
  public MessageTypes getType() {
    return null;
  }
}
