package org.tron.core.db2.snapshot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.db2.database.TronDatabase;
import org.tron.core.exception.RevokingStoreIllegalStateException;

import java.util.ArrayList;
import java.util.List;

@Component
public class SnapshotManager {
  private List<TronDatabase> dbs = new ArrayList<>();
  @Getter
  private int size;
  private boolean disabled = true;
  private int activeSession = 0;

  public Session buildSession() {
    return buildSession(false);
  }

  public synchronized Session buildSession(boolean forceEnable) {
    if (disabled && !forceEnable) {
      return new Session(this);
    }

    boolean disableOnExit = disabled && forceEnable;
    if (forceEnable) {
      disabled = false;
    }

    advance();
    ++activeSession;
    return new Session(this, disableOnExit);
  }

  public void add(TronDatabase db) {
    dbs.add(db);
  }

  public void advance() {
    dbs.forEach(db -> db.setHead(db.getHead().advance()));
    ++size;
  }

  public void retreat() {
    dbs.forEach(db -> db.setHead(db.getHead().retreat()));
    --size;
  }

  public void merge() {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException("activeDialog has to be greater than 0");
    }

    if (size < 2) {
      return;
    }

    dbs.forEach(db -> {
      db.getHead().getPrevious().merge(db.getHead());
      db.setHead(db.getHead().getPrevious());
    });
    --size;
  }

  public void flush() {
    dbs.forEach(db -> {
      Snapshot head = db.getHead();
      while (head.getPrevious().getPrevious().getPrevious() != null) {
        head = head.getPrevious();
      }

      head.getPrevious().getPrevious().merge(head.getPrevious());
      head.setPrevious(head.getPrevious().getPrevious());
    });
  }

  public synchronized void revoke() {
    if (disabled) {
      return;
    }

    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException("activeSession has to be greater than 0");
    }

    disabled = true;

    try {
      retreat();
    } finally {
      disabled = false;
    }
    --activeSession;
  }

  public synchronized void commit() {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException("activeSession has to be greater than 0");
    }

    --activeSession;
  }

  public synchronized void pop() {
    if (activeSession != 0) {
      throw new RevokingStoreIllegalStateException("activeSession has to be equal 0");
    }

    disabled = true;

    try {
      retreat();
    } finally {
      disabled = false;
    }
  }

  public synchronized void enable() {
    disabled = false;
  }

  public synchronized void disable() {
    disabled = true;
  }

  @Slf4j
  @Getter // only for unit test
  public static class Session implements AutoCloseable {

    private SnapshotManager snapshotManager;
    private boolean applySnapshot = true;
    private boolean disableOnExit = false;

    public Session(SnapshotManager snapshotManager) {
      this(snapshotManager, false);
    }

    public Session(SnapshotManager snapshotManager, boolean disableOnExit) {
      this.snapshotManager = snapshotManager;
      this.disableOnExit = disableOnExit;
    }

    void commit() {
      applySnapshot = false;
      snapshotManager.commit();
    }

    void revoke() {
      if (applySnapshot) {
        snapshotManager.revoke();
      }

      applySnapshot = false;
    }

    void merge() {
      if (applySnapshot) {
        snapshotManager.merge();
      }

      applySnapshot = false;
    }

    public void destroy() {
      try {
        if (applySnapshot) {
          snapshotManager.revoke();
        }
      } catch (Exception e) {
        logger.error("revoke database error.", e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }

    @Override
    public void close() {
      try {
        if (applySnapshot) {
          snapshotManager.revoke();
        }
      } catch (Exception e) {
        logger.error("revoke database error.", e);
        throw new RevokingStoreIllegalStateException(e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }
  }

  public static final class SessionOptional {

    private static final SessionOptional INSTANCE = OptionalEnum.INSTANCE.getInstance();

    private java.util.Optional<Session> value;

    private SessionOptional() {
      this.value = java.util.Optional.empty();
    }

    public synchronized SessionOptional setValue(Session value) {
      if (!this.value.isPresent()) {
        this.value = java.util.Optional.of(value);
      }
      return this;
    }

    public synchronized boolean valid() {
      return value.isPresent();
    }

    public synchronized void reset() {
      value.ifPresent(Session::destroy);
      value = java.util.Optional.empty();
    }

    public static SessionOptional instance() {
      return INSTANCE;
    }

    private enum OptionalEnum {
      INSTANCE;

      private SessionOptional instance;

      OptionalEnum() {
        instance = new SessionOptional();
      }

      private SessionOptional getInstance() {
        return instance;
      }
    }

  }

}
