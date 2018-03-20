package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Neighbour;
import org.tron.protos.Discover.Neighbours;

public class NeighborsMessage extends Message {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] data) {
    super(data);
    unPack();
  }

  public NeighborsMessage(List<Neighbour> neighbours, long timestamp) {
    this.neighbours = Neighbours.newBuilder()
        .addAllNeighbours(neighbours)
        .setTimestamp(timestamp)
        .build();
    pack();
  }

  private void unPack() {
    try {
      this.neighbours = Discover.Neighbours.parseFrom(data);
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
  public byte getType() {
    return this.type;
  }
}
