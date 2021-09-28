package org.tron.core.services.jsonrpc;

import java.util.List;
import lombok.Getter;

public abstract class FilterResult<T> {

  private long expireTimeStamp;

  @Getter
  protected List<T> result;

  public void updateExpireTime() {
    expireTimeStamp = System.currentTimeMillis() + TronJsonRpcImpl.expireSeconds * 1000;
  }

  public boolean isExpire() {
    return expireTimeStamp < System.currentTimeMillis();
  }

  public abstract void add(T t);

  public abstract void clear();
}
