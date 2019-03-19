package org.tron.stresstest.dispatch.strategy;

import java.util.List;
import org.tron.stresstest.dispatch.Stats;

public interface IStrategy<T> {

  T dispatch();

  List<Stats> stats();
}
