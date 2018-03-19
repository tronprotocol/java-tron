package org.tron.core.config.args;

public class Overlay {

  private int port;

  public int getPort() {
    return port;
  }

  /**
   * Monitor port number.
   */
  public void setPort(int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Port(" + port + ") must in [0, 65535]");
    }

    this.port = port;
  }
}
