package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.EntryType;

public class PublicMethodTest {

  @Test
  public void jsonStr2AbiAcceptsReceiveEntry() {
    String json = "[{\"stateMutability\":\"payable\",\"type\":\"receive\"}]";
    ABI abi = PublicMethod.jsonStr2Abi(json);
    assertEquals(1, abi.getEntrysCount());
    Entry entry = abi.getEntrys(0);
    assertEquals(EntryType.Receive, entry.getType());
    assertEquals(0, entry.getInputsCount());
    assertEquals(0, entry.getOutputsCount());
  }

  @Test
  public void getEntryTypeMapsReceive() {
    assertEquals(EntryType.Receive, PublicMethod.getEntryType("receive"));
  }

  @Test
  public void getEntryTypeUnknownStaysUnrecognized() {
    assertTrue(PublicMethod.getEntryType("weirdo") == EntryType.UNRECOGNIZED);
  }
}
