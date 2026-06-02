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

  /**
   * Whether to apply pre-execution sender checks before each simulated call.
   *
   * <p><b>Diverges from the Ethereum {@code eth_simulateV1} spec.</b> Upstream
   * geth's {@code validation=true} enables: signature verification, nonce match,
   * base-fee / max-fee-per-gas / max-priority-fee-per-gas consistency, sender
   * balance ≥ {@code gas × gasPrice + value}, and intrinsic-gas check.
   *
   * <p>Tron's {@code validation=true} runs only two checks (see
   * {@code Wallet#validateSenderForSimulate}):
   * <ul>
   *   <li>sender account exists in state (i.e. has been activated on-chain);</li>
   *   <li>sender balance ≥ {@code callValue}.</li>
   * </ul>
   * Signatures, nonces, fee fields, and energy-limit are NOT validated. Clients
   * porting from geth must not rely on a failed {@code validation=true} call
   * meaning the transaction would also fail broadcast — it covers fewer cases.
   */
  @Getter
  @Setter
  private boolean validation;
}
