package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;

public class PingMessage extends Message {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) {
      super(data);
      unPack();
  }

  public PingMessage(Discover.PingMessage pingMessage){
      this.pingMessage = pingMessage;
      pack();
  }

  private void unPack() {
    try {
      this.pingMessage = Discover.PingMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  private void pack() {
    this.data = this.pingMessage.toByteArray();
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

    String out = String.format("[PingMessage] \n %s:%d ==> %s:%d\n",
        this.getFromHost(), this.getFromPort(), this.getToHost(), this.getToPort());

    return out;
  }

}
