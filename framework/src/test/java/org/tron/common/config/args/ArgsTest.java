package org.tron.common.config.args;

import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class ArgsTest {

  private static final String dbPath = "output_arg_test";

  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--p2p-disable", "true",
        "--debug"}, Constant.TEST_CONF);
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testConfig() {
    Assert.assertEquals(Args.getInstance().getMaxTransactionPendingSize(), 2000);
    Assert.assertEquals(Args.getInstance().getPendingTransactionTimeout(), 60_000);
    Assert.assertEquals(Args.getInstance().getNodeDiscoveryPingTimeout(), 15_000);
    Assert.assertEquals(Args.getInstance().getMaxFastForwardNum(), 3);
    Assert.assertEquals(Args.getInstance().getBlockCacheTimeout(), 60);
    Assert.assertEquals(Args.getInstance().isNodeDetectEnable(), false);
    Assert.assertFalse(Args.getInstance().isNodeEffectiveCheckEnable());
    Assert.assertEquals(Args.getInstance().getRateLimiterGlobalQps(), 50000);
    Assert.assertEquals(Args.getInstance().getRateLimiterGlobalIpQps(), 10000);
    Assert.assertEquals(Args.getInstance().p2pDisable, true);
  }
}