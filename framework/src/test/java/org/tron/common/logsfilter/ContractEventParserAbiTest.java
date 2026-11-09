package org.tron.common.logsfilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_ENTRIES;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_ENTRY_PARAMS;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_EVENTS;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_IDENTIFIER_LENGTH;
import static org.tron.common.logsfilter.ContractEventParserAbi.hasTooManyEntries;
import static org.tron.common.logsfilter.ContractEventParserAbi.hasTooManyEvents;
import static org.tron.common.logsfilter.ContractEventParserAbi.isEntryTooLarge;

import org.junit.Test;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

public class ContractEventParserAbiTest {

  private ABI.Entry.Param param(String type, String name) {
    return ABI.Entry.Param.newBuilder().setType(type).setName(name).build();
  }

  private String repeat(int n) {
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      sb.append('A');
    }
    return sb.toString();
  }

  /** Build an ABI with the given number of function and (non-anonymous) event entries. */
  private ABI abiOf(int functions, int events) {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < functions; i++) {
      b.addEntrys(ABI.Entry.newBuilder().setType(ABI.Entry.EntryType.Function));
    }
    for (int i = 0; i < events; i++) {
      b.addEntrys(ABI.Entry.newBuilder().setType(ABI.Entry.EntryType.Event));
    }
    return b.build();
  }

  // The entry-count limit and the event-count limit are exercised independently below: each case
  // stays within one limit and crosses only the other, so a test cannot pass by accident if one
  // of the two guards were removed.

  @Test
  public void atBothBoundsIsDecoded() {
    // 1,023 functions + 1 event = 1,024 entries, 1 event: within both limits.
    ABI abi = abiOf(MAX_ABI_ENTRIES - 1, 1);
    assertFalse(hasTooManyEntries(abi));
    assertFalse(hasTooManyEvents(abi));
  }

  @Test
  public void onlyEntryLimitExceeded() {
    // 1,024 functions + 1 event = 1,025 entries, 1 event: only the entry limit is exceeded.
    ABI abi = abiOf(MAX_ABI_ENTRIES, 1);
    assertTrue(hasTooManyEntries(abi));
    assertFalse(hasTooManyEvents(abi));
  }

  @Test
  public void onlyEventLimitExceeded() {
    // 513 events + 3 functions = 516 entries, 513 events: only the event limit is exceeded.
    ABI abi = abiOf(3, MAX_ABI_EVENTS + 1);
    assertFalse(hasTooManyEntries(abi));
    assertTrue(hasTooManyEvents(abi));
  }

  @Test
  public void eventCountAtBoundIsAllowed() {
    assertFalse(hasTooManyEvents(abiOf(0, MAX_ABI_EVENTS)));
  }

  @Test
  public void emptyAbiIsWithinLimit() {
    assertFalse(hasTooManyEntries(ABI.getDefaultInstance()));
    assertFalse(hasTooManyEvents(ABI.getDefaultInstance()));
  }

  @Test
  public void anonymousEventsAreNotCounted() {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < MAX_ABI_EVENTS + 1; i++) {
      b.addEntrys(ABI.Entry.newBuilder().setType(ABI.Entry.EntryType.Event).setAnonymous(true));
    }
    assertFalse(hasTooManyEvents(b.build()));
  }

  @Test
  public void normalEntryIsWithinBound() {
    ABI.Entry entry = ABI.Entry.newBuilder().setName("Transfer")
        .addInputs(param("address", "from")).addInputs(param("uint256", "value")).build();
    assertFalse(isEntryTooLarge(entry));
  }

  @Test
  public void overLongEventNameIsRejected() {
    ABI.Entry entry = ABI.Entry.newBuilder()
        .setName(repeat(MAX_ABI_IDENTIFIER_LENGTH + 1)).build();
    assertTrue(isEntryTooLarge(entry));
  }

  @Test
  public void overLongParamTypeIsRejected() {
    ABI.Entry entry = ABI.Entry.newBuilder().setName("E")
        .addInputs(param(repeat(MAX_ABI_IDENTIFIER_LENGTH + 1), "x")).build();
    assertTrue(isEntryTooLarge(entry));
  }

  @Test
  public void overLongParamNameIsRejected() {
    ABI.Entry entry = ABI.Entry.newBuilder().setName("E")
        .addInputs(param("uint256", repeat(MAX_ABI_IDENTIFIER_LENGTH + 1))).build();
    assertTrue(isEntryTooLarge(entry));
  }

  @Test
  public void tooManyParamsIsRejected() {
    ABI.Entry.Builder b = ABI.Entry.newBuilder().setName("E");
    for (int i = 0; i <= MAX_ABI_ENTRY_PARAMS; i++) {
      b.addInputs(param("uint256", "p"));
    }
    assertTrue(isEntryTooLarge(b.build()));
  }

  @Test
  public void paramCountAtBoundIsAllowed() {
    ABI.Entry.Builder b = ABI.Entry.newBuilder().setName("E");
    for (int i = 0; i < MAX_ABI_ENTRY_PARAMS; i++) {
      b.addInputs(param("uint256", "p"));
    }
    assertFalse(isEntryTooLarge(b.build()));
  }

  @Test
  public void identifierExactlyAtBoundIsAllowed() {
    ABI.Entry entry = ABI.Entry.newBuilder()
        .setName(repeat(MAX_ABI_IDENTIFIER_LENGTH)).build();
    assertFalse(isEntryTooLarge(entry));
  }
}
