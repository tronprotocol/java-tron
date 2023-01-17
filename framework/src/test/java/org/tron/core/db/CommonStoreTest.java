package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class CommonStoreTest extends BaseTest {
  private static String dbDirectory = "db_CommonStore_test";
  private static String indexDirectory = "index_CommonStore_test";
  @Resource
  private CommonStore commonStore;

  static {
    dbPath = "output_CommonStore_test";
    Args.setParam(new String[] {
        "--output-directory", dbPath,
        "--storage-db-directory", dbDirectory,
        "--storage-index-directory", indexDirectory},
        Constant.TEST_CONF
    );
  }

  @Test
  public void nodeTypeTest() {
    Assert.assertEquals(0, commonStore.getNodeType());
    commonStore.setNodeType(1);
    Assert.assertEquals(1, commonStore.getNodeType());
  }

  @Test
  public void lowestBlockNumTest() {
    Assert.assertEquals(0, commonStore.getLowestBlockNum());
    commonStore.setLowestBlockNum(100);
    Assert.assertEquals(100, commonStore.getLowestBlockNum());
  }
}
