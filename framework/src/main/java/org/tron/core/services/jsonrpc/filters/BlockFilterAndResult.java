package org.tron.core.services.jsonrpc.filters;

import java.util.ArrayList;
import org.tron.core.services.jsonrpc.filters.FilterResult;

public class BlockFilterAndResult extends FilterResult<String> {

  public BlockFilterAndResult() {
    this.updateExpireTime();
    result = new ArrayList<>();
  }

  @Override
  public void add(String s) {
    result.add(s);
  }

  @Override
  public void clear() {
    result.clear();
    this.updateExpireTime();
  }
}
