package org.tron.core.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

import java.util.concurrent.Callable;

@Slf4j(topic = "API")
public abstract class WalletOnCursor {

  @Autowired
  private Manager dbManager;

  protected Chainbase.Cursor cursor = Chainbase.Cursor.HEAD;

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

  public interface TronCallable<T> extends Callable<T> {
    @Override
    T call();
  }
}
