package org.tron.common.message.udp;

import java.util.HashMap;
import java.util.Map;

public enum  UdpMessageTypeEnum {

  DISCOVER_PING(0x01),

  DISCOVER_PONG(0x02),

  DISCOVER_FIND_PEER(0x03),

  DISCOVER_PEERS(0x04),

  LAST(0xFF);

  private final int type;

  private static final Map<Integer, UdpMessageTypeEnum> intToTypeMap = new HashMap<>();

  static {
    for (UdpMessageTypeEnum value : values()) {
      intToTypeMap.put(value.type, value);
    }
  }

  UdpMessageTypeEnum(int type) {
    this.type = type;
  }

  public static UdpMessageTypeEnum fromInt(int i) {
    return intToTypeMap.get(i);
  }
}
