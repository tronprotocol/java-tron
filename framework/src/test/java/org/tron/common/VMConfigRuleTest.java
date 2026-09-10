package org.tron.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.Statement;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;

public class VMConfigRuleTest {

  @Rule
  public final VMConfigRule vmConfigRule = new VMConfigRule();

  @Before
  public void seedConfig() {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmOsaka = true;
    VMConfig.setGlobalSnapshot(snapshot);
    ConfigLoader.disable = false;
  }

  @Test
  public void restoresConfigAfterAssertionFailure() {
    Statement failingTest = new Statement() {
      @Override
      public void evaluate() {
        polluteConfig();
        throw new AssertionError("intentional failure");
      }
    };
    AssertionError failure = Assert.assertThrows(AssertionError.class,
        () -> new VMConfigRule().apply(failingTest, Description.EMPTY).evaluate());
    Assert.assertEquals("intentional failure", failure.getMessage());
    assertConfigRestored();
  }

  @Test
  public void restoresConfigAfterSetupFailure() throws Exception {
    // Explicitly run the fixture while normal test discovery honors its class-level @Ignore.
    Result result = new JUnitCore().run(new BlockJUnit4ClassRunner(FailingSetup.class));
    Assert.assertEquals(1, result.getRunCount());
    Assert.assertEquals(1, result.getFailureCount());
    Assert.assertEquals("intentional setup failure", result.getFailures().get(0).getMessage());
    assertConfigRestored();
  }

  @Test
  public void skipsFixtureDuringNormalDiscovery() {
    Result result = JUnitCore.runClasses(FailingSetup.class);
    Assert.assertEquals(0, result.getRunCount());
    Assert.assertEquals(0, result.getFailureCount());
    Assert.assertEquals(1, result.getIgnoreCount());
    assertConfigRestored();
  }

  private static void polluteConfig() {
    VMConfig.initAllowTvmOsaka(0);
    ConfigLoader.disable = true;
    VMConfig.setLocalSnapshot(new VMConfig.Snapshot());
  }

  private static void assertConfigRestored() {
    Assert.assertTrue("Global snapshot or thread-local view leaked", VMConfig.allowTvmOsaka());
    Assert.assertFalse("Config loader switch leaked", ConfigLoader.disable);
  }

  @Ignore("Failure fixture executed explicitly by VMConfigRuleTest")
  public static class FailingSetup {
    @Rule
    public final VMConfigRule vmConfigRule = new VMConfigRule();

    @Before
    public void setUp() {
      polluteConfig();
      throw new IllegalStateException("intentional setup failure");
    }

    @Test
    public void body() {
      Assert.fail("Body must not run after failed setup");
    }
  }
}
