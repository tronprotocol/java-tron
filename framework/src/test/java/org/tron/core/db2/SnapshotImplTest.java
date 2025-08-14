package org.tron.core.db2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db2.core.Snapshot;
import org.tron.core.db2.core.SnapshotImpl;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;

public class SnapshotImplTest {
  private RevokingDbWithCacheNewValueTest.TestRevokingTronStore tronDatabase;
  private TronApplicationContext context;
  private Application appT;
  private SnapshotManager revokingDatabase;
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void init() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
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
    tronDatabase.close();
  }

  /**
   * linklist is: from -> root
   * root:key1=>value1, key2=>value2
   * from:key3=>value3, key4=>value4
   * after construct, getSnapshotImplIns(root);
   * from: key1=>value1, key2=>value2, key3=>value3, key4=>value4
   * from: get key1 or key2, traverse 0 times
   */
  @Test
  public void testMergeRoot() {
    // linklist is: from -> root
    SnapshotRoot root = new SnapshotRoot(tronDatabase.getDb());
    //root.setOptimized(true);

    root.put("key1".getBytes(), "value1".getBytes());
    root.put("key2".getBytes(), "value2".getBytes());
    SnapshotImpl from = getSnapshotImplIns(root);
    from.put("key3".getBytes(), "value3".getBytes());
    from.put("key4".getBytes(), "value4".getBytes());

    byte[] s1 = from.get("key1".getBytes());
    assertEquals(new String("value1".getBytes()), new String(s1));
    byte[] s2 = from.get("key2".getBytes());
    assertEquals(new String("value2".getBytes()), new String(s2));
  }

  /**
   * linklist is: from2 -> from -> root
   * root:
   * from:key1=>value1, key2=>value2
   * from2:key3=>value3,key4=>value4
   * before merge: from2.mergeAhead(from);
   * from2: get key1 or key2, traverse 1 times
   * after merge
   * from2:key1=>value1, key2=>value2, value3=>value3,key4=>value4
   * from2: get key1 or key2, traverse 0 times
   *
   */
  @Test
  public void testMergeAhead() {

    // linklist is: from2 -> from -> root
    SnapshotRoot root = new SnapshotRoot(tronDatabase.getDb());
    SnapshotImpl from = getSnapshotImplIns(root);
    from.put("key1".getBytes(), "value1".getBytes());
    from.put("key2".getBytes(), "value2".getBytes());

    SnapshotImpl from2 = getSnapshotImplIns(from);
    from2.put("key3".getBytes(), "value3".getBytes());
    from2.put("key4".getBytes(), "value4".getBytes());

    /*
    // before merge  get data in from is success，traverse 0 times
    byte[] s1 = from.get("key1".getBytes());
    assertEquals(new String("value1".getBytes()), new String(s1));
    byte[] s2 = from.get("key2".getBytes());
    assertEquals(new String("value2".getBytes()), new String(s2));
    // before merge  get data in from2 is success， traverse 0 times
    byte[] s3 = from2.get("key3".getBytes());
    assertEquals(new String("value3".getBytes()), new String(s3));
    byte[] s4 = from2.get("key4".getBytes());
    assertEquals(new String("value4".getBytes()), new String(s4));
     */

    // before merge from2 get data is success， traverse 1 times
    byte[] s11 = from2.get("key1".getBytes());
    assertEquals(new String("value1".getBytes()), new String(s11));
    byte[] s12 = from2.get("key2".getBytes());
    assertEquals(new String("value2".getBytes()), new String(s12));
    // this can not get key3 and key4
    assertNull(from.get("key3".getBytes()));
    assertNull(from.get("key4".getBytes()));

    // do mergeAhead
    from2.mergeAhead(from);
    /*
    // after merge  get data in from is success， traverse 0 times
    s1 = from.get("key1".getBytes());
    assertEquals(new String("value1".getBytes()), new String(s1));
    s2 = from.get("key2".getBytes());
    assertEquals(new String("value2".getBytes()), new String(s2));

    // after merge get data in from2 is success， traverse 0 times
    s3 = from2.get("key3".getBytes());
    assertEquals(new String("value3".getBytes()), new String(s3));
    s4 = from2.get("key4".getBytes());
    assertEquals(new String("value4".getBytes()), new String(s4));
     */

    // after merge from2 get data is success， traverse 0 times
    byte[] s1 = from2.get("key1".getBytes());
    assertEquals(new String("value1".getBytes()), new String(s1));
    byte[] s2 = from2.get("key2".getBytes());
    assertEquals(new String("value2".getBytes()), new String(s2));

    // this can not get key3 and key4
    assertNull(from.get("key3".getBytes()));
    assertNull(from.get("key4".getBytes()));
  }

  /**
   * from: key1=>value1, key2=>value2, key3=>value31
   * from2: key3=>value32,key4=>value4
   * after merge: from2.mergeAhead(from);
   * from2: key1=>value1, key2=>value2, key3=>value32, key4=>value4
   */
  @Test
  public void testMergeOverride() {
    // linklist is: from2 -> from -> root
    SnapshotRoot root = new SnapshotRoot(tronDatabase.getDb());
    SnapshotImpl from = getSnapshotImplIns(root);
    from.put("key1".getBytes(), "value1".getBytes());
    from.put("key2".getBytes(), "value2".getBytes());
    from.put("key3".getBytes(), "value31".getBytes());

    SnapshotImpl from2 = getSnapshotImplIns(from);
    from2.put("key3".getBytes(), "value32".getBytes());
    from2.put("key4".getBytes(), "value4".getBytes());
    // do mergeAhead
    from2.mergeAhead(from);

    // after merge from2 get data is success， traverse 0 times
    byte[] s1 = from2.get("key1".getBytes());
    assertEquals(new String("value1".getBytes()), new String(s1));
    byte[] s2 = from2.get("key2".getBytes());
    assertEquals(new String("value2".getBytes()), new String(s2));
    byte[] s3 = from2.get("key3".getBytes());
    assertEquals(new String("value32".getBytes()), new String(s3));
    byte[] s4 = from2.get("key4".getBytes());
    assertEquals(new String("value4".getBytes()), new String(s4));
  }

  /**
   * The constructor of SnapshotImpl is not public
   * so reflection is used to construct the object here.
   */
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
