package org.tron.core.db;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDB;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.store.CheckPointV2Store;

public class CheckPointV2StoreTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static {
    RocksDB.loadLibrary();
  }

  @BeforeClass
  public static void initArgs() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void testCloseCallsSuperClose() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-close-super");
    
    // 获取父类的 writeOptions 字段
    Field parentWriteOptionsField = TronDatabase.class.getDeclaredField("writeOptions");
    parentWriteOptionsField.setAccessible(true);
    WriteOptionsWrapper originalParentWriteOptions = (WriteOptionsWrapper) parentWriteOptionsField.get(store);
    
    // 保存原始的 rocks 对象引用，用于后续验证
    org.rocksdb.WriteOptions originalRocks = originalParentWriteOptions.rocks;
    
    // 创建一个 spy 来监控父类的 writeOptions
    WriteOptionsWrapper spyParentWriteOptions = spy(originalParentWriteOptions);
    parentWriteOptionsField.set(store, spyParentWriteOptions);
    
    // 创建一个 spy 来监控 rocks.close() 方法
    org.rocksdb.WriteOptions spyRocks = spy(originalRocks);
    spyParentWriteOptions.rocks = spyRocks;
    
    // 验证父类的 writeOptions 和 dbSource 存在
    Assert.assertNotNull(spyParentWriteOptions);
    Assert.assertNotNull(spyRocks);
    Assert.assertNotNull(store.getDbSource());
    
    // 关闭 store
    store.close();
    
    // 验证父类的 writeOptions.close() 被调用了（通过 super.close()）
    verify(spyParentWriteOptions, times(1)).close();
    
    // 验证 rocks.close() 被调用了（资源真正被关闭）
    verify(spyRocks, times(1)).close();
  }
}
