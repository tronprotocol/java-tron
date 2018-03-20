package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PongMessage extends DiscoverMessage {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] rawData) {
    super(MessageTypes.DISCOVER_PONG.asByte(), rawData);
    unPack();
  }

  @Override
  public byte[] getRawData() {
    return this.rawData;
  }

  public PongMessage(Node to, int echo, long timestamp) {
    Endpoint toEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(to.getHost())))
        .setPort(to.getPort())
        .setNodeId(ByteString.copyFrom(to.getId()))
        .build();

    this.pongMessage = Discover.PongMessage.newBuilder()
        .setTo(toEndpoint)
        .setEcho(echo)
        .setTimestamp(timestamp)
        .build();
    this.rawData = this.pongMessage.toByteArray();
  }

  public static PongMessage create(Node from, int echo) {
    return new PongMessage(from, echo, System.currentTimeMillis());
  }

  public Node getFrom(){
    Endpoint from = this.pongMessage.getTo();

    Node node = new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());

    return node;
  }



  private void unPack() {
    try {
      this.pongMessage = Discover.PongMessage.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return String.format("[PongMessage]\n");
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}
