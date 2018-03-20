package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.node.Node;
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

  public PongMessage(ByteString toAddress, int toPort, int echo) {
    this.pongMessage = Discover.PongMessage.newBuilder()
        .setTo(Endpoint.newBuilder().setAddress(toAddress).setTcpPort(toPort).setUdpPort(toPort)
            .build())
        .setEcho(echo)
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.rawData = this.pongMessage.toByteArray();
  }

  public static PongMessage create(Node from){
    return null;
  }

  public Node getFrom(){
    return null;
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
