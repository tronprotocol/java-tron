package org.tron.core.services.jsonrpc.types;

import java.util.List;
import org.tron.core.capsule.BlockCapsule;

public final class SimulateOutcome {

  private final BlockCapsule headBlockCapsule;
  private final List<SimulateCallOutcome> calls;

  public SimulateOutcome(BlockCapsule headBlockCapsule, List<SimulateCallOutcome> calls) {
    this.headBlockCapsule = headBlockCapsule;
    this.calls = calls;
  }

  public BlockCapsule getHeadBlockCapsule() {
    return headBlockCapsule;
  }

  public List<SimulateCallOutcome> getCalls() {
    return calls;
  }
}
