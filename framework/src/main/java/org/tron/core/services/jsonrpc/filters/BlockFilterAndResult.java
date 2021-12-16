package org.tron.core.services.jsonrpc.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockFilterAndResult extends FilterResult<String> {

  public BlockFilterAndResult() {
    this.updateExpireTime();
    result = new LinkedBlockingQueue<>();
  }

  @Override
  public void add(String s) {
    result.add(s);
  }

  @Override
  public List<String> popAll() {
    List<String> elements = new ArrayList<>();
    result.drainTo(elements);
    return elements;
  }
}
