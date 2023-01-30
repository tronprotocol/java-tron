package org.tron.core.db2;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Iterator;
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
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.SnapshotManager;

@Slf4j
public class CheckpointV2Test {

  private SnapshotManager revokingDatabase;
  private TronApplicationContext context;
  private Application appT;
  private TestRevokingTronStore tronDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_SnapshotManager_test"},
        Constant.TEST_CONF);
    Args.getInstance().getStorage().setCheckpointVersion(2);
    Args.getInstance().getStorage().setCheckpointSync(true);
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
  public void testCheckpointV2() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(0);
    List<Chainbase> dbList = revokingDatabase.getDbs();
    Map<String, Chainbase> dbMap = dbList.stream()
        .map(db -> Maps.immutableEntry(db.getDbName(), db))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    for (int i = 1; i <= 5; i++) {
      BlockCapsule blockCapsule = new BlockCapsule(i, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), ByteString.EMPTY);
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        dbMap.get("block").put(Longs.toByteArray(i), blockCapsule.getData());
        tmpSession.commit();
      }
    }
    revokingDatabase.buildSession();

    Iterator<Map.Entry<byte[], byte[]>> iterator = dbMap.get("block").iterator();
    Sha256Hash preDbHash = Sha256Hash.ZERO_HASH;
    while (iterator.hasNext()) {
      Map.Entry<byte[], byte[]> entry = iterator.next();
      byte[] hashBytes = Bytes.concat(entry.getKey(), entry.getValue());
      preDbHash = Sha256Hash.of(true, Bytes.concat(preDbHash.getBytes(), hashBytes));
    }

    revokingDatabase.check();
    revokingDatabase.buildSession();

    Iterator<Map.Entry<byte[], byte[]>> iterator2 = dbMap.get("block").iterator();
    Sha256Hash afterDbHash = Sha256Hash.ZERO_HASH;
    while (iterator2.hasNext()) {
      Map.Entry<byte[], byte[]> entry = iterator2.next();
      byte[] hashBytes = Bytes.concat(entry.getKey(), entry.getValue());
      afterDbHash = Sha256Hash.of(true, Bytes.concat(afterDbHash.getBytes(), hashBytes));
    }

    Assert.assertEquals(0, preDbHash.compareTo(afterDbHash));
  }
}
