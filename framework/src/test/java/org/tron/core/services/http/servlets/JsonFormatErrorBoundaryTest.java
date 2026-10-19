package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.tron.core.services.http.servlets.JsonFormatTestSupport.assertBoundedWithoutRepeatedInput;
import static org.tron.core.services.http.servlets.JsonFormatTestSupport.mergeAbi;

import com.google.common.base.Strings;
import org.junit.Test;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class JsonFormatErrorBoundaryTest {

  private static final int LARGE_TOKEN_LENGTH = 65_536;

  @Test
  public void oversizedEnumIdentifierIsRejectedWithoutEcho() {
    String value = Strings.repeat("9", 100_000);

    JsonFormat.ParseException error = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"name\":\"x\",\"type\":\"" + value + "\"}]"));

    assertTrue(error.getMessage().contains("Enum token is too long"));
    assertBoundedWithoutRepeatedInput(error.getMessage(), '9');
  }

  @Test
  public void enumRawLimitIncludesQuotes() {
    String atLimit = Strings.repeat("A", JsonFormat.MAX_ENUM_TOKEN_LENGTH - 2);
    String overLimit = Strings.repeat("A", JsonFormat.MAX_ENUM_TOKEN_LENGTH - 1);

    JsonFormat.ParseException atLimitError = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"name\":\"x\",\"type\":\"" + atLimit + "\"}]"));
    JsonFormat.ParseException overLimitError = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"name\":\"x\",\"type\":\"" + overLimit + "\"}]"));

    // Values at the enum-token limit retain a bounded identifier prefix for diagnostics.
    // Oversized enum tokens take the fixed no-echo rejection path.
    String expectedPrefix = Strings.repeat("A", 64) + "...(truncated)";
    assertFalse(atLimitError.getMessage().contains("Enum token is too long"));
    assertFalse(atLimitError.getMessage().contains(atLimit));
    assertTrue(atLimitError.getMessage().contains(expectedPrefix));
    assertTrue("Unexpectedly long error message", atLimitError.getMessage().length() < 512);
    assertTrue(overLimitError.getMessage().contains("Enum token is too long"));
    assertBoundedWithoutRepeatedInput(overLimitError.getMessage(), 'A');
  }

  @Test
  public void oversizedUnknownFieldNameRemainsIgnored() throws Exception {
    String unknownName = Strings.repeat("a", 100_000);

    ABI abi = mergeAbi("[{\"name\":\"x\",\"" + unknownName + "\":1}]");

    assertEquals(1, abi.getEntrysCount());
    assertEquals("x", abi.getEntrys(0).getName());
  }

  @Test
  public void largeStringTokenRemainsAccepted() throws Exception {
    String value = Strings.repeat("A", LARGE_TOKEN_LENGTH);
    Entry.Builder builder = Entry.newBuilder();

    JsonFormat.merge("{\"name\":\"" + value + "\"}", builder, false);

    assertEquals(value, builder.getName());
  }

  @Test
  public void largeBytesTokenRemainsAccepted() throws Exception {
    String value = Strings.repeat("ab", LARGE_TOKEN_LENGTH / 2);
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();

    JsonFormat.merge("{\"data\":\"" + value + "\"}", builder, false);

    assertEquals(LARGE_TOKEN_LENGTH / 2, builder.getData().size());
    assertEquals((byte) 0xab, builder.getData().byteAt(0));
    assertEquals((byte) 0xab, builder.getData().byteAt(builder.getData().size() - 1));
  }
}
