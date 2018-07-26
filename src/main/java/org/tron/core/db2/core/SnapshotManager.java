package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class SnapshotManager implements RevokingDatabase {
  private static final int DEFAULT_STACK_MAX_SIZE = 256;

  private List<RevokingDBWithCachingNewValue> dbs = new ArrayList<>();
  @Getter
  private int size = 0;
  private AtomicInteger maxSize = new AtomicInteger(DEFAULT_STACK_MAX_SIZE);

  private boolean disabled = true;
  private int activeSession = 0;
  private boolean unChecked = true;
  private WriteOptions writeOptions = new WriteOptions().sync(true);

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

    while (size > maxSize.get()) {
      flush();
    }

    advance();
    ++activeSession;
    return new Session(this, disableOnExit);
  }

  @Override
  public void add(IRevokingDB db) {
    dbs.add((RevokingDBWithCachingNewValue) db);
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

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize.set(maxSize);
  }

  public int getMaxSize() {
    return maxSize.get();
  }

  public synchronized void disable() {
    disabled = true;
  }

  @Override
  public void shutdown() {
    System.err.println("******** begin to pop revokingDb ********");
    System.err.println("******** before revokingDb size:" + size);
    System.err.println("******** end to pop revokingDb ********");
  }

  public void flush() {
    if (unChecked) {
      return;
    }

    createCheckPoint();

    dbs.forEach(db -> {
      Snapshot head = db.getHead();
      if (head.getPrevious() == null) {
        return;
      }

      if (head.getPrevious().getClass() == SnapshotRoot.class) {
        head.getPrevious().merge(head);
        db.setHead(head.getPrevious());
        return;
      }

      while (head.getPrevious().getPrevious().getClass() == SnapshotImpl.class) {
        head = head.getPrevious();
      }

      head.getPrevious().getPrevious().merge(head.getPrevious());
      head.setPrevious(head.getPrevious().getPrevious());
    });

    --size;
    deleteCheckPoint();
  }

  private void createCheckPoint() {
    LevelDbDataSourceImpl levelDbDataSource =
        new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");
    levelDbDataSource.initDB();
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
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
        batch.put(WrappedByteArray.of(Bytes.concat(simpleEncode(dbName), k.getBytes())), WrappedByteArray.of(v.encode()));
      }
    }

    levelDbDataSource.updateByBatch(batch.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll), writeOptions);
    levelDbDataSource.closeDB();
  }

  private void deleteCheckPoint() {
    LevelDbDataSourceImpl levelDbDataSource =
        new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");

    FileUtil.recursiveDelete(levelDbDataSource.getDbPath().toString());
  }

  // ensure run this method first after process start.
  @Override
  public void check() {
    for (RevokingDBWithCachingNewValue db : dbs) {
      if (db.getHead().getClass() != SnapshotRoot.class) {
        throw new IllegalStateException("first check.");
      }
    }

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
        byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);

        byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
        if (realValue != null) {
          dbMap.get(db).getHead().put(realKey, realValue);
        } else {
          dbMap.get(db).getHead().remove(realKey);
        }
      }

      dbs.forEach(db -> {
        db.getHead().getRoot().merge(db.getHead());
        db.setHead(db.getHead().getPrevious());
      });
      retreat();
    }

    levelDbDataSource.closeDB();
    FileUtil.recursiveDelete(levelDbDataSource.getDbPath().toString());
    unChecked = false;
  }

  private byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  public static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
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
