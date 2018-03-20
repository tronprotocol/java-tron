package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.overlay.discover.Node;
import java.util.List;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Neighbour;
import org.tron.protos.Discover.Neighbours;

import java.util.ArrayList;
import java.util.List;

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

  public List<Node> getNodes(){
    List<Node> nodes = new ArrayList<>();
    neighbours.getNeighboursList().forEach(neighbour -> nodes.add(
            new Node(neighbour.getNodeId().toByteArray(),
                    neighbour.getEndpoint().getAddress().toString(),
                    neighbour.getEndpoint())));
  }

  public Neighbours getNeighbours() {
    return neighbours;
  }

  @Override
  public String toString() {
    return String.format("[NeighborsMessage] \n");
  }

}
