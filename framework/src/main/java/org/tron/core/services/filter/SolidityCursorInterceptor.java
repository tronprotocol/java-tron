package org.tron.core.services.filter;

import org.springframework.stereotype.Component;
import org.tron.core.db2.core.Chainbase;

/**
 * Makes every call on the Solidity gRPC server read the solidified state view.
 */
@Component
public class SolidityCursorInterceptor extends CursorServerInterceptor {

  public SolidityCursorInterceptor() {
    this.cursor = Chainbase.Cursor.SOLIDITY;
  }
}
