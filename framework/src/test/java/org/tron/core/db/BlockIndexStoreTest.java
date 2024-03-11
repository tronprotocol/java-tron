package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;

public class BlockIndexStoreTest extends BaseTest {

  @Resource
  private BlockIndexStore blockIndexStore;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath()
            },
            Constant.TEST_CONF
    );
  }

  private BlockCapsule getBlockCapsule(long number) {
    return new BlockCapsule(number, Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(), ByteString.EMPTY);
  }

  @Test
  public void testPut() {
    BlockCapsule blockCapsule = getBlockCapsule(1);
    blockIndexStore.put(blockCapsule.getBlockId());
    byte[] key = ByteArray.fromLong(blockCapsule.getBlockId().getNum());
    Assert.assertTrue(blockIndexStore.has(key));
  }

  @Test
  public void testGet() throws ItemNotFoundException {
    BlockCapsule blockCapsule = getBlockCapsule(1);
    blockIndexStore.put(blockCapsule.getBlockId());
    byte[] key = ByteArray.fromLong(blockCapsule.getBlockId().getNum());
    BytesCapsule bytesCapsule = blockIndexStore.get(key);
    Assert.assertNotNull(bytesCapsule);
  }

  @Test
  public void testDelete() throws ItemNotFoundException {
    BlockCapsule blockCapsule = getBlockCapsule(1);
    blockIndexStore.put(blockCapsule.getBlockId());
    byte[] key = ByteArray.fromLong(blockCapsule.getBlockId().getNum());
    BytesCapsule bytesCapsule = blockIndexStore.get(key);
    Assert.assertNotNull(bytesCapsule);

    blockIndexStore.delete(key);
    BytesCapsule capsule = blockIndexStore.getUnchecked(key);
    Assert.assertNull(capsule.getData());
  }
}
