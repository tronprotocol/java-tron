package org.tron.core.services;

import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

@Slf4j(topic = "API")
public abstract class WalletOnCursor {

  protected Chainbase.Cursor cursor = Chainbase.Cursor.HEAD;
  @Autowired
  private Manager dbManager;

  public <T> T futureGet(TronCallable<T> callable) {
    try {
      dbManager.setCursor(cursor);
      return callable.call();
    } finally {
      dbManager.resetCursor();
    }
  }

  public void futureGet(Runnable runnable) {
    try {
      dbManager.setCursor(cursor);
      runnable.run();
    } finally {
      dbManager.resetCursor();
    }
  }

  /**
   * Selects this view's cursor on the current thread until the returned scope is closed, for a
   * body that throws checked exceptions and so cannot run through {@link #futureGet}.
   */
  public CursorScope selectCursor() {
    dbManager.setCursor(cursor);
    return dbManager::resetCursor;
  }

  /** Resets the current thread's cursor to HEAD when closed. */
  public interface CursorScope extends AutoCloseable {

    @Override
    void close();
  }

  public interface TronCallable<T> extends Callable<T> {

    @Override
    T call();
  }
}
