package org.tron.core.config.args;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.net.TronNetService;
import org.tron.p2p.P2pConfig;

public class DynamicArgsTest extends BaseMethodTest {
  private DynamicArgs dynamicArgs;

  @Override
  protected void afterInit() {
    dynamicArgs = context.getBean(DynamicArgs.class);
  }

  @Test
  public void start() {
    CommonParameter parameter = Args.getInstance();
    Assert.assertEquals(TestConstants.TEST_CONF, Args.getConfigFilePath());
    Assert.assertTrue(parameter.isDynamicConfigEnable());
    Assert.assertEquals(600, parameter.getDynamicConfigCheckInterval());

    dynamicArgs.init();
    File configFile = (File) ReflectUtils.getFieldObject(dynamicArgs, "configFile");
    Assert.assertNotNull(configFile);
    Assert.assertEquals(TestConstants.TEST_CONF, configFile.getName());
    Assert.assertEquals(0, (long) ReflectUtils.getFieldObject(dynamicArgs, "lastModified"));

    TronNetService tronNetService = context.getBean(TronNetService.class);
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", new P2pConfig());
    File config = new File(Args.getConfigFilePath());
    if (!config.exists()) {
      try {
        config.createNewFile();
      } catch (Exception e) {
        return;
      }
      dynamicArgs.run();
      try {
        config.delete();
      } catch (Exception e) {
        return;
      }
    }
    try {
      dynamicArgs.reload();
    } catch (Exception e) {
      // no need to deal with
    }

    dynamicArgs.close();
  }
}
