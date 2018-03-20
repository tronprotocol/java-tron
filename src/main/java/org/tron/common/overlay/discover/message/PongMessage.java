package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) {
    super(data, Message.TYPE_PONG);
    unPack();
  }

  public PongMessage(ByteString toAddress, int toPort, int echo) {
    this.data = this.pongMessage.toByteArray();
    this.pongMessage = Discover.PongMessage.newBuilder()
        .setTo(Endpoint.newBuilder().setAddress(toAddress).setTcpPort(toPort).setUdpPort(toPort)
            .build())
        .setEcho(echo)
        .setTimestamp(System.currentTimeMillis())
        .build();
  }

  private void unPack() {
    try {
      this.pongMessage = Discover.PongMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return String.format("[PongMessage]\n");
  }

}
