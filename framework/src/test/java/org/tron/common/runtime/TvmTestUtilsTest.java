package org.tron.common.runtime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.EntryType;

public class TvmTestUtilsTest {

  @Test
  public void jsonStr2AbiAcceptsReceiveEntry() {
    String json = "[{\"stateMutability\":\"payable\",\"type\":\"receive\"}]";
    ABI abi = TvmTestUtils.jsonStr2Abi(json);
    assertEquals(1, abi.getEntrysCount());
    Entry entry = abi.getEntrys(0);
    assertEquals(EntryType.Receive, entry.getType());
    assertEquals(0, entry.getInputsCount());
    assertEquals(0, entry.getOutputsCount());
  }
}
