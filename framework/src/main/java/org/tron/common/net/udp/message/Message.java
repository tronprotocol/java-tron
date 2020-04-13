package org.tron.common.net.udp.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.net.udp.message.backup.KeepAliveMessage;
import org.tron.common.net.udp.message.discover.FindNodeMessage;
import org.tron.common.net.udp.message.discover.NeighborsMessage;
import org.tron.common.net.udp.message.discover.PingMessage;
import org.tron.common.net.udp.message.discover.PongMessage;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.exception.P2pException;
import org.tron.protos.Discover.Endpoint;

public abstract class Message {

  protected UdpMessageTypeEnum type;
  protected byte[] data;

  public Message(UdpMessageTypeEnum type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  public static Node getNode(Endpoint endpoint) {
    Node node = new Node(endpoint.getNodeId().toByteArray(),
        ByteArray.toStr(endpoint.getAddress().toByteArray()), endpoint.getPort());
    return node;
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
      case BACKUP_KEEP_ALIVE:
        return new KeepAliveMessage(data);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + type);
    }
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
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(), getData());
  }

  public abstract Node getFrom();

  public abstract long getTimestamp();

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
}
