package org.tron.core.services.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

@Component
public class PbftCursorFilter extends WalletCursorFilter {

  @Autowired
  public PbftCursorFilter(Manager dbManager) {
    super(dbManager, Chainbase.Cursor.PBFT);
  }
}
