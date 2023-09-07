package org.tron.core.db2;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Constructor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db2.core.Snapshot;
import org.tron.core.db2.core.SnapshotImpl;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;


public class SnapshotTest {

  private RevokingDbWithCacheNewValueTest.TestRevokingTronStore tronDatabase;
  private TronApplicationContext context;
  private Application appT;
  private SnapshotManager revokingDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);

    tronDatabase = new RevokingDbWithCacheNewValueTest.TestRevokingTronStore(
        "testSnapshotRoot-testMerge");
    revokingDatabase = context.getBean(SnapshotManager.class);
    revokingDatabase.enable();
    revokingDatabase.add(tronDatabase.getRevokingDB());
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("output_revokingStore_test"));

    tronDatabase.close();
    revokingDatabase.shutdown();
  }


  @Test
  public void testSnapshotIsRoot() {
    SnapshotRoot root = new SnapshotRoot(tronDatabase.getDb());
    assertEquals(true, Snapshot.isRoot(root));
  }

  @Test
  public void testSnapshotIsImpl() {
    SnapshotRoot root = new SnapshotRoot(tronDatabase.getDb());
    SnapshotImpl from = getSnapshotImplIns(root);
    assertEquals(true, Snapshot.isImpl(from));
  }

  private SnapshotImpl getSnapshotImplIns(Snapshot snapshot) {
    Class clazz = SnapshotImpl.class;
    try {
      Constructor constructor = clazz.getDeclaredConstructor(Snapshot.class);
      constructor.setAccessible(true);
      return (SnapshotImpl) constructor.newInstance(snapshot);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


}
