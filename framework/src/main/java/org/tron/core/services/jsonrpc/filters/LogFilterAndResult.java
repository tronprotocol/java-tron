package org.tron.core.services.jsonrpc.filters;

import java.util.ArrayList;
import lombok.Getter;
import org.tron.core.Wallet;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;

public class LogFilterAndResult extends FilterResult<LogFilterElement> {

  @Getter
  private LogFilterWrapper logFilterWrapper;

  public LogFilterAndResult(FilterRequest fr, long currentMaxBlockNum, Wallet wallet)
      throws JsonRpcInvalidParamsException {
    this.logFilterWrapper = new LogFilterWrapper(fr, currentMaxBlockNum, wallet);
    result = new ArrayList<>();
    this.updateExpireTime();
  }

  @Override
  public void add(LogFilterElement logFilterElement) {
    result.add(logFilterElement);
  }

  @Override
  public void clear() {
    result.clear();
    this.updateExpireTime();
  }
}
