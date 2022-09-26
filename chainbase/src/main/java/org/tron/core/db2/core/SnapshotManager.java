package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.error.TronDBException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.TronDatabase;
import org.tron.core.db2.ISession;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.core.store.CheckPointV2Store;
import org.tron.core.store.CheckTmpStore;

@Slf4j(topic = "DB")
public class SnapshotManager implements RevokingDatabase {

  public static final int DEFAULT_MAX_FLUSH_COUNT = 500;
  public static final int DEFAULT_MIN_FLUSH_COUNT = 1;
  private static final int DEFAULT_STACK_MAX_SIZE = 256;
  private static final long ONE_MINUTE_MILLS = 60*1000L;
  private static final String CHECKPOINT_V2_DIR = "checkpoint";
  @Getter
  private List<Chainbase> dbs = new ArrayList<>();
  @Getter
  private int size = 0;
  private AtomicInteger maxSize = new AtomicInteger(DEFAULT_STACK_MAX_SIZE);

  private boolean disabled = true;
  // for test
  @Getter
  private int activeSession = 0;
  // for test
  @Setter
  private boolean unChecked = true;

  private volatile int flushCount = 0;

  private Thread exitThread;
  private volatile boolean  hitDown;

  private Map<String, ListeningExecutorService> flushServices = new HashMap<>();

  private ScheduledExecutorService pruneCheckpointThread = null;

  @Autowired
  @Setter
  @Getter
  private CheckTmpStore checkTmpStore;

  @Setter
  private volatile int maxFlushCount = DEFAULT_MIN_FLUSH_COUNT;

  private int checkpointVersion = 1;   // default v1

  private long currentBlockNum = -1;

  public synchronized long getCurrentBlockNum() {
    return currentBlockNum;
  }

  public synchronized void setCurrentBlockNum(long blockNum) {
    currentBlockNum = blockNum;
  }

  public SnapshotManager(String checkpointPath) {
  }

  @PostConstruct
  public void init() {
    checkpointVersion = CommonParameter.getInstance().getStorage().getCheckpointVersion();
    // prune checkpoint
    if (isV2Open()) {
      pruneCheckpointThread = Executors.newSingleThreadScheduledExecutor();
      pruneCheckpointThread.scheduleWithFixedDelay(() -> {
        try {
          if (!unChecked) {
            pruneCheckpoint();
          }
        } catch (Throwable t) {
          logger.error("Exception in prune checkpoint", t);
        }
      }, 10000, 3600, TimeUnit.MILLISECONDS);
    }
    exitThread =  new Thread(() -> {
      LockSupport.park();
      // to Guarantee Some other thread invokes unpark with the current thread as the target
      if (hitDown) {
        System.exit(1);
      }
    });
    exitThread.setName("exit-thread");
    exitThread.start();
  }

