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

public class RecentBlockStoreTest extends BaseTest {

  @Resource
  private RecentBlockStore recentBlockStore;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath()
            },
            Constant.TEST_CONF
    );
  }

  private BlockCapsule getBlockCapsule() {
    long number = 1;
    return new BlockCapsule(number,
            Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(),
            ByteString.EMPTY);
  }

  @Test
  public void testPut() {
    BlockCapsule blockCapsule = getBlockCapsule();
    byte[] key = ByteArray.subArray(
            ByteArray.fromLong(blockCapsule.getNum()), 6, 8);
    recentBlockStore.put(key,
            new BytesCapsule(ByteArray.subArray(blockCapsule
                    .getBlockId().getBytes(),
                    8,
                    16)));

    Assert.assertTrue(recentBlockStore.has(key));
  }

  @Test
  public void testGet() throws ItemNotFoundException {
    BlockCapsule blockCapsule = getBlockCapsule();
    byte[] key = ByteArray.subArray(
            ByteArray.fromLong(blockCapsule.getNum()), 6, 8);
    BytesCapsule value = new BytesCapsule(ByteArray
            .subArray(blockCapsule.getBlockId().getBytes(),
            8,
            16));
    recentBlockStore.put(key, value);

    BytesCapsule bytesCapsule = recentBlockStore.get(key);
    Assert.assertNotNull(bytesCapsule);
    Assert.assertArrayEquals(value.getData(), bytesCapsule.getData());
  }

  @Test
  public void testDelete() {
    BlockCapsule blockCapsule = getBlockCapsule();
    byte[] key = ByteArray.subArray(
            ByteArray.fromLong(blockCapsule.getNum()), 6, 8);
    recentBlockStore.put(key,
            new BytesCapsule(ByteArray.subArray(blockCapsule
                            .getBlockId().getBytes(),
                    8,
                    16)));
    recentBlockStore.delete(key);
    Assert.assertFalse(recentBlockStore.has(key));
  }
}
