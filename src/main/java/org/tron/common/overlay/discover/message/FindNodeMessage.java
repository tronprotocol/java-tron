package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Discover;

public class FindNodeMessage extends Message {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] data) {
    super(data);
    unPack();
  }

  private void unPack() {
    try {
      this.findNeighbours = Discover.FindNeighbours.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.findNeighbours.toByteArray();
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

    long currTime = System.currentTimeMillis() / 1000;

    String out = String.format("[FindNodeMessage] \n");

    return out;
  }

  @Override
  public byte getType() {
    return this.type;
  }

}
