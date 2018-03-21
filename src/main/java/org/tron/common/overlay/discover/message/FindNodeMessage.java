package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Discover.FindNeighbours;

public class FindNodeMessage extends Message {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] data) {
    super(data);
    this.type = Message.FINE_PEERS;
    this.data = data;
    try {
      this.findNeighbours = Discover.FindNeighbours.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public FindNodeMessage(Node from, byte[] targetId) {
    super(Message.PING, null);

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();

    this.findNeighbours = FindNeighbours.newBuilder()
        .setFrom(fromEndpoint)
        .setTargetId(ByteString.copyFrom(targetId))
        .setTimestamp(System.currentTimeMillis())
        .build();

    this.data = this.findNeighbours.toByteArray();
  }

  public byte[] getTargetId(){
      return this.findNeighbours.getTargetId().toByteArray();
  }

  @Override
  public byte[] getNodeId() {
      return this.findNeighbours.getFrom().getNodeId().toByteArray();
  }

}
