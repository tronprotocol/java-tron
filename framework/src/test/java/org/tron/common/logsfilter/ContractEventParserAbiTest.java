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

  private ABI abiWithEntries(int count) {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < count; i++) {
      b.addEntrys(ABI.Entry.newBuilder().setType(ABI.Entry.EntryType.Event));
    }
    return b.build();
  }

  @Test
  public void normalSizedAbiIsDecoded() {
    assertFalse(hasTooManyEntries(abiWithEntries(MAX_ABI_ENTRIES)));
  }

  @Test
  public void oversizedAbiIsNotDecoded() {
    assertTrue(hasTooManyEntries(abiWithEntries(MAX_ABI_ENTRIES + 1)));
  }

  @Test
  public void emptyAbiIsWithinLimit() {
    assertFalse(hasTooManyEntries(ABI.getDefaultInstance()));
  }

  @Test
  public void eventCountAtBoundIsAllowed() {
    assertFalse(hasTooManyEvents(abiWithEntries(MAX_ABI_EVENTS)));
  }

  @Test
  public void tooManyEventsIsRejected() {
    assertTrue(hasTooManyEvents(abiWithEntries(MAX_ABI_EVENTS + 1)));
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
