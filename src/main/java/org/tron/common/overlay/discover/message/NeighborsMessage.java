package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Discover.Neighbours;
import org.tron.protos.Discover.Neighbours.Builder;

public class NeighborsMessage extends DiscoverMessage {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] rawData) {
    super(MessageTypes.DISCOVER_PEERS.asByte(), rawData);
    unPack();
  }

  @Override
  public byte[] getRawData() {
    return this.rawData;
  }

  @Override
  public byte[] getNodeId() {
    return this.neighbours.getFrom().getNodeId().toByteArray();
  }

  public NeighborsMessage(Node from, List<Node> neighbours) {
    Builder builder = Neighbours.newBuilder()
        .setTimestamp(System.currentTimeMillis());

    neighbours.forEach(neighbour -> {
      Endpoint endpoint = Endpoint.newBuilder()
          .setAddress(ByteString.copyFrom(ByteArray.fromString(neighbour.getHost())))
          .setPort(neighbour.getPort())
          .setNodeId(ByteString.copyFrom(neighbour.getId()))
          .build();

      builder.addNeighbours(endpoint);
    });

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();

    builder.setFrom(fromEndpoint);

    this.neighbours = builder.build();

    this.type = MessageTypes.DISCOVER_PEERS.asByte();
    this.rawData = this.neighbours.toByteArray();
  }

  private void unPack() {
    try {
      this.neighbours = Discover.Neighbours.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public static NeighborsMessage create(Node from, List<Node> nodes) {
    NeighborsMessage neighborsMessage = new NeighborsMessage(from, nodes);
    return neighborsMessage;
  }

  public List<Node> getNodes(){
    List<Node> nodes = new ArrayList<>();
    neighbours.getNeighboursList().forEach(neighbour -> nodes.add(
            new Node(neighbour.getNodeId().toByteArray(),
                ByteArray.toStr(neighbour.getAddress().toByteArray()),
                neighbour.getPort())));

    return nodes;
  }

  public Neighbours getNeighbours() {
    return neighbours;
  }

  @Override
  public String toString() {
    return String.format("[NeighborsMessage] \n");
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}
