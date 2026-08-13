package org.tron.p2p.discover.message;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {

  KAD_PING((byte) 0x01),

  KAD_PONG((byte) 0x02),

  KAD_FIND_NODE((byte) 0x03),

  KAD_NEIGHBORS((byte) 0x04),

  UNKNOWN((byte) 0xFF);

  private final byte type;

  MessageType(byte type) {
    this.type = type;
  }

  public byte getType() {
    return type;
  }

  private static final Map<Byte, MessageType> map = new HashMap<>();

  static {
    for (MessageType value : values()) {
      map.put(value.type, value);
    }
  }
  public static MessageType fromByte(byte type) {
    MessageType typeEnum = map.get(type);
    return typeEnum == null ? UNKNOWN : typeEnum;
  }

}
