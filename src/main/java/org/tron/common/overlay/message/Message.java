package org.tron.common.overlay.message;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.discover.message.FindNodeMessage;
import org.tron.common.overlay.discover.message.NeighborsMessage;
import org.tron.common.overlay.discover.message.PingMessage;
import org.tron.common.overlay.discover.message.PongMessage;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.MessageTypes;

public abstract class Message {

  protected static final Logger logger = LoggerFactory.getLogger("Net");

  protected boolean unpacked;
  protected byte[] data;
  protected byte[] rawData;
  protected byte type;

  public Message() {
  }

  public Message(byte[] rawData) {
    this.rawData = rawData;
    unpacked = false;
  }

  public Message(byte type, byte[] rawData) {

    this.type = type;

    this.rawData = rawData;

    this.data = ArrayUtils.add(rawData, 0, type);

    unpacked = false;
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public abstract byte[] getRawData();

  public byte[] getData() {
    return this.data;
  }

  public static Message parse(byte[] data) {
    byte[] wire = new byte[data.length - 1];
    System.arraycopy(data, 1, wire, 0, data.length - 1);
    MessageTypes type = MessageTypes.fromByte(data[0]);
    switch (type) {
      case DISCOVER_PING:
        return new PingMessage(wire);
      case DISCOVER_PONG:
        return new PongMessage(wire);
      case DISCOVER_FIND_PEER:
        return new FindNodeMessage(wire);
      case DISCOVER_PEERS:
        return new NeighborsMessage(wire);
      default:
        throw new RuntimeException("Bad message");
    }
  }

  public String toString() {
    return "[Message Type: " + getType() + ", Message Hash: " + getMessageId() + "]";
  }

  //public byte getCode() { return type; }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }

  public abstract MessageTypes getType();

}
