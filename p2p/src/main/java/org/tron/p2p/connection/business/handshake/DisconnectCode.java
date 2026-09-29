package org.tron.p2p.connection.business.handshake;

public enum DisconnectCode {
  NORMAL(0),
  TOO_MANY_PEERS(1),
  DIFFERENT_VERSION(2),
  TIME_BANNED(3),
  DUPLICATE_PEER(4),
  MAX_CONNECTION_WITH_SAME_IP(5),
  UNKNOWN(256);

  private final Integer value;

  DisconnectCode(Integer value) {
    this.value = value;
  }

  public Integer getValue() {
    return value;
  }

  public static DisconnectCode forNumber(int code) {
    for (DisconnectCode disconnectCode : values()) {
      if (disconnectCode.value == code) {
        return disconnectCode;
      }
    }
    return UNKNOWN;
  }
}
