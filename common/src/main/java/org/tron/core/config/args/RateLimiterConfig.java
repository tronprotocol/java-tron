package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiter configuration bean.
 * Field names match config.conf keys under "rate.limiter".
 */
@Slf4j
@Getter
@Setter
public class RateLimiterConfig {

  private GlobalConfig global = new GlobalConfig();
  private P2pRateLimitConfig p2p = new P2pRateLimitConfig();
  private List<HttpRateLimitItem> http = new ArrayList<>();
  private List<RpcRateLimitItem> rpc = new ArrayList<>();

  @Getter
  @Setter
  public static class GlobalConfig {
    private int qps = 50000;
    private IpConfig ip = new IpConfig();
    private ApiConfig api = new ApiConfig();

    @Getter
    @Setter
    public static class IpConfig {
      private int qps = 10000;
    }

    @Getter
    @Setter
    public static class ApiConfig {
      private int qps = 1000;
    }
  }

  @Getter
  @Setter
  public static class P2pRateLimitConfig {
    private double syncBlockChain = 3.0;
    private double fetchInvData = 3.0;
    private double disconnect = 1.0;
  }

  @Getter
  @Setter
  public static class HttpRateLimitItem {
    private String component = "";
    private String strategy = "";
    private String paramString = "";
  }

  @Getter
  @Setter
  public static class RpcRateLimitItem {
    private String component = "";
    private String strategy = "";
    private String paramString = "";
  }

  private static final Config DEFAULTS = ConfigFactory.parseString(
      "global { qps = 50000, ip { qps = 10000 }, api { qps = 1000 } }\n"
          + "p2p { syncBlockChain = 3.0, fetchInvData = 3.0, disconnect = 1.0 }\n"
          + "http = []\n"
          + "rpc = []\n"
  );

  public static RateLimiterConfig fromConfig(Config config) {
    Config section = config.hasPath("rate.limiter")
        ? config.getConfig("rate.limiter").withFallback(DEFAULTS)
        : DEFAULTS;
    return ConfigBeanFactory.create(section, RateLimiterConfig.class);
  }
}
