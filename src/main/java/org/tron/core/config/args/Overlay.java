package org.tron.core.config.args;

import lombok.Getter;
import org.apache.commons.lang3.Range;

public class Overlay {

  @Getter
  private int port;

  /**
   * Monitor port number.
   */
  public void setPort(final int port) {
    Range<Integer> range = Range.between(0, 65535);
    if (!range.contains(port)) {
      throw new IllegalArgumentException("Port(" + port + ") must in [0, 65535]");
    }

    this.port = port;
  }
}
