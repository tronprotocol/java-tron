package org.tron.core.net.services;

import java.io.File;
import java.lang.reflect.Method;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.service.SyncService;


public class SyncServiceTest {
  private static final Logger logger = LoggerFactory.getLogger("Test");
  private TronApplicationContext context;
  private CommonParameter argsTest;
  private Application appTest;
  private TronNetDelegate delegate;
  private Class syncClazz;
  private SyncService sync;

  /**
   * start the application.
   */
  @Before
  public void init() {
    argsTest = Args.getInstance();
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.initServices(argsTest);
    appTest.startServices();
    appTest.startup();
    delegate = context.getBean(TronNetDelegate.class);
    sync = context.getBean(SyncService.class);
  }

  /**
   * destroy the context.
   */
  @After
  public void destroy() {
    Args.clearParam();
    appTest.shutdownServices();
    appTest.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File("output-directory"))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void processSyncBlockTest_invalidBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    Method m = ReflectionUtils.findMethod(SyncService.class,
        "processSyncBlock", BlockCapsule.class);
    ReflectionUtils.makeAccessible(m);
    try {
      ReflectionUtils.invokeMethod(m, sync, blockCapsule);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(e instanceof TronException);
    }
  }

}
