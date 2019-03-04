package org.tron.stresstest.dispatch.strategy;

import java.util.List;
import java.util.stream.Collectors;
import org.tron.stresstest.dispatch.Stats;

public class Level1Strategy extends AbstractStrategy<Level2Strategy> {
  @Override
  public List<Stats> stats() {
    return source.stream()
        .map(IStrategy::stats)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
