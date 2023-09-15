package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.IncrementalMerkleTreeStore;

public class IncrementalMerkleTreeStoreTest extends BaseTest {

  private static final byte[] incrementalMerkleTreeData = {10, 0};

  @Resource
  private IncrementalMerkleTreeStore incrementalMerkleTreeStore;

  static {
    Args.setParam(
        new String[] {
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    incrementalMerkleTreeStore.put("Address1".getBytes(), new IncrementalMerkleTreeCapsule(
        incrementalMerkleTreeData));
  }

  @Test
  public void testGet() throws Exception {
    final IncrementalMerkleTreeCapsule result =
        incrementalMerkleTreeStore.get("Address1".getBytes());
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getInstance(), new IncrementalMerkleTreeCapsule(
        incrementalMerkleTreeData).getInstance());
  }

  @Test
  public void testContain() throws Exception {
    final boolean result1 = incrementalMerkleTreeStore.contain("Address1".getBytes());
    final boolean result2 = incrementalMerkleTreeStore.contain("Address2".getBytes());
    Assert.assertTrue(result1);
    Assert.assertFalse(result2);
  }
}
