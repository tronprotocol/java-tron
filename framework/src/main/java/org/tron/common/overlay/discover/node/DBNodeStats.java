package org.tron.common.overlay.discover.node;

import lombok.Getter;
import lombok.Setter;

public class DBNodeStats {

  @Getter
  @Setter
  private byte[] id;

  @Getter
  @Setter
  private String host;

  @Getter
  @Setter
  private int port;

  @Getter
  @Setter
  private int reputation;

  public DBNodeStats() {
  }

  public DBNodeStats(byte[] id, String host, int port, int reputation) {
    this.id = id;
    this.host = host;
    this.port = port;
    this.reputation = reputation;
  }

}
