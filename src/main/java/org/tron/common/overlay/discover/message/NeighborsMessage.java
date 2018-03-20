package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Discover.Neighbour;
import org.tron.protos.Discover.Neighbours;
import org.tron.protos.Discover.Neighbours.Builder;

public class NeighborsMessage extends Message {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] data) {
    super(data, Message.TYPE_PEERS);
    unPack();
  }

  public NeighborsMessage(List<Node> neighbours) {
    Builder builder = Neighbours.newBuilder()
        .setTimestamp(System.currentTimeMillis());

    neighbours.forEach(neighbour -> {
      Endpoint endpoint = Endpoint.newBuilder()
          .setAddress(ByteString.copyFrom(ByteArray.fromString(neighbour.getHost())))
          .setTcpPort(neighbour.getPort())
          .setUdpPort(neighbour.getPort())
          .build();

      Neighbour ne = Neighbour.newBuilder()
          .setEndpoint(endpoint)
          .setNodeId(ByteString.copyFrom(neighbour.getId()))
          .build();

      builder.addNeighbours(ne);
    });

    this.neighbours = builder.build();

    this.data = this.neighbours.toByteArray();
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
                ByteArray.toStr(neighbour.getEndpoint().getAddress().toByteArray()),
                neighbour.getEndpoint().getTcpPort())));

    return nodes;
  }

  public Neighbours getNeighbours() {
    return neighbours;
  }

  @Override
  public String toString() {
    return String.format("[NeighborsMessage] \n");
  }

}
