package org.tron.core.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
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
  public void  clearMocks() {
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
  public void ZksnarkInitTest() {
    try (MockedStatic<JLibrustzcash> mock = mockStatic(JLibrustzcash.class)) {
      mock.when(() -> JLibrustzcash.librustzcashInitZksnarkParams(any()))
          .thenAnswer(invocation -> {
            throw new ZksnarkException("Zksnark init failed");
          });
      TronError thrown = assertThrows(TronError.class,
          ZksnarkInitService::librustzcashInitZksnarkParams);
      assertEquals(TronError.ErrCode.ZCASH_INIT, thrown.getErrCode());
    }
  }

  @Test
  public void LogLoadTest() throws IOException {
    LogService.load("non-existent.xml");
    Path path = temporaryFolder.newFile("logback.xml").toPath();
    TronError thrown = assertThrows(TronError.class, () -> LogService.load(path.toString()));
    assertEquals(TronError.ErrCode.LOG_LOAD, thrown.getErrCode());
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
    Map<String,String> params = new HashMap<>();
    params.put(Constant.NODE_SHUTDOWN_BLOCK_TIME, "0");
    params.put("storage.db.directory", "database");
    Config config = ConfigFactory.defaultOverrides().withFallback(
        ConfigFactory.parseMap(params));
    TronError thrown = assertThrows(TronError.class, () -> Args.setParam(config));
    assertEquals(TronError.ErrCode.AUTO_STOP_PARAMS, thrown.getErrCode());
  }
}
