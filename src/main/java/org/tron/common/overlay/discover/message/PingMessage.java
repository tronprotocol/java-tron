package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Discover.PingMessage.Builder;

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

  public PingMessage(int version, ByteString fromAddress, int fromPort, ByteString toAddress,
      int toPort) {
    Builder builder = Discover.PingMessage.newBuilder()
        .setVersion(version)
        .setTimestamp(System.currentTimeMillis());

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setAddress(fromAddress)
        .setTcpPort(fromPort)
        .setUdpPort(fromPort)
        .build();

    Endpoint toEndpoint = Endpoint.newBuilder()
        .setAddress(toAddress)
        .setTcpPort(toPort)
        .setUdpPort(toPort)
        .build();

    this.pingMessage = builder.setFrom(fromEndpoint).setTo(toEndpoint).build();
    this.rawData = this.pingMessage.toByteArray();
  }

  private void unPack() {
    try {
      this.pingMessage = Discover.PingMessage.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public String getFromHost() {
    return ByteArray.toHexString(this.pingMessage.getFrom().getAddress().toByteArray());
  }

  public int getFromPort() {
    return this.pingMessage.getFrom().getUdpPort();
  }

  public String getToHost() {
    return ByteArray.toHexString(this.pingMessage.getTo().getAddress().toByteArray());
  }

  public int getToPort() {
    return this.pingMessage.getTo().getUdpPort();
  }

  @Override
  public String toString() {

    return String.format("[PingMessage] \n %s:%d ==> %s:%d\n",
        this.getFromHost(), this.getFromPort(), this.getToHost(), this.getToPort());
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}
