package org.tron.core.services.jsonrpc.filters;

import java.util.List;
import lombok.Getter;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;

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
