package org.tron.core.db2;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingTronStore;
import org.tron.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.TronError;

@Slf4j
public class SnapshotManagerTest {

  private SnapshotManager revokingDatabase;
  private TronApplicationContext context;
  private Application appT;
  private TestRevokingTronStore tronDatabase;


  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_SnapshotManager_test"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = context.getBean(SnapshotManager.class);
    revokingDatabase.enable();
    tronDatabase = new TestRevokingTronStore("testSnapshotManager-test");
    revokingDatabase.add(tronDatabase.getRevokingDB());
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    tronDatabase.close();
    FileUtil.deleteDir(new File("output_SnapshotManager_test"));
    revokingDatabase.getCheckTmpStore().close();
    tronDatabase.close();
  }

  @Test
  public synchronized void testRefresh()
      throws BadItemException, ItemNotFoundException {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    List<Chainbase> dbList = revokingDatabase.getDbs();
    Map<String, Chainbase> dbMap = dbList.stream()
        .map(db -> Maps.immutableEntry(db.getDbName(), db))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        tronDatabase.put(protoCapsule.getData(), testProtoCapsule);
        BlockCapsule blockCapsule = new BlockCapsule(i, Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(), ByteString.EMPTY);
        dbMap.get("block").put(Longs.toByteArray(i), blockCapsule.getData());
        tmpSession.commit();
      }
    }

    revokingDatabase.flush();
    Assert.assertEquals(new ProtoCapsuleTest("refresh10".getBytes()),
        tronDatabase.get(protoCapsule.getData()));
  }

  @Test
  public synchronized void testClose() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("close".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("close" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        tronDatabase.put(protoCapsule.getData(), testProtoCapsule);
      }
    }
    Assert.assertEquals(null,
        tronDatabase.get(protoCapsule.getData()));

  }

  @Test
  public void testCheckError() {
    SnapshotManager manager = spy(new SnapshotManager(""));
    when(manager.getCheckpointList()).thenReturn(Arrays.asList("check1", "check2"));
    TronError thrown = Assert.assertThrows(TronError.class, manager::check);
    Assert.assertEquals(TronError.ErrCode.CHECKPOINT_VERSION, thrown.getErrCode());
  }

  @Test
  public void testFlushError() {
    SnapshotManager manager = spy(new SnapshotManager(""));
    manager.setUnChecked(false);
    when(manager.getCheckpointList()).thenReturn(Arrays.asList("check1", "check2"));
    when(manager.shouldBeRefreshed()).thenReturn(true);
    TronError thrown = Assert.assertThrows(TronError.class, manager::flush);
    Assert.assertEquals(TronError.ErrCode.DB_FLUSH, thrown.getErrCode());
  }
}
