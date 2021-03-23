package org.tron.common.parameter;

import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;


public class RateLimiterInitialization {

  @Getter
  private boolean httpFlag;

  @Getter
  private boolean rpcFlag;

  @Getter
  private Map<String, HttpRateLimiterItem> httpMap = new HashMap();

  @Getter
  private Map<String, RpcRateLimiterItem> rpcMap = new HashMap();

  @Nullable
  public static HttpRateLimiterItem createHttpItem(final ConfigObject asset) {
    try {
      return new HttpRateLimiterItem(asset);
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  public static RpcRateLimiterItem createRpcItem(final ConfigObject asset) {
    try {
      return new RpcRateLimiterItem(asset);
    } catch (Exception e) {
      return null;
    }
  }

  public void setHttpMap(List<HttpRateLimiterItem> list) {
    for (HttpRateLimiterItem item : list) {
      if (item != null) {
        httpMap.put(item.component, item);
      }
    }
    httpFlag = httpMap.size() > 0;
  }

  public void setRpcMap(List<RpcRateLimiterItem> list) {
    for (RpcRateLimiterItem item : list) {
      if (item != null) {
        rpcMap.put(item.component, item);
      }
    }
    rpcFlag = rpcMap.size() > 0;
  }

  public static class HttpRateLimiterItem {

    @Getter
    private String component;

    @Getter
    private String strategy;

    @Getter
    private String params;

    public HttpRateLimiterItem(ConfigObject asset) {
      component = asset.get("component").unwrapped().toString();
      strategy = asset.get("strategy").unwrapped().toString();
      params = asset.get("paramString").unwrapped().toString();
    }
  }


  public static class RpcRateLimiterItem {

    @Getter
    private String component;

    @Getter
    private String strategy;

    @Getter
    private String params;

    public RpcRateLimiterItem(ConfigObject asset) {
      component = asset.get("component").unwrapped().toString();
      strategy = asset.get("strategy").unwrapped().toString();
      params = asset.get("paramString").unwrapped().toString();
    }
  }
}