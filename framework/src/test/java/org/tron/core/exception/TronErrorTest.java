package org.tron.core.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.tron.common.arch.Arch;
import org.tron.common.log.LogService;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.GetBlockServlet;
import org.tron.core.services.http.RateLimiterServlet;
import org.tron.core.zen.ZksnarkInitService;


@RunWith(MockitoJUnitRunner.class)
public class TronErrorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
  public void clearMocks() {
    Mockito.clearAllCaches();
    Args.clearParam();
  }

  @Test
  public void testTronError() {
    TronError tronError = new TronError("message", TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
    Assert.assertEquals(tronError.getErrCode(), TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
    Assert.assertEquals(tronError.getErrCode().toString(), "WITNESS_KEYSTORE_LOAD(-1)");
    Assert.assertEquals(tronError.getErrCode().getCode(), -1);
    tronError = new TronError("message", new Throwable(), TronError.ErrCode.API_SERVER_INIT);
    Assert.assertEquals(tronError.getErrCode(), TronError.ErrCode.API_SERVER_INIT);
    tronError = new TronError(new Throwable(), TronError.ErrCode.LEVELDB_INIT);
    Assert.assertEquals(tronError.getErrCode(), TronError.ErrCode.LEVELDB_INIT);
  }

  @Test
  public void ZksnarkInitTest() throws IllegalAccessException, NoSuchFieldException {
    Field field = ZksnarkInitService.class.getDeclaredField("initialized");
    field.setAccessible(true);
    AtomicBoolean atomicBoolean = (AtomicBoolean) field.get(null);
    boolean originalValue = atomicBoolean.get();
    atomicBoolean.set(false);

    try (MockedStatic<JLibrustzcash> mock = mockStatic(JLibrustzcash.class)) {
      mock.when(() -> JLibrustzcash.librustzcashInitZksnarkParams(any()))
          .thenAnswer(invocation -> {
            throw new ZksnarkException("Zksnark init failed");
          });
      TronError thrown = assertThrows(TronError.class,
          ZksnarkInitService::librustzcashInitZksnarkParams);
      assertEquals(TronError.ErrCode.ZCASH_INIT, thrown.getErrCode());
    } finally {
      atomicBoolean.set(originalValue);
    }
  }

  @Test
  public void LogLoadTest() throws IOException {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    try {
      LogService.load("non-existent.xml");
      Path path = temporaryFolder.newFile("logback.xml").toPath();
      TronError thrown = assertThrows(TronError.class, () -> LogService.load(path.toString()));
      assertEquals(TronError.ErrCode.LOG_LOAD, thrown.getErrCode());
    } finally {
      try {
        context.reset();
        ContextInitializer ci = new ContextInitializer(context);
        ci.autoConfig();
      } catch (JoranException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void witnessInitTest() {
    TronError thrown = assertThrows(TronError.class, () -> {
      Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    });
    assertEquals(TronError.ErrCode.WITNESS_INIT, thrown.getErrCode());
  }

  @Test
  public void rateLimiterServletInitTest() {
    Args.setParam(new String[]{}, Constant.TEST_CONF);
    RateLimiterInitialization rateLimiter = new RateLimiterInitialization();
    Args.getInstance().setRateLimiterInitialization(rateLimiter);
    Map<String, String> item = new HashMap<>();
    item.put("strategy", "strategy");
    item.put("paramString", "params");
    item.put("component", "GetBlockServlet");
    ConfigObject config = ConfigFactory.parseMap(item).root();
    rateLimiter.setHttpMap(
        Collections.singletonList(new RateLimiterInitialization.HttpRateLimiterItem(config)));
    RateLimiterServlet servlet = new GetBlockServlet();
    TronError thrown = assertThrows(TronError.class, () ->
        ReflectUtils.invokeMethod(servlet, "addRateContainer"));
    assertEquals(TronError.ErrCode.RATE_LIMITER_INIT, thrown.getErrCode());
  }

  @Test
  public void shutdownBlockTimeInitTest() {
    Map<String, String> params = new HashMap<>();
    params.put(Constant.NODE_SHUTDOWN_BLOCK_TIME, "0");
    params.put("storage.db.directory", "database");
    Config config = ConfigFactory.defaultOverrides().withFallback(
        ConfigFactory.parseMap(params));
    TronError thrown = assertThrows(TronError.class, () -> Args.setParam(config));
    assertEquals(TronError.ErrCode.AUTO_STOP_PARAMS, thrown.getErrCode());
  }

  @Test
  public void testThrowIfUnsupportedJavaVersion() {
    runArchTest("x86_64", "1.8", false);
    runArchTest("x86_64", "11", true);
    runArchTest("x86_64", "17", true);
    runArchTest("aarch64", "17", false);
    runArchTest("aarch64", "1.8", true);
    runArchTest("aarch64", "11", true);
  }

  private void runArchTest(String osArch, String javaVersion, boolean expectThrow) {
    try (MockedStatic<Arch> mocked = mockStatic(Arch.class)) {
      boolean isX86 = "x86_64".equals(osArch);
      boolean isArm64 = "aarch64".equals(osArch);

      boolean isJava8 = "1.8".equals(javaVersion);
      boolean isJava17 = "17".equals(javaVersion);

      mocked.when(Arch::isX86).thenReturn(isX86);
      mocked.when(Arch::isArm64).thenReturn(isArm64);

      mocked.when(Arch::isJava8).thenReturn(isJava8);
      mocked.when(Arch::isJava17).thenReturn(isJava17);

      mocked.when(Arch::getOsArch).thenReturn(osArch);
      mocked.when(Arch::javaSpecificationVersion).thenReturn(javaVersion);
      mocked.when(Arch::withAll).thenReturn(String.format(
          "Architecture: %s, Java Version: %s", osArch, javaVersion));

      mocked.when(Arch::throwIfUnsupportedJavaVersion).thenCallRealMethod();

      if (expectThrow) {
        TronError err = assertThrows(
            TronError.class, () -> Args.setParam(new String[]{}, Constant.TEST_CONF));

        String expectedJavaVersion = isX86 ? "1.8" : "17";
        String expectedMessage = String.format(
            "Java %s is required for %s architecture. Detected version %s",
            expectedJavaVersion, osArch, javaVersion);
        assertEquals(expectedMessage, err.getCause().getMessage());
        assertEquals(TronError.ErrCode.JDK_VERSION, err.getErrCode());
        mocked.verify(Arch::withAll, times(1));
      } else {
        try {
          Arch.throwIfUnsupportedJavaVersion();
        } catch (Exception e) {
          fail("Expected no exception, but got: " + e.getMessage());
        }
        mocked.verify(Arch::withAll, never());
      }
    }
  }

}
