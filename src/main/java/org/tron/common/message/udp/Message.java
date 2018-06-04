package org.tron.common.message.udp;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.message.udp.backup.KeepAliveMessage;
import org.tron.common.message.udp.discover.FindNodeMessage;
import org.tron.common.message.udp.discover.NeighborsMessage;
import org.tron.common.message.udp.discover.PingMessage;
import org.tron.common.message.udp.discover.PongMessage;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.exception.P2pException;

public abstract class Message {

  protected UdpMessageTypeEnum type;
  protected byte[] data;

  public Message(UdpMessageTypeEnum type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  public UdpMessageTypeEnum getType() {
    return this.type;
  }

  public byte[] getData() {
    return this.data;
  }

  public byte[] getSendData() {
    return ArrayUtils.add(this.data, 0, type.getType());
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public abstract byte[] getNodeId();

  @Override
  public String toString() {
    return "[Message Type: " + getType() + ", len: " + (data == null ? 0 : data.length) + "]";
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }

  public static Message parse(byte[] encode) throws Exception {
    byte type = encode[0];
    byte[] data = ArrayUtils.subarray(encode, 1, encode.length);
    switch (UdpMessageTypeEnum.fromByte(type)) {
      case DISCOVER_PING:
        return new PingMessage(data);
      case DISCOVER_PONG:
        return new PongMessage(data);
      case DISCOVER_FIND_NODE:
        return new FindNodeMessage(data);
      case DISCOVER_NEIGHBORS:
        return new NeighborsMessage(data);
      case BACKUP:
        return new KeepAliveMessage(data);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + type);
    }
  }
}
