package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) {
    super(data);
    unPack();
  }

  public PongMessage(ByteString toAddresss, int toPort, int echo, int timestamp) {
    this.pongMessage = Discover.PongMessage.newBuilder()
        .setTo(Endpoint.newBuilder().setAddress(toAddresss).setTcpPort(toPort).setUdpPort(toPort)
            .build())
        .setEcho(echo)
        .setTimestamp(timestamp)
        .build();
    pack();
  }

  private void unPack() {
    try {
      this.pongMessage = Discover.PongMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.pongMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  @Override
  public byte getType() {
    return this.type;
  }

  @Override
  public String toString() {

    String out = String.format("[PongMessage]\n");

    return out;
  }
}
