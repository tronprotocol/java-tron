package org.tron.core.services.jsonrpc.types;

import java.util.List;
import org.tron.common.runtime.ProgramResult;
import org.tron.core.vm.program.listener.BufferingSimulationTracer;

public final class SimulateCallOutcome {

  private final ProgramResult result;
  private final List<BufferingSimulationTracer.Entry> tracerEntries;

  public SimulateCallOutcome(ProgramResult result,
      List<BufferingSimulationTracer.Entry> tracerEntries) {
    this.result = result;
    this.tracerEntries = tracerEntries;
  }

  public ProgramResult getResult() {
    return result;
  }

  public List<BufferingSimulationTracer.Entry> getTracerEntries() {
    return tracerEntries;
  }
}
