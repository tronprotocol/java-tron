package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class RateLimiterConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    RateLimiterConfig rl = RateLimiterConfig.fromConfig(empty);
    assertEquals(50000, rl.getGlobal().getQps());
    assertEquals(10000, rl.getGlobal().getIp().getQps());
    assertEquals(1000, rl.getGlobal().getApi().getQps());
    assertEquals(3.0, rl.getP2p().getSyncBlockChain(), 0.001);
    assertEquals(3.0, rl.getP2p().getFetchInvData(), 0.001);
    assertEquals(1.0, rl.getP2p().getDisconnect(), 0.001);
    assertTrue(rl.getHttp().isEmpty());
    assertTrue(rl.getRpc().isEmpty());
    assertFalse(rl.isApiNonBlocking());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "rate.limiter {"
            + " global { qps = 100, ip { qps = 50 }, api { qps = 10 } },"
            + " p2p { syncBlockChain = 5.0, disconnect = 2.0 },"
            + " http = [{ component = TestServlet, strategy = QpsRateLimiterAdapter,"
            + "   paramString = \"qps=10\" }],"
            + " rpc = [{ component = TestRpc, strategy = GlobalPreemptibleAdapter,"
            + "   paramString = \"permit=1\" }],"
            + " apiNonBlocking = true"
            + "}");
    RateLimiterConfig rl = RateLimiterConfig.fromConfig(config);
    assertEquals(100, rl.getGlobal().getQps());
    assertEquals(50, rl.getGlobal().getIp().getQps());
    assertEquals(5.0, rl.getP2p().getSyncBlockChain(), 0.001);
    assertEquals(1, rl.getHttp().size());
    assertEquals("TestServlet", rl.getHttp().get(0).getComponent());
    assertEquals(1, rl.getRpc().size());
    assertEquals("TestRpc", rl.getRpc().get(0).getComponent());
    assertTrue(rl.isApiNonBlocking());
  }
}