  public static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
    return new String(value);
  }

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

    if (size > maxSize.get() && !hitDown) {
      flushCount = flushCount + (size - maxSize.get());
      updateSolidity(size - maxSize.get());
      size = maxSize.get();
      flush();
    }

    advance();
    ++activeSession;
    return new Session(this, disableOnExit);
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor) {
    dbs.forEach(db -> db.setCursor(cursor));
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor, long offset) {
    dbs.forEach(db -> db.setCursor(cursor, offset));
  }

  @Override
  public void add(IRevokingDB db) {
    Chainbase revokingDB = (Chainbase) db;
    dbs.add(revokingDB);
    flushServices.put(revokingDB.getDbName(),
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
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
      throw new RevokingStoreIllegalStateException(activeSession);
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
      throw new RevokingStoreIllegalStateException(activeSession);
    }

    if (size <= 0) {
      return;
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
      throw new RevokingStoreIllegalStateException(activeSession);
    }

    --activeSession;
  }

  public synchronized void pop() {
    if (activeSession != 0) {
      throw new RevokingStoreIllegalStateException(
          String.format("activeSession has to be equal 0, current %d", activeSession));
    }

    if (size <= 0) {
      throw new RevokingStoreIllegalStateException(
          String.format("there is not snapshot to be popped, current: %d", size));
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

  public int getMaxSize() {
    return maxSize.get();
  }

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize.set(maxSize);
  }

  public synchronized void disable() {
    disabled = true;
  }

  @Override
  public void shutdown() {
    logger.info("******** Begin to pop revokingDb. ********");
    logger.info("******** Before revokingDb size: {}.", size);
    checkTmpStore.close();
    logger.info("******** End to pop revokingDb. ********");
    if (pruneCheckpointThread != null) {
      pruneCheckpointThread.shutdown();
    }
  }

  public void updateSolidity(int hops) {
    for (int i = 0; i < hops; i++) {
      for (Chainbase db : dbs) {
        db.getHead().updateSolidity();
      }
    }
  }

  private boolean shouldBeRefreshed() {
    return flushCount >= maxFlushCount;
  }

  private void refresh() {
    List<ListenableFuture<?>> futures = new ArrayList<>(dbs.size());
    for (Chainbase db : dbs) {
      futures.add(flushServices.get(db.getDbName()).submit(() -> refreshOne(db)));
    }
    Future<?> future = Futures.allAsList(futures);
    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TronDBException(e);
    } catch (ExecutionException e) {
      throw new TronDBException(e);
    }
  }

  private void refreshOne(Chainbase db) {
    if (Snapshot.isRoot(db.getHead())) {
      return;
    }

    List<Snapshot> snapshots = new ArrayList<>();

    SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
    Snapshot next = root;
    for (int i = 0; i < flushCount; ++i) {
      next = next.getNext();
      snapshots.add(next);
    }
    if (!db.getDbName().equals("market_pair_price_to_order")
        && !db.getDbName().equals("witness")
        && !db.getDbName().equals("votes")
        && !db.getDbName().equals("recent-transaction")
        && !db.getDbName().equals("trans-cache")
        && !db.getDbName().equals("witness_schedule")) {
         next.put("block_number".getBytes(), Longs.toByteArray(getCurrentBlockNum()));
         logger.info("checkpoint add debug info, db: {}, blocknumber: {}", db.getDbName(), Longs.toByteArray(getCurrentBlockNum()));
    }

    root.merge(snapshots);

    root.resetSolidity();
    if (db.getHead() == next) {
      db.setHead(root);
    } else {
      next.getNext().setPrevious(root);
      root.setNext(next.getNext());
    }
  }

  public void flush() {
    if (unChecked) {
      return;
    }

    if (shouldBeRefreshed()) {
      try {
        long start = System.currentTimeMillis();
        if (!isV2Open()) {
          deleteCheckpoint();
        }
        createCheckpoint();

        long checkPointEnd = System.currentTimeMillis();
        refresh();
        logger.info("Flush: {}, flush count: {}, create checkpoint: {} ms, cost: {} ms, refresh cost: {} ms.",
            getCurrentBlockNum(),
            flushCount,
            System.currentTimeMillis() - start,
            checkPointEnd - start,
            System.currentTimeMillis() - checkPointEnd
        );
        flushCount = 0;
      } catch (TronDBException e) {
        logger.error(" Find fatal error, program will be exited soon.", e);
        hitDown = true;
        LockSupport.unpark(exitThread);
      }
    }
  }

  private void createCheckpoint() {
    long numberInPro = -1;
    long numberInblock = -1;
    TronDatabase<byte[]> checkPointStore = null;
    boolean syncFlag;
    try {
      Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
      for (Chainbase db : dbs) {
        Snapshot head = db.getHead();
        if (Snapshot.isRoot(head)) {
          return;
        }

        String dbName = db.getDbName();
        Snapshot next = head.getRoot();
        for (int i = 0; i < flushCount; ++i) {
          next = next.getNext();
          SnapshotImpl snapshot = (SnapshotImpl) next;
          DB<Key, Value> keyValueDB = snapshot.getDb();
          for (Map.Entry<Key, Value> e : keyValueDB) {
            Key k = e.getKey();
            Value v = e.getValue();
            batch.put(WrappedByteArray.of(Bytes.concat(simpleEncode(dbName), k.getBytes())),
                WrappedByteArray.of(v.encode()));
            if (db.getDbName().equals("block")) {
              numberInblock = new BlockCapsule(v.getBytes()).getNum();
             // logger.info("checkpoint check blocknumber, numberInblock: {}", numberInblock);
            }
            if (db.getDbName().equals("properties")) {
              if (Arrays.equals(k.getBytes(), "latest_block_header_number".getBytes())) {
                numberInPro = Longs.fromByteArray(v.getBytes());
              //  logger.info("checkpoint check blocknumber, numberInPro: {}", numberInPro);
              }
            }
            if (Arrays.equals("block_number".getBytes(), k.getBytes())) {
              logger.error("checkpoint should not contain 'blocknumber', db:{}, number: {}",
                  keyValueDB.getDbName(), Longs.fromByteArray(v.getBytes()));
            }
          }
        }
      }
      if (numberInblock != numberInPro) {
        logger.error("checkpoint err, fatal numberInblock != numberInPro, {}, {}", numberInblock, numberInPro);
        System.exit(-1);
      }
      setCurrentBlockNum(numberInblock);
      if (getCurrentBlockNum() == -1) {
        throw new TronDBException("create checkpoint failed, block num should not be -1");
      }
      if (isV2Open()) {
        String dbName = System.currentTimeMillis()+"_"+getCurrentBlockNum();
        checkPointStore = getCheckpointDB(dbName);
        syncFlag = CommonParameter.getInstance().getStorage().isCheckpointSync();
      } else {
        checkPointStore = checkTmpStore;
        syncFlag = CommonParameter.getInstance().getStorage().isDbSync();
      }

      checkPointStore.getDbSource().updateByBatch(batch.entrySet().stream()
              .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
              .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll),
          WriteOptionsWrapper.getInstance().sync(syncFlag));

    } catch (Exception e) {
      throw new TronDBException(e);
    } finally {
      if (isV2Open() && checkPointStore != null) {
        checkPointStore.close();
      }
    }
  }

  private TronDatabase<byte[]> getCheckpointDB(String dbName) {
    return new CheckPointV2Store(CHECKPOINT_V2_DIR+"/"+dbName);
  }

  private List<String> getCheckpointList() {
    String dbPath = Paths.get(StorageUtils.getOutputDirectoryByDbName(CHECKPOINT_V2_DIR),
        CommonParameter.getInstance().getStorage().getDbDirectory()).toString();
    File file = new File(Paths.get(dbPath, CHECKPOINT_V2_DIR).toString());
    if (file.exists() && file.isDirectory()) {
      String[] subDirs = file.list();
      if (subDirs != null) {
        return Arrays.stream(subDirs).sorted().collect(Collectors.toList());
      }
    }
    return null;
  }

  private void deleteCheckpoint() {
    checkTmpStore.reset();
  }

  private void pruneCheckpoint() {
    if (unChecked) {
      return;
    }
    List<String> cpList = getCheckpointList();
    if (cpList == null) {
      return;
    }
    if (cpList.size() < 3) {
      return;
    }
    for (String cp: cpList.subList(0, cpList.size()-3)) {
      long timestamp = Long.parseLong(cp.split("_")[0]);
      long blockNum = Long.parseLong(cp.split("_")[1]);
      if (System.currentTimeMillis() - timestamp < ONE_MINUTE_MILLS*2) {
        break;
      }
      String checkpointPath = Paths.get(StorageUtils.getOutputDirectoryByDbName(CHECKPOINT_V2_DIR),
          CommonParameter.getInstance().getStorage().getDbDirectory(), CHECKPOINT_V2_DIR).toString();
      if (!FileUtil.recursiveDelete(Paths.get(checkpointPath, cp).toString())) {
        logger.error("checkpoint prune failed, blockNum: {}", blockNum);
        return;
      }
      logger.info("checkpoint prune success, blockNum: {}", blockNum);
    }
  }

  // ensure run this method first after process start.
  @Override
  public void check() {
    if (!isV2Open()) {
      List<String> cpList = getCheckpointList();
      if (cpList != null && cpList.size() != 0) {
        logger.error("checkpoint check failed, can't convert checkpoint from v2 to v1");
        System.exit(-1);
      }
      checkV1();
    } else {
      checkV2();
    }
  }

  private void checkV1() {
    for (Chainbase db: dbs) {
      if (!Snapshot.isRoot(db.getHead())) {
        throw new IllegalStateException("First check.");
      }
    }
    recover(checkTmpStore);
    unChecked = false;
  }

  private void checkV2() {
    logger.info("checkpoint version: {}", CommonParameter.getInstance().getStorage().getCheckpointVersion());
    logger.info("checkpoint sync: {}", CommonParameter.getInstance().getStorage().isCheckpointSync());
    List<String> cpList = getCheckpointList();
    if (cpList == null || cpList.size() == 0) {
      logger.info("checkpoint size is 0, using v1 recover");
      checkV1();
      deleteCheckpoint();
      return;
    }

    long minBlockNum = Long.MAX_VALUE, maxBlockNum = -1;
    for (Chainbase db : dbs) {
      if (!Snapshot.isRoot(db.getHead())) {
        throw new IllegalStateException("first check.");
      }
      if (db.getDbName().equals("market_pair_price_to_order")
          || db.getDbName().equals("witness")
          || db.getDbName().equals("votes")
          || db.getDbName().equals("recent-transaction")
          || db.getDbName().equals("trans-cache")
          || db.getDbName().equals("witness_schedule")) {
        continue;
      }
      try {
        long blockNumber = Longs.fromByteArray(db.get("block_number".getBytes()));
        logger.info("store: {}, block numer: {}", db.getDbName(), blockNumber);
        minBlockNum = Math.min(minBlockNum, blockNumber);
        maxBlockNum = Math.max(maxBlockNum, blockNumber);
      } catch (ItemNotFoundException e) {
        logger.error("check failed, dbs meta data corrupt, can not get current block number，" +
            " db: {}", db.getDbName());
        System.exit(-1);
      }
    }

    if (minBlockNum == Long.MAX_VALUE || maxBlockNum == -1) {
      logger.error("check failed, dbs current block number illegal, minblockNum:{}, maxBlockNum: {}", minBlockNum, maxBlockNum);
      System.exit(-1);
    }

    List<Long> sortedAllKeys = cpList.stream().map(entry -> Long.parseLong(entry.split("_")[1]))
        .sorted().collect(Collectors.toList());
    if (minBlockNum < sortedAllKeys.get(0) || maxBlockNum > sortedAllKeys.get(sortedAllKeys.size()-1)) {
      logger.error("check failed, checkpoint incomplete，checkpoint start number: {}, end number: {}, " +
              "dbs start number: {}, end number: {}",
          sortedAllKeys.get(0), sortedAllKeys.get(sortedAllKeys.size()-1), minBlockNum, maxBlockNum);
      System.exit(-1);
    }

    for (String cp: cpList) {
      TronDatabase<byte[]> checkPointV2Store = getCheckpointDB(cp);
      recover(checkPointV2Store);
      checkPointV2Store.close();
    }
    logger.info("checkpoint v2 recover success");
    unChecked = false;
  }

  private void recover(TronDatabase<byte[]> tronDatabase) {
    Map<String, Chainbase> dbMap = dbs.stream()
        .map(db -> Maps.immutableEntry(db.getDbName(), db))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    advance();
    for (Map.Entry<byte[], byte[]> e: tronDatabase.getDbSource()) {
      byte[] key = e.getKey();
      byte[] value = e.getValue();
      String db = simpleDecode(key);
      if (dbMap.get(db) == null) {
        logger.error("checkpoint db not exist, dbname: {}, key: {}, value: {}",
            db, ByteArray.toHexString(key), ByteArray.toHexString(value));
        continue;
      }
      byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);
      byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
      if ("properties".equals(db)) {
        if (Arrays.equals("block_number".getBytes(), realKey)) {
          logger.info("checkpoint properties recover, block number: {}", Longs.fromByteArray(realValue));
        }
      }
      if (realValue != null) {
        dbMap.get(db).getHead().put(realKey, realValue);
      } else {
        dbMap.get(db).getHead().remove(realKey);
      }
    }

    dbs.forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    retreat();
    logger.info("checkpoint v2 recover: {}", tronDatabase.getDbName());
  }

  private boolean isV2Open() {
    return checkpointVersion == 2;
  }

  private byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  @Slf4j(topic = "DB")
  @Getter // only for unit test
  public static class Session implements ISession {

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
        logger.error("Revoke database error.", e);
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
        logger.error("Revoke database error.", e);
        throw new RevokingStoreIllegalStateException(e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }
  }

}
