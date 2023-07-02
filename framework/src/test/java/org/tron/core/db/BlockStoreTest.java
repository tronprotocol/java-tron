package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

import javax.annotation.Resource;

@Slf4j
public class BlockStoreTest extends BaseTest {

  @Resource
  private BlockStore blockStore;

  private static String BLOCK_HASH = "000000000000000130504843d49b38045fb4d362e319a0c0374f3860da0521b4";
  static {
    dbPath = "output-blockStore-test";
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
  }

  @Before
  public void createBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(
            1,
            Sha256Hash.wrap(ByteString.copyFrom(
                    ByteArray.fromHexString(
                            "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
            1,
            ByteString.copyFromUtf8("testAddress"));
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
  }

  @Test
  public void testCreateBlockStore() {
  }

  @Test
  public void testGetBlock() {
    try {
      BlockCapsule blockCapsule = blockStore.get(Hex.decode(BLOCK_HASH));
      Assert.assertEquals(blockCapsule.getBlockId().toString(), BLOCK_HASH);
    } catch (ItemNotFoundException | BadItemException e) {
      e.printStackTrace();
    }
  }
}
