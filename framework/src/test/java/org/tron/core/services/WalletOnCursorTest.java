package org.tron.core.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.junit.Test;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;

public class WalletOnCursorTest {

  @Test
  public void testSelectCursorHoldsTheCursorUntilClosed() throws Exception {
    Manager manager = mock(Manager.class);
    WalletOnPBFT view = new WalletOnPBFT();
    Field field = WalletOnCursor.class.getDeclaredField("dbManager");
    field.setAccessible(true);
    field.set(view, manager);

    try (WalletOnCursor.CursorScope ignored = view.selectCursor()) {
      verify(manager).setCursor(Chainbase.Cursor.PBFT);
      verify(manager, never()).resetCursor();
    }
    verify(manager).resetCursor();
  }
}
