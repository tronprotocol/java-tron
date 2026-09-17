package org.tron.core.services.filter;

import org.springframework.stereotype.Component;
import org.tron.core.db2.core.Chainbase;

/**
 * Makes every call on the PBFT gRPC server read the PBFT-confirmed state view.
 */
@Component
public class PbftCursorInterceptor extends CursorServerInterceptor {

  public PbftCursorInterceptor() {
    this.cursor = Chainbase.Cursor.PBFT;
  }
}
