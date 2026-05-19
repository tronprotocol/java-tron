package org.tron.core.vm.program.listener;

import org.tron.common.runtime.vm.LogInfo;

/**
 * Tracer surface for {@code eth_simulateV1}. Captures EVM value transfers
 * and explicit LOG opcode emissions into a single ordered stream so the
 * RPC response can interleave them by emission order (matching geth's
 * {@code OnEnter}/{@code OnLog} interleaving). Implementations are
 * expected to keep a per-sub-call frame stack and a shared monotonic
 * sequence counter; reverted frames discard their entries.
 */
public interface SimulationTracer {

  /** Push a new frame for a sub-call. */
  void enterFrame();

  /** Pop the top frame and merge its entries into the parent. */
  void exitFrame();

  /** Pop the top frame and drop its entries (sub-call reverted or threw). */
  void revertFrame();

  /**
   * Record a value transfer at the current top frame. {@code fromEvm} and
   * {@code toEvm} must be EVM 20-byte form (Tron's leading {@code 0x41}
   * prefix already stripped) since the bytes flow into ERC-20
   * {@code Transfer(address,address,uint256)} indexed topics.
   */
  void onTransfer(byte[] fromEvm, byte[] toEvm, long amount);

  /**
   * Record a TRC-10 token transfer at the current top frame. Same address
   * encoding rules as {@link #onTransfer}. {@code tokenId} is the Tron asset
   * id (matches the {@code TriggerSmartContract.tokenId} proto type);
   * {@code amount} is the transferred quantity in the token's smallest unit.
   */
  void onTokenTransfer(byte[] fromEvm, byte[] toEvm, long tokenId, long amount);

  /** Record an explicit EVM LOG opcode emission at the current top frame. */
  void onLog(LogInfo logInfo);
}
