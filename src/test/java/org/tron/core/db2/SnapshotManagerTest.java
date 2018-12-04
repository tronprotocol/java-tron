package org.tron.core.db2;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingTronStore;
import org.tron.core.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.tron.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.tron.core.db2.core.ISession;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public class SnapshotManagerTest {

  private SnapshotManager revokingDatabase;
  private TronApplicationContext context;
  private Application appT;
  private TestRevokingTronStore tronDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    tronDatabase = new TestRevokingTronStore("testSnapshotManager-test");
    revokingDatabase.add(tronDatabase.getRevokingDB());
    LevelDbDataSourceImpl tmpLevelDbDataSource  =
      new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("testSnapshotManager-tmp"), "testSnapshotManagerTmp");
    tmpLevelDbDataSource.initDB();
    revokingDatabase.setTmpLevelDbDataSource(tmpLevelDbDataSource);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    tronDatabase.close();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
    revokingDatabase.getTmpLevelDbDataSource().closeDB();
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
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        tronDatabase.put(protoCapsule.getData(), testProtoCapsule);
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
      try (ISession _ = revokingDatabase.buildSession()) {
        tronDatabase.put(protoCapsule.getData(), testProtoCapsule);
      }
    }
    Assert.assertEquals(null,
        tronDatabase.get(protoCapsule.getData()));

  }
}
