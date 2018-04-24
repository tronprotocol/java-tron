package org.tron.core.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.DialogOptional;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingStoreTest {

  private AbstractRevokingStore revokingDatabase;

  @Before
  public void init() {
    revokingDatabase = new TestRevokingTronDatabase();
    revokingDatabase.enable();
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testUndo() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingTronStore tronDatabase = new TestRevokingTronStore(
        "testrevokingtronstore-testUndo", revokingDatabase);

    DialogOptional dialog = DialogOptional.instance().setValue(revokingDatabase.buildDialog());
    for (int i = 0; i < 10; i++) {
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("undo" + i).getBytes());
      try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
        tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertFalse(tronDatabase.getDbSource().allKeys().isEmpty());
        Assert.assertEquals(revokingDatabase.getStack().size(), 2);
        tmpDialog.merge();
        Assert.assertEquals(revokingDatabase.getStack().size(), 1);
      }
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 1);

    dialog.reset();

    Assert.assertTrue(revokingDatabase.getStack().isEmpty());
    Assert.assertTrue(tronDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
    tronDatabase.close();
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingTronStore tronDatabase = new TestRevokingTronStore(
        "testrevokingtronstore-testPop", revokingDatabase);

    for (int i = 1; i < 11; i++) {
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("pop" + i).getBytes());
      try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
        tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertFalse(tronDatabase.getDbSource().allKeys().isEmpty());
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 1);
        tmpDialog.commit();
        Assert.assertEquals(revokingDatabase.getStack().size(), i);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, tronDatabase.getDbSource().allKeys().size());
      Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
    }

    Assert.assertTrue(tronDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
  }

  @Test
  public void shutdown() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingTronStore tronDatabase = new TestRevokingTronStore(
        "testrevokingtronstore-shutdown", revokingDatabase);

    List<TestProtoCapsule> capsules = new ArrayList<>();
    for (int i = 1; i < 11; i++) {
      revokingDatabase.buildDialog();
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("test" + i).getBytes());
      capsules.add(testProtoCapsule);
      tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
      Assert.assertFalse(tronDatabase.getDbSource().allKeys().isEmpty());
      Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
      Assert.assertEquals(revokingDatabase.getStack().size(), i);
    }

    for (TestProtoCapsule capsule : capsules) {
      logger.info(new String(capsule.getData()));
      Assert.assertEquals(capsule, tronDatabase.get(capsule.getData()));
    }

    revokingDatabase.shutdown();

    for (TestProtoCapsule capsule : capsules) {
      logger.info(tronDatabase.get(capsule.getData()).toString());
      Assert.assertEquals(null, tronDatabase.get(capsule.getData()).getData());
    }

    Assert.assertTrue(tronDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
    tronDatabase.close();

  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class TestProtoCapsule implements ProtoCapsule<Object> {

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
      return "TestProtoCapsule{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }

  private static class TestRevokingTronStore extends TronStoreWithRevoking<TestProtoCapsule> {

    protected TestRevokingTronStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }

    @Override
    public TestProtoCapsule get(byte[] key) {
      return new TestProtoCapsule(dbSource.getData(key));
    }

    @Override
    public boolean has(byte[] key) {
      return dbSource.getData(key) != null;
    }
  }

  private static class TestRevokingTronDatabase extends AbstractRevokingStore {

  }
}
