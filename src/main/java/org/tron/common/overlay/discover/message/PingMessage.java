package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PingMessage extends DiscoverMessage {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] rawData) {
    super(MessageTypes.DISCOVER_PING.asByte(), rawData);
    unPack();
  }

  @Override
  public byte[] getRawData() {
    return this.rawData;
  }

  @Override
  public byte[] getNodeId() {
    return this.pingMessage.getFrom().getNodeId().toByteArray();
  }

  public PingMessage(int version, Node from, Node to,
      long timestamp) {
    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Endpoint toEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(to.getId()))
        .setPort(to.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(to.getHost())))
        .build();

    this.pingMessage = Discover.PingMessage.newBuilder().setVersion(version)
        .setFrom(fromEndpoint)
        .setTo(toEndpoint)
        .setTimestamp(timestamp)
        .build();

    this.type = MessageTypes.DISCOVER_PING.asByte();
    this.rawData = this.pingMessage.toByteArray();
  }

  private void unPack() {
    try {
      this.pingMessage = Discover.PingMessage.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public static PingMessage create(Node from, Node to){
    return new PingMessage(1, from, to, System.currentTimeMillis());
  }

  public Node getFrom (){
    Endpoint from = this.pingMessage.getFrom();

    Node node = new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());

    return node;
  }

  public Node getTo(){
    Endpoint to = this.pingMessage.getTo();

    Node node = new Node(to.getNodeId().toByteArray(),
        ByteArray.toStr(to.getAddress().toByteArray()), to.getPort());

    return node;
  }

  @Override
  public String toString() {

    return String.format("[PingMessage] \n");
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}
