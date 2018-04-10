package org.tron.common.overlay.discover.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.Sha256Hash;

public abstract class Message {

  public static byte PING = 1;
  public static byte PONG = 2;
  public static byte FINE_PEERS = 3;
  public static byte GET_PEERS = 4;

  protected byte type;
  protected byte[] data;

  public Message(byte type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  public byte getType(){
    return this.type;
  }

  public byte[] getData() {
    return this.data;
  }

  public byte[] getSendData() {
    return ArrayUtils.add(this.data, 0 ,type);
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public abstract byte[] getNodeId();

  @Override
  public String toString() {
    return "[Message Type: " + getType() + ", len: " + (data == null? 0:data.length) + "]";
  }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }

  public static Message parse(byte[] encode) {
    byte type = encode[0];
    byte[] data = ArrayUtils.subarray(encode, 1, encode.length);
    switch (type) {
      case 1:
        return new PingMessage(data);
      case 2:
        return new PongMessage(data);
      case 3:
        return new FindNodeMessage(data);
      case 4:
        return new NeighborsMessage(data);
      default:
        throw new IllegalArgumentException("No such message");
    }
  }
}
