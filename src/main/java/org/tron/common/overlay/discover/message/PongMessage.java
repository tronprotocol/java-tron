package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.Args;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) {
    super(Message.PONG, data);
    try {
      this.pongMessage = Discover.PongMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public PongMessage(Node from) {
    super(Message.PONG, null);
    Endpoint toEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();
    this.pongMessage = Discover.PongMessage.newBuilder()
        .setFrom(toEndpoint)
        .setEcho(Args.getInstance().getNodeP2pVersion())
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.pongMessage.toByteArray();
  }

  public Node getFrom(){
    Endpoint from = this.pongMessage.getFrom();
    Node node = new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
    return node;
  }

  @Override
  public byte[] getNodeId() {
    return this.pongMessage.getFrom().getNodeId().toByteArray();
  }

  @Override
  public String toString() {
    return "[pongMessage: " + pongMessage;
  }
}
