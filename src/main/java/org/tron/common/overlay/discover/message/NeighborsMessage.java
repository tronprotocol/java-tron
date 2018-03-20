package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Neighbour;
import org.tron.protos.Discover.Neighbours;

public class NeighborsMessage extends Message {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] data) {
    super(data, Message.TYPE_PEERS);
    unPack();
  }

  public NeighborsMessage(List<Neighbour> neighbours) {
    this.data = this.neighbours.toByteArray();
    this.neighbours = Neighbours.newBuilder()
        .addAllNeighbours(neighbours)
        .setTimestamp(System.currentTimeMillis())
        .build();
  }

  private void unPack() {
    try {
      this.neighbours = Discover.Neighbours.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public Neighbours getNeighbours() {
    return neighbours;
  }

  @Override
  public String toString() {
    return String.format("[NeighborsMessage] \n");
  }

}
