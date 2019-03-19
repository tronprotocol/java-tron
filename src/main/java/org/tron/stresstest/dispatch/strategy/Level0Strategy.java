package org.tron.stresstest.dispatch.strategy;

import java.util.List;
import java.util.stream.Collectors;
import org.tron.stresstest.dispatch.Stats;

public abstract class Level0Strategy extends AbstractStrategy<Level1Strategy> {
  @Override
  public List<Stats> stats() {
    return source.stream()
        .map(IStrategy::stats)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

}
