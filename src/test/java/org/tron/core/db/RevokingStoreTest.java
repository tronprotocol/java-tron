package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.DialogOptional;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.protos.Protocol.Witness;

@Slf4j
public class RevokingStoreTest {

  private RevokingDatabase revokingDatabase;

  @Before
  public void init() {
    revokingDatabase = RevokingStore.getInstance();
    revokingDatabase.enable();
    Args.setParam(new String[]{"--witness"}, Configuration.getByPath(Constant.NORMAL_CONF));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File("output_manager_test"));
  }

  @Test
  public void testUndo() {
    TestRevokingTronStore tronDatabase = new TestRevokingTronStore("test");
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFromUtf8(""), 100L, "");

    DialogOptional dialog = DialogOptional.of(revokingDatabase.buildDialog());
    try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
      tronDatabase.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
      Assert.assertFalse(tronDatabase.getDbSource().allKeys().isEmpty());
      tmpDialog.merge();
    } catch (RevokingStoreIllegalStateException e) {
      e.printStackTrace();
    }

    dialog.reset();

    Assert.assertTrue(tronDatabase.getDbSource().allKeys().isEmpty());
    tronDatabase.close();
  }

  @Test
  public void testPop() {
    TestRevokingTronStore tronDatabase = new TestRevokingTronStore("test");
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFromUtf8(""), 100L, "");

    try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
      tronDatabase.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
      Assert.assertFalse(tronDatabase.getDbSource().allKeys().isEmpty());
      tmpDialog.commit();
    } catch (RevokingStoreIllegalStateException e) {
      e.printStackTrace();
    }

    try {
      revokingDatabase.pop();
    } catch (RevokingStoreIllegalStateException e) {
      e.printStackTrace();
    }

    Assert.assertTrue(tronDatabase.getDbSource().allKeys().isEmpty());
    tronDatabase.close();
  }

  private static class TestProtoCapsule extends WitnessCapsule {


    public TestProtoCapsule(ByteString pubKey, String url) {
      super(pubKey, url);
    }

    public TestProtoCapsule(Witness witness) {
      super(witness);
    }

    public TestProtoCapsule(ByteString address) {
      super(address);
    }

    public TestProtoCapsule(ByteString address, long voteCount, String url) {
      super(address, voteCount, url);
    }

    public TestProtoCapsule(byte[] data) {
      super(data);
    }
  }

  private static class TestRevokingTronStore extends WitnessStore {

    protected TestRevokingTronStore(String dbName) {
      super(dbName);
    }
  }
}
