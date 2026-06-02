package org.tron.core.vm.program.listener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.tron.common.runtime.vm.LogInfo;

public class BufferingSimulationTracer implements SimulationTracer {

  /**
   * Defensive cap on buffered entries per simulate call. A well-behaved call produces O(100)
   * entries (transfers + LOGs); typical batches stay well under 10 KB / call. The cap guards
   * against pathological or adversarial contracts that emit thousands of LOG opcodes within
   * a single energy budget. Exceeding it aborts the call with a clear {@link RuntimeException},
   * which the simulate driver surfaces as a per-call failure outcome.
   */
  public static final int MAX_ENTRIES_PER_CALL = 100_000;

  public enum EntryKind { EXPLICIT, TRANSFER, TOKEN_TRANSFER }

  public static final class Entry {
    private final long seq;
    private final EntryKind kind;
    private final LogInfo logInfo;
    private final byte[] fromEvm;
    private final byte[] toEvm;
    private final long tokenId;
    private final long amount;

    private Entry(long seq, EntryKind kind, LogInfo logInfo,
        byte[] fromEvm, byte[] toEvm, long tokenId, long amount) {
      this.seq = seq;
      this.kind = kind;
      this.logInfo = logInfo;
      this.fromEvm = fromEvm;
      this.toEvm = toEvm;
      this.tokenId = tokenId;
      this.amount = amount;
    }

    public long getSeq() { return seq; }

    public EntryKind getKind() { return kind; }

    public LogInfo getLogInfo() { return logInfo; }

    public byte[] getFromEvm() { return fromEvm; }

    public byte[] getToEvm() { return toEvm; }

    public long getTokenId() { return tokenId; }

    public long getAmount() { return amount; }
  }

  private final Deque<List<Entry>> stack = new ArrayDeque<>();
  private long seq;
  private int callEntryCount;

  public BufferingSimulationTracer() {
  }

  /** Reset per-call state. Drops any leftover frames and starts a fresh root. */
  public void beginCall() {
    stack.clear();
    stack.push(new ArrayList<>());
    callEntryCount = 0;
  }

  /** Drop all buffered entries for the current call (used on top-level revert). */
  public void dropCall() {
    beginCall();
  }

  /** Returns the root frame's entries in {@code seq} order. */
  public List<Entry> snapshotCall() {
    if (stack.isEmpty()) {
      return Collections.emptyList();
    }
    List<Entry> all = new ArrayList<>(stack.peekLast());
    all.sort((a, b) -> Long.compare(a.seq, b.seq));
    return all;
  }

  @Override
  public void enterFrame() {
    stack.push(new ArrayList<>());
  }

  @Override
  public void exitFrame() {
    if (stack.size() <= 1) {
      return;
    }
    List<Entry> top = stack.pop();
    stack.peek().addAll(top);
  }

  @Override
  public void revertFrame() {
    if (stack.size() <= 1) {
      return;
    }
    stack.pop();
  }

  @Override
  public void onTransfer(byte[] fromEvm, byte[] toEvm, long amount) {
    if (stack.isEmpty()) {
      return;
    }
    checkEntryBudget();
    stack.peek().add(new Entry(seq++, EntryKind.TRANSFER, null, fromEvm, toEvm, 0L, amount));
  }

  @Override
  public void onTokenTransfer(byte[] fromEvm, byte[] toEvm, long tokenId, long amount) {
    if (stack.isEmpty()) {
      return;
    }
    checkEntryBudget();
    stack.peek().add(new Entry(seq++, EntryKind.TOKEN_TRANSFER, null,
        fromEvm, toEvm, tokenId, amount));
  }

  @Override
  public void onLog(LogInfo logInfo) {
    if (stack.isEmpty()) {
      return;
    }
    checkEntryBudget();
    stack.peek().add(new Entry(seq++, EntryKind.EXPLICIT, logInfo, null, null, 0L, 0L));
  }

  private void checkEntryBudget() {
    if (callEntryCount >= MAX_ENTRIES_PER_CALL) {
      throw new RuntimeException(
          "simulation tracer entry budget exceeded: more than " + MAX_ENTRIES_PER_CALL
              + " logs/transfers in a single call");
    }
    callEntryCount++;
  }
}
