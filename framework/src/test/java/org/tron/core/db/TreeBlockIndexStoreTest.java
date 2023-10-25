package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.TreeBlockIndexStore;

public class TreeBlockIndexStoreTest extends BaseTest {

  @Resource
  private TreeBlockIndexStore treeBlockIndexStore;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }

  @Test
  public void testPut() throws ItemNotFoundException {
    treeBlockIndexStore.put(1L, "testPut".getBytes());
    byte[] result = treeBlockIndexStore.get(1L);
    Assert.assertEquals(new String(result),"testPut");
  }

  @Test
  public void testGetByNum() throws Exception {
    treeBlockIndexStore.put(2L, "testGetByNum".getBytes());
    byte[] result = treeBlockIndexStore.get(2L);
    Assert.assertEquals(new String(result),"testGetByNum");
    Assert.assertThrows(ItemNotFoundException.class, () -> treeBlockIndexStore.get(0L));
  }

  @Test
  public void testGet() throws Exception {
    treeBlockIndexStore.put(3L, "testGet".getBytes());
    final BytesCapsule result = treeBlockIndexStore.get(ByteArray.fromLong(3L));
    Assert.assertEquals(new String(result.getData()),"testGet");
    Assert.assertThrows(ItemNotFoundException.class, () -> treeBlockIndexStore
        .get(ByteArray.fromLong(0L)));
  }

}
