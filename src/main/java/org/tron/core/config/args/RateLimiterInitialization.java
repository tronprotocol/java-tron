package org.tron.core.config.args;

import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;


public class RateLimiterInitialization {

  public boolean httpFlag;
  public boolean rpcFlag;


  @Getter
  private Map<String, HttpRateLimiterItem> httpMap = new HashMap();

  @Getter
  private Map<String, RpcRateLimiterItem> rpcMap = new HashMap();


  public void setHttpMap(List<HttpRateLimiterItem> list) {
    for (HttpRateLimiterItem item : list) {
      httpMap.put(item.servlet, item);
    }
    httpFlag = httpMap.size() > 0;
  }


  public void setRpcMap(List<RpcRateLimiterItem> list) {
    for (RpcRateLimiterItem item : list) {
      rpcMap.put(item.servlet, item);
    }
    rpcFlag = rpcMap.size() > 0;
  }


  public static HttpRateLimiterItem createHttpItem(final ConfigObject asset) {
    return new HttpRateLimiterItem(asset);
  }

  public static RpcRateLimiterItem createRpcItem(final ConfigObject asset) {
    return new RpcRateLimiterItem(asset);
  }

  public static class HttpRateLimiterItem {

    @Getter
    String servlet;

    @Getter
    String strategy;

    @Getter
    String params;

    public HttpRateLimiterItem(ConfigObject asset) {
      servlet = asset.get("servlet").unwrapped().toString();
      strategy = asset.get("strategy").unwrapped().toString();
      params = asset.get("paramString").unwrapped().toString();
    }
  }


  public static class RpcRateLimiterItem {

    @Getter
    String servlet;

    @Getter
    String strategy;

    @Getter
    String params;

    public RpcRateLimiterItem(ConfigObject asset) {
      servlet = asset.get("servlet").unwrapped().toString();
      strategy = asset.get("strategy").unwrapped().toString();
      params = asset.get("paramString").unwrapped().toString();
    }
  }
}