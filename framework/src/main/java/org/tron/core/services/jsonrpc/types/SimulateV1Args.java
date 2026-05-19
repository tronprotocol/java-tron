package org.tron.core.services.jsonrpc.types;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SimulateV1Args {

  @Getter
  @Setter
  private List<SimulateBlock> blockStateCalls;

  @Getter
  @Setter
  private boolean traceTransfers;

  @Getter
  @Setter
  private boolean returnFullTransactions;

  @Getter
  @Setter
  private boolean validation;
}
