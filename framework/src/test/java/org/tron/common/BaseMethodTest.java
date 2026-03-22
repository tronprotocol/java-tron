package org.tron.common;

import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

/**
 * Base class for tests that need a fresh Spring context per test method.
 *
 * Each @Test method gets its own TronApplicationContext, created in @Before
 * and destroyed in @After, ensuring full isolation between tests.
 *
 * Subclasses can customize behavior by overriding hook methods:
 *   extraArgs()      — additional CLI args (e.g. "--debug")
 *   configFile()     — config file (default: config-test.conf)
 *   beforeContext()  — runs after Args.setParam, before Spring context creation
 *   afterInit()      — runs after Spring context is ready (e.g. get extra beans)
 *   beforeDestroy()  — runs before context shutdown (e.g. close resources)
 *
 * Use this when:
 *   - methods modify database state that would affect other methods
 *   - methods need different CommonParameter settings (e.g. setP2pDisable)
 *   - methods need beforeContext() to configure state before Spring starts
 *
 * If methods are read-only and don't interfere with each other, use BaseTest instead.
 * Tests that don't need Spring (e.g. pure unit tests) should NOT extend either base class.
 */
@Slf4j
public abstract class BaseMethodTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  protected TronApplicationContext context;
  protected Application appT;
  protected Manager dbManager;
  protected ChainBaseManager chainBaseManager;

  protected String[] extraArgs() {
    return new String[0];
  }

  protected String configFile() {
    return TestConstants.TEST_CONF;
  }

  @Before
  public final void initContext() throws IOException {
    String[] baseArgs = new String[]{
        "--output-directory", temporaryFolder.newFolder().toString()};
    String[] allArgs = mergeArgs(baseArgs, extraArgs());
    Args.setParam(allArgs, configFile());
    beforeContext();
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    afterInit();
  }

  protected void beforeContext() {
  }

  protected void afterInit() {
  }

  @After
  public final void destroyContext() {
    beforeDestroy();
    if (context != null) {
      context.close(); // triggers appT.shutdown() via TronApplicationContext
    }
    Args.clearParam();
  }

  protected void beforeDestroy() {
  }

  private static String[] mergeArgs(String[] base, String[] extra) {
    String[] result = Arrays.copyOf(base, base.length + extra.length);
    System.arraycopy(extra, 0, result, base.length, extra.length);
    return result;
  }
}
