package org.tron.common.overlay.discover.message;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.Sha256Hash;

public abstract class Message {

  protected static final Logger logger = LoggerFactory.getLogger("Net");

  public static byte TYPE_PING = 1;
  public static byte TYPE_PONG = 2;
  public static byte TYPE_FIND_PEER = 3;
  public static byte TYPE_PEERS = 4;

  protected byte[] data;
  protected byte type;

  public Message() {
  }

  public Message(byte[] data, byte type) {
    this.data = data;
    this.type = type;
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public byte[] getData() {
    return this.data;
  }

  public byte getType() {
    return type;
  }

  public byte[] getSendData() {
    return ArrayUtils.add(this.data, 0, this.type);
  }

  public static Message parse(byte[] data){
    byte[] wire = new byte[data.length - 1];
    System.arraycopy(data, 1, wire, 0, data.length - 1);
      switch (data[0]){
        case 1:
          return new PingMessage(wire);
        case 2:
          return new PongMessage(wire);
        case 3:
          return new FindNodeMessage(wire);
        case 4:
          return new NeighborsMessage(wire);
        default:
            throw new RuntimeException("Bad message");
      }
  }

  @Override
  public String toString() {
    return "[Message Type: " + getType() + ", Message Hash: " + getMessageId() + "]";
  }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }
}