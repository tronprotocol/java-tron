package org.tron.core.db2;

import java.io.File;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.SessionOptional;
import org.tron.core.Constant;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingTronStore;
import org.tron.core.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.tron.core.db2.core.SnapshotManager;

public class SnapshotRootTest {

  private TestRevokingTronStore tronDatabase;
  private TronApplicationContext context;
  private Application appT;
  private SnapshotManager revokingDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testRemove() {
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("test".getBytes());
    tronDatabase = new TestRevokingTronStore("testSnapshotRoot-testRemove");
    tronDatabase.put("test".getBytes(), testProtoCapsule);
    Assert.assertEquals(testProtoCapsule, tronDatabase.get("test".getBytes()));

    tronDatabase.delete("test".getBytes());
    Assert.assertEquals(null, tronDatabase.get("test".getBytes()));
    tronDatabase.close();
  }

  @Test
  public synchronized void testMerge() {
    tronDatabase = new TestRevokingTronStore("testSnapshotRoot-testMerge");
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    revokingDatabase.add(tronDatabase.getRevokingDB());

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("merge".getBytes());
    tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.getDbs().forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    dialog.reset();
    Assert.assertEquals(tronDatabase.get(testProtoCapsule.getData()), testProtoCapsule);

    tronDatabase.close();
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class ProtoCapsuleTest implements ProtoCapsule<Object> {
    private byte[] value;

    @Override
    public byte[] getData() {
      return value;
    }

    @Override
    public Object getInstance() {
      return value;
    }

    @Override
    public String toString() {
      return "ProtoCapsuleTest{"
        + "value=" + Arrays.toString(value)
        + ", string=" + (value == null ? "" : new String(value))
        + '}';
    }
  }
}
