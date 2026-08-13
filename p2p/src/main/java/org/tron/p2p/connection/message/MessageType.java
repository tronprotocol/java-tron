package org.tron.p2p.connection.message;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {

  KEEP_ALIVE_PING((byte) 0xff),

  KEEP_ALIVE_PONG((byte) 0xfe),

  HANDSHAKE_HELLO((byte) 0xfd),

  STATUS((byte) 0xfc),

  DISCONNECT((byte) 0xfb),

  UNKNOWN((byte) 0x80);

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
