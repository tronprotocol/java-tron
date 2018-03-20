package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Discover;

public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) {
    super(data);
    unPack();
  }

  private void unPack() {
    try {
      this.pongMessage = Discover.PongMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.pongMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  @Override
  public byte getType() {
    return this.type;
  }

  @Override
  public String toString() {

    String out = String.format("[PongMessage]\n");

    return out;
  }
}
