package org.tron.core.config.args;

import org.apache.commons.lang3.Range;

public class Overlay {

  private int port;

  public int getPort() {
    return port;
  }

  /**
   * Monitor port number.
   */
  public void setPort(int port) {
    Range<Integer> range = Range.between(0, 65535);
    if (!range.contains(port)) {
      throw new IllegalArgumentException("Port(" + port + ") must in [0, 65535]");
    }

    this.port = port;
  }
}
