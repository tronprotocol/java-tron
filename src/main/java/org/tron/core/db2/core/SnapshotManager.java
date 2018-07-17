package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.exception.RevokingStoreIllegalStateException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SnapshotManager implements RevokingDatabase {
  private List<RevokingDBWithCachingNewValue> dbs = new ArrayList<>();
  @Getter
  private int size = 0;
  private boolean disabled = true;
  private int activeSession = 0;

  public ISession buildSession() {
    return buildSession(false);
  }

  public synchronized ISession buildSession(boolean forceEnable) {
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

  public void add(RevokingDBWithCachingNewValue db) {
    dbs.add(db);
  }

  private void advance() {
    dbs.forEach(db -> db.setHead(db.getHead().advance()));
    ++size;
  }

  private void retreat() {
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

    dbs.forEach(db -> db.getHead().getPrevious().merge(db.getHead()));
    retreat();
    --activeSession;
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

  @Override
  public int size() {
    return size;
  }

  public synchronized void disable() {
    disabled = true;
  }

  @Override
  public void shutdown() {
  }

  public void flush() {
    createCheckPoint();

    dbs.forEach(db -> {
      Snapshot head = db.getHead();
      while (head.getPrevious().getPrevious().getPrevious() != null) {
        head = head.getPrevious();
      }

      head.getPrevious().getPrevious().merge(head.getPrevious());
      head.setPrevious(head.getPrevious().getPrevious());
    });

    deleteCheckPoint();
  }

  private void createCheckPoint() {
    LevelDbDataSourceImpl levelDbDataSource =
        new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");
    levelDbDataSource.initDB();

    Map<byte[], byte[]> batch = new HashMap<>();
    for (RevokingDBWithCachingNewValue db : dbs) {
      Snapshot head = db.getHead();
      while (head.getPrevious().getPrevious() != null) {
        head = head.getPrevious();
      }

      SnapshotImpl snapshot = (SnapshotImpl) head;
      DB<Key, Value> keyValueDB = snapshot.getDb();
      String dbName = db.getDbName();
      for (Map.Entry<Key, Value> e : keyValueDB) {
        Key k = e.getKey();
        Value v = e.getValue();
        batch.put(Bytes.concat(simpleEncode(dbName), k.getBytes()), v.encode());
      }
    }
    levelDbDataSource.updateByBatch(batch);
    levelDbDataSource.closeDB();
  }

  private void deleteCheckPoint() {
    LevelDbDataSourceImpl levelDbDataSource =
        new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");

    FileUtil.recursiveDelete(levelDbDataSource.getDbPath().toString());
  }

  // ensure run this method first after process start.
  public void check() {
    LevelDbDataSourceImpl levelDbDataSource =
        new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");
    levelDbDataSource.initDB();
    if (!levelDbDataSource.allKeys().isEmpty()) {
      Map<String, RevokingDBWithCachingNewValue> dbMap = dbs.stream()
          .map(db -> Maps.immutableEntry(db.getDbName(), db))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      advance();
      for (Map.Entry<byte[], byte[]> e : levelDbDataSource) {
        byte[] key = e.getKey();
        byte[] value = e.getValue();
        String db = simpleDecode(key);
        byte[] realKey = new byte[key.length - db.getBytes().length - 4];
        System.arraycopy(key, db.getBytes().length + 4, realKey, 0, key.length - db.getBytes().length - 4);

        byte[] realValue = value.length == 1 ? null : new byte[value.length - 1];
        if (realValue != null) {
          dbMap.get(db).getHead().put(realKey, realValue);
        } else {
          dbMap.get(db).getHead().remove(realKey);
        }
      }

      dbs.forEach(db -> {
        db.getHead().getPrevious().merge(db.getHead());
        db.setHead(db.getHead().getPrevious());
      });
      retreat();
    }

    levelDbDataSource.closeDB();
    FileUtil.recursiveDelete(levelDbDataSource.getDbPath().toString());
  }

  private byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  private String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = new byte[4];
    System.arraycopy(bytes, 0, lengthBytes, 0, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = new byte[length];
    System.arraycopy(bytes, 4, value, 0, length);
    return new String(value);
  }

  @Slf4j
  @Getter // only for unit test
  public static class Session  implements ISession {
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

    @Override
    public void commit() {
      applySnapshot = false;
      snapshotManager.commit();
    }

    @Override
    public void revoke() {
      if (applySnapshot) {
        snapshotManager.revoke();
      }

      applySnapshot = false;
    }

    @Override
    public void merge() {
      if (applySnapshot) {
        snapshotManager.merge();
      }

      applySnapshot = false;
    }

    @Override
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

}
