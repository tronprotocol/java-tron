package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) {
    this.type = Message.PONG;
    this.data = data;
    try {
      this.pongMessage = Discover.PongMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public PongMessage(Node from) {

    Endpoint toEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();

    this.pongMessage = Discover.PongMessage.newBuilder()
        .setFrom(toEndpoint)
        .setEcho(1)
        .setTimestamp(System.currentTimeMillis())
        .build();

    this.type = Message.PONG;;

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
}
