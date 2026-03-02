package org.tron.core.net.service.nodepersist;

import lombok.Getter;
import lombok.Setter;

public class DBNode {

  @Getter
  @Setter
  private String host;

  @Getter
  @Setter
  private int port;

  public DBNode() {
  }

  public DBNode(String host, int port) {
    this.host = host;
    this.port = port;
  }

}
