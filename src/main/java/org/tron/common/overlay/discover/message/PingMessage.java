package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;

public class PingMessage extends Message {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) {
    super(data);
  }

  private void unPack() {
    try {
      this.pingMessage = Discover.PingMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.pingMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  public String getFromHost() {
    this.unPack();
    return ByteArray.toHexString(this.pingMessage.getFrom().getAddress().toByteArray());
  }

  public int getFromPort() {
    this.unPack();
    return this.pingMessage.getFrom().getUdpPort();
  }

  public String getToHost() {
    this.unPack();
    return ByteArray.toHexString(this.pingMessage.getTo().getAddress().toByteArray());
  }

  public int getToPort() {
    this.unPack();
    return this.pingMessage.getTo().getUdpPort();
  }

  @Override
  public String toString() {

    String out = String.format("[PingMessage] \n %s:%d ==> %s:%d\n",
        this.getFromHost(), this.getFromPort(), this.getToHost(), this.getToPort());

    return out;
  }

  @Override
  public MessageTypes getType() {
    return null;
  }
}
