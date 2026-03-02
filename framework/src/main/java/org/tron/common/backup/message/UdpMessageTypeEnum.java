package org.tron.common.backup.message;

import java.util.HashMap;
import java.util.Map;

public enum UdpMessageTypeEnum {

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
