package org.tron.core.db2.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.sun.org.apache.xpath.internal.WhitespaceStrippingElementMatcher;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.exception.RevokingStoreIllegalStateException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class SnapshotManager implements RevokingDatabase {
  private static final int DEFAULT_STACK_MAX_SIZE = 256;
  private static final int DEFAULT_FLUSH_COUNT = 5;

 @Autowired
  private TronApplicationContext tronApplicationContext;

  @Getter
  private List<RevokingDBWithCachingNewValue> dbs = new ArrayList<>();
  @Getter
  private int size = 0;
  private AtomicInteger maxSize = new AtomicInteger(DEFAULT_STACK_MAX_SIZE);

  private boolean disabled = true;
  private int activeSession = 0;
  private boolean unChecked = true;

  private volatile int flushCount = 0;

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

//    if (size > maxSize.get()) {
//      logger.info("****size:" + size + ", maxsize:" + maxSize.get());
//      size = maxSize.get();
//      flush();
//    }
    // debug begin
//    debug();
    // debug end
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

  @Override
  public void fastPop() {
    pop();
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
    try {
      while (shouldBeRefreshed()) {
        logger.info("waiting leveldb flush done");
        TimeUnit.MILLISECONDS.sleep(10);
      }
    } catch (InterruptedException e) {
        System.out.println(e.getMessage() + e);
        Thread.currentThread().interrupt();
    }
    System.err.println("******** end to pop revokingDb ********");
  }

  @Override
  public void updateSolidity(long oldSolidifiedBlockNum, long newSolidifedBlockNum) {
    long diff = newSolidifedBlockNum - oldSolidifiedBlockNum;

    // debug begin
    RevokingDBWithCachingNewValue debugDB = dbs.get(0);
    Snapshot next = debugDB.getHead().getRoot();
    List<Snapshot> snapshots = new ArrayList<>();
    while (next != null) {
      snapshots.add(next);
      next = next.getNext();
    }
    logger.info("****updateSolidity newNum:{}, oldNum:{}, diff:{}, db:{}, solid:{}, head:{}, next:{}, all snapshot:{}",
        newSolidifedBlockNum,
        oldSolidifiedBlockNum,
        diff,
        debugDB.getDbName(),
        debugDB.getHead().getSolidity(),
        debugDB.getHead(),
        debugDB.getHead().getSolidity().getNext(),
        snapshots
    );
    // debug end

    for (int i = 0; i < diff; i++) {
      ++flushCount;
      for (RevokingDBWithCachingNewValue db : dbs) {
        db.getHead().updateSolidity();
      }
    }
    flush();
  }

  private boolean shouldBeRefreshed() {
    return flushCount >= DEFAULT_FLUSH_COUNT;
  }

  private void refresh() {
    // debug begin
//    List<String> debugBlockHashs = new ArrayList<>();
//    Map<String, String> debugDumpDataMap = new HashMap<>();
//    Multimap<String, byte[]> values = ArrayListMultimap.create();
    // debug end

    for (RevokingDBWithCachingNewValue db : dbs) {
      if (Snapshot.isRoot(db.getHead())) {
        return;
      }

      List<Snapshot> snapshots = new ArrayList<>();
      Snapshot solidity = db.getHead().getSolidity();
      if (Snapshot.isRoot(solidity)) {
        return;
      }

      Snapshot next = solidity.getRoot().getNext();
      while (next != solidity.getNext()) {
        // debug begin
//        String dbName = db.getDbName();
//        SnapshotImpl snapshot = (SnapshotImpl) next;
//        DB<Key, Value> keyValueDB = snapshot.getDb();
//        for (Map.Entry<Key, Value> e : keyValueDB) {
//          Key k = e.getKey();
//          Value v = e.getValue();
//          debugDumpDataMap.put(dbName + ":" + ByteUtil.toHexString(k.getBytes()),
//              dbName + ":" + ByteUtil.toHexString(k.getBytes()) + ":"
//              + (e.getValue().getBytes() == null ? null : Sha256Hash.of(v.getBytes())));
//          if ("block".equals(dbName)) {
//            debugBlockHashs.add(Longs.fromByteArray(k.getBytes()) + ":" + ByteUtil.toHexString(k.getBytes()));
//          }
//          if ("account".equals(dbName) && v.getBytes() != null) {
//            values.put(ByteUtil.toHexString(k.getBytes()), v.getBytes());
//          }
//        }
        // debug end
        snapshots.add(next);
        next = next.getNext();
        --size;
      }

      // debug begin
//        if ("block".equals(db.getDbName())) {
//            logger.info("**** debug previous:{}, next:{}, snapshots:{}",snapshots.get(0).getPrevious(), snapshots.get(snapshots.size() - 1).getNext(), snapshots);
//        }
      // debug end

      ((SnapshotRoot) solidity.getRoot()).merge(snapshots);

      solidity.resetSolidity();
      if (db.getHead() == solidity) {
       db.setHead(solidity.getRoot());
      } else {
        solidity.getNext().setPrevious(solidity.getRoot());
        solidity.getRoot().setNext(solidity.getNext());
      }
    }
    // debug begin
//    List<String> debugDumpDatas = debugDumpDataMap.entrySet().stream().map(Entry::getValue).sorted(String::compareTo).collect(Collectors.toList());
//    logger.info("***debug refresh:    blocks={}, datahash:{}, accounts:{}\n", debugBlockHashs, Sha256Hash.of(debugDumpDatas.toString().getBytes()), printAccount(null));
    // debug end
  }

  public void flush() {
    if (unChecked) {
      return;
    }

    if (shouldBeRefreshed()) {
      flushCount = 0;
      deleteCheckPoint();
      createCheckPoint();
      refresh();
    }
  }

  private void createCheckPoint() {
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
    // debug begin
//    List<String> debugBlockHashs = new ArrayList<>();
//    Map<String, String> debugDumpDataMap = new HashMap<>();
//    Multimap<String, byte[]> values = ArrayListMultimap.create();
    // debug end
    for (RevokingDBWithCachingNewValue db : dbs) {
      Snapshot head = db.getHead();
      if (Snapshot.isRoot(head)) {
        return;
      }

      String dbName = db.getDbName();
      Snapshot solidity = db.getHead().getSolidity();
      if (Snapshot.isRoot(solidity)) {
        return;
      }

      Snapshot next = solidity.getRoot().getNext();
      while (next != solidity.getNext()) {
        SnapshotImpl snapshot = (SnapshotImpl) next;
        DB<Key, Value> keyValueDB = snapshot.getDb();
        for (Map.Entry<Key, Value> e : keyValueDB) {
          Key k = e.getKey();
          Value v = e.getValue();
          batch.put(WrappedByteArray.of(Bytes.concat(simpleEncode(dbName), k.getBytes())),
              WrappedByteArray.of(v.encode()));
          // debug begin
//          debugDumpDataMap.put(dbName + ":" + ByteUtil.toHexString(k.getBytes()),
//              dbName + ":" + ByteUtil.toHexString(k.getBytes()) + ":" + (v.getBytes() == null ? null : Sha256Hash.of(v.getBytes())));
//          if ("block".equals(dbName)) {
//            debugBlockHashs.add(Longs.fromByteArray(k.getBytes()) + ":" + ByteUtil.toHexString(k.getBytes()));
//          }
//          if ("account".equals(dbName) && v.getBytes() != null) {
//            values.put(ByteUtil.toHexString(k.getBytes()), v.getBytes());
//          }
          // debug end
        }
        next = next.getNext();
      }
    }

    // debug begin
//    List<String> debugDumpDatas = debugDumpDataMap.entrySet().stream().map(Entry::getValue).sorted(String::compareTo).collect(Collectors.toList());
//    logger.info("***debug checkpoint: blocks={}, datahash:{}, accounts:{}\n", debugBlockHashs, Sha256Hash.of(debugDumpDatas.toString().getBytes()), printAccount(null));
    // debug end
    LevelDbDataSourceImpl levelDbDataSource =
        new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");
    levelDbDataSource.initDB();
    levelDbDataSource.updateByBatch(batch.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll), new WriteOptions().sync(true));
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
    // debug begin
//    List<String> debugBlockHashs = new ArrayList<>();
//    List<String> debugDumpDatas = new ArrayList<>();
//    Multimap<String, byte[]> values = ArrayListMultimap.create();
    // debug end

    for (RevokingDBWithCachingNewValue db : dbs) {
      if (!Snapshot.isRoot(db.getHead())) {
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

        // debug begin
//        debugDumpDatas.add(db + ":" + ByteUtil.toHexString(realKey) + ":" + (realValue == null ? null : Sha256Hash.of(realValue)));
//        if ("block".equals(db)) {
//          debugBlockHashs.add(Longs.fromByteArray(realKey) + ":" + ByteUtil.toHexString(realKey));
//        }
//        if ("account".equals(db) && realValue != null) {
//          values.put(ByteUtil.toHexString(realKey), realValue);
//        }
        // debug end
      }

      dbs.forEach(db -> db.getHead().getRoot().merge(db.getHead()));
      retreat();
    }

    // debug begin
//    debugDumpDatas.sort(String::compareTo);
//    logger.info("***debug check:      blocks={}, datahash:{}, accounts:{}\n", debugBlockHashs, Sha256Hash.of(debugDumpDatas.toString().getBytes()), printAccount(null));
    // debug end

    levelDbDataSource.closeDB();
//    FileUtil.recursiveDelete(levelDbDataSource.getDbPath().toString());
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

  private void debug() {
    // debug begin
    List<String> debugBlockHashs = new ArrayList<>();
    List<String> debugDumpDatas = new ArrayList<>();
    Map<String, byte[]> values = new HashMap<>();
    for (RevokingDBWithCachingNewValue db : dbs) {
      String dbName = db.getDbName();
      Snapshot head = db.getHead();
      if (!Snapshot.isImpl(head)) {
        return;
      }
      SnapshotImpl snapshot = (SnapshotImpl) head;
      Streams.stream(snapshot.db).forEach(e -> {
        if ("block".equals(dbName)) {
          debugBlockHashs.add(Longs.fromByteArray(e.getKey().getBytes()) + ":" + ByteUtil.toHexString(e.getKey().getBytes()));
        }
        debugDumpDatas.add(dbName + ":" + ByteUtil.toHexString(e.getKey().getBytes()) + ":" + (e.getValue().getBytes() == null ? null : Sha256Hash.of(e.getValue().getBytes())));
        if ("account".equals(dbName) && e.getValue().getBytes() != null) {
          values.put(ByteUtil.toHexString(e.getKey().getBytes()), e.getValue().getBytes());
        }
      });
    }
    if (!debugBlockHashs.isEmpty()) {
      debugDumpDatas.sort(String::compareTo);
      logger.info("***debug debug:      blocks={}, datahash:{}, account:{}\n", debugBlockHashs,
          Sha256Hash.of(debugDumpDatas.toString().getBytes()), printAccount(values));
    }
    // debug end

  }

  private Map<String, AccountCapsule> printAccount(Map<String, byte[]> values) {
    return null;
//    if (unChecked) {
//      return null;
//    }
//    return tronApplicationContext.getBean(Manager.class).getWitnessController().getActiveWitnesses().stream()
//    .map(b -> b.toByteArray())
//    .map(b -> Maps.immutableEntry(ByteUtil.toHexString(b), tronApplicationContext.getBean(
//        AccountStore.class).get(b)))
//        .map(e -> Maps.immutableEntry(e.getKey(), e.getValue()))
//        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (k, v) -> k));
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
