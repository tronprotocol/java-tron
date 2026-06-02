package org.tron.core.services.jsonrpc.types;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder(alphabetic = true)
public class SimulateBlockResult extends BlockResult {

  @Getter
  @Setter
  private List<SimulateCallResult> calls;
}
