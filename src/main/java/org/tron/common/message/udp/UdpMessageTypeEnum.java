package org.tron.common.message.udp;

import java.util.HashMap;
import java.util.Map;

public enum  UdpMessageTypeEnum {

  DISCOVER_PING((byte) 0x01),

  DISCOVER_PONG((byte) 0x02),

  DISCOVER_FIND_PEER((byte) 0x03),

  DISCOVER_PEERS((byte) 0x04),

  LAST((byte) 0xFF);

  private final byte type;

  private static final Map<Byte, UdpMessageTypeEnum> intToTypeMap = new HashMap<>();

  static {
    for (UdpMessageTypeEnum value : values()) {
      intToTypeMap.put(value.type, value);
    }
  }

  UdpMessageTypeEnum(byte type) {
    this.type = type;
  }

  public static UdpMessageTypeEnum fromByte(byte i) {
    return intToTypeMap.get(i);
  }
}
