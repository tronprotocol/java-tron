package org.tron.core.db;

import java.io.File;
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
    TestProtoCapsule testProtoCapsule = new TestProtoCapsule();

    DialogOptional dialog = DialogOptional.instance().setValue(revokingDatabase.buildDialog());
    for (int i = 0; i < 10; i++) {
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
    TestProtoCapsule testProtoCapsule = new TestProtoCapsule();

    for (int i = 1; i < 11; i++) {
      try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
        tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertFalse(tronDatabase.getDbSource().allKeys().isEmpty());
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 1);
        tmpDialog.commit();
        Assert.assertEquals(revokingDatabase.getStack().size(), i);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
      }
    }

    try {
      revokingDatabase.pop();
    } catch (RevokingStoreIllegalStateException e) {
      logger.debug(e.getMessage(), e);
    }

    Assert.assertTrue(tronDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getStack().size(), 9);
    tronDatabase.close();
  }

  private static class TestProtoCapsule implements ProtoCapsule<Object> {

    @Override
    public byte[] getData() {
      return new byte[0];
    }

    @Override
    public Object getInstance() {
      return null;
    }
  }

  private static class TestRevokingTronStore extends TronStoreWithRevoking<TestProtoCapsule> {

    protected TestRevokingTronStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }

    @Override
    public TestProtoCapsule get(byte[] key) {
      return null;
    }

    @Override
    public boolean has(byte[] key) {
      return false;
    }
  }

  private static class TestRevokingTronDatabase extends AbstractRevokingStore {

  }
}
