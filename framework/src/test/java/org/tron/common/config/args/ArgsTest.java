package org.tron.common.config.args;

import com.beust.jcommander.JCommander;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;


public class ArgsTest {

  private static final String dbPath = "output_arg_test";

  @Before
  public void init() {
    Args.setParam(new String[] {"--output-directory", dbPath, "--p2p-disable", "true",
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
    Assert.assertEquals(Args.getInstance().getRateLimiterGlobalQps(), 1000);
    Assert.assertEquals(Args.getInstance().getRateLimiterGlobalIpQps(), 1000);
    Assert.assertEquals(Args.getInstance().p2pDisable, true);
    Assert.assertEquals(Args.getInstance().getMaxTps(), 1000);
    RateLimiterInitialization rateLimiter = Args.getInstance().getRateLimiterInitialization();
    Assert.assertEquals(rateLimiter.getHttpMap().size(), 1);
    Assert.assertEquals(rateLimiter.getRpcMap().size(), 0);
  }

  @Test
  public void testHelpMessage() {
    JCommander jCommander = JCommander.newBuilder().addObject(Args.PARAMETER).build();
    Method method;
    try {
      method = Args.class.getDeclaredMethod("printVersion");
      method.setAccessible(true);
      method.invoke(Args.class);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      Assert.fail();
    }
    Args.printHelp(jCommander);
  }
}
