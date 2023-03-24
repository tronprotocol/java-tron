package org.tron.core.config.args;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.net.TronNetService;
import org.tron.p2p.P2pConfig;

import java.io.File;

public class DynamicArgsTest {
  protected TronApplicationContext context;
  private DynamicArgs dynamicArgs;
  private String dbPath = "output-dynamic-config-test";

  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dynamicArgs = context.getBean(DynamicArgs.class);

  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void get() {
    CommonParameter parameter = Args.getInstance();
    Assert.assertFalse(parameter.isDynamicConfigEnable());

  }

  @Test
  public void start() {
    dynamicArgs.start();
    TronNetService tronNetService = context.getBean(TronNetService.class);
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", new P2pConfig());
    dynamicArgs.reload();
    dynamicArgs.close();
    Assert.assertTrue((boolean)ReflectUtils.getFieldObject(dynamicArgs, "shutdown"));
  }
}
