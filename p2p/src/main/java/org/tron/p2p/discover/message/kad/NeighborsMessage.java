package org.tron.p2p.discover.message.kad;

import java.util.ArrayList;
import java.util.List;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.discover.protocol.kad.table.KademliaOptions;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.protos.Discover.Endpoint;
import org.tron.p2p.protos.Discover.Neighbours;
import org.tron.p2p.protos.Discover.Neighbours.Builder;
import org.tron.p2p.utils.NetUtil;

public class NeighborsMessage extends KadMessage {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] data) throws Exception {
    super(MessageType.KAD_NEIGHBORS, data);
    this.neighbours = Discover.Neighbours.parseFrom(data);
  }

  public NeighborsMessage(Node from, List<Node> neighbours, long sequence) {
    super(MessageType.KAD_NEIGHBORS, null);
    Builder builder = Neighbours.newBuilder()
        .setTimestamp(sequence);

    neighbours.forEach(neighbour -> {
      Endpoint endpoint = getEndpointFromNode(neighbour);
      builder.addNeighbours(endpoint);
    });

    Endpoint fromEndpoint = getEndpointFromNode(from);

    builder.setFrom(fromEndpoint);

    this.neighbours = builder.build();

    this.data = this.neighbours.toByteArray();
  }

  public List<Node> getNodes() {
    List<Node> nodes = new ArrayList<>();
    neighbours.getNeighboursList().forEach(n -> nodes.add(NetUtil.getNode(n)));
    return nodes;
  }

  @Override
  public long getTimestamp() {
    return this.neighbours.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return NetUtil.getNode(neighbours.getFrom());
  }

  @Override
  public String toString() {
    return "[neighbours: " + neighbours;
  }

  @Override
  public boolean valid() {
    if (!NetUtil.validNode(getFrom())) {
      return false;
    }
    if (getNodes().size() > 0) {
      if (getNodes().size() > KademliaOptions.BUCKET_SIZE) {
        return false;
      }
      for (Node node : getNodes()) {
        if (!NetUtil.validNode(node)) {
          return false;
        }
      }
    }
    return true;
  }
}
