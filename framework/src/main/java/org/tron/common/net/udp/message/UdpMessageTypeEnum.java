package org.tron.common.net.udp.message;

import java.util.HashMap;
import java.util.Map;

public enum UdpMessageTypeEnum {

  DISCOVER_PING((byte) 0x01),

  DISCOVER_PONG((byte) 0x02),

  DISCOVER_FIND_NODE((byte) 0x03),

  DISCOVER_NEIGHBORS((byte) 0x04),

  BACKUP_KEEP_ALIVE((byte) 0x05),

  UNKNOWN((byte) 0xFF);

  private static final Map<Byte, UdpMessageTypeEnum> intToTypeMap = new HashMap<>();

  static {
    for (UdpMessageTypeEnum value : values()) {
      intToTypeMap.put(value.type, value);
    }
  }

  private final byte type;

  UdpMessageTypeEnum(byte type) {
    this.type = type;
  }

  public static UdpMessageTypeEnum fromByte(byte type) {
    UdpMessageTypeEnum typeEnum = intToTypeMap.get(type);
    return typeEnum == null ? UNKNOWN : typeEnum;
  }

  public byte getType() {
    return type;
  }
}
