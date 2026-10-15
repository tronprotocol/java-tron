package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.tron.core.services.http.servlets.JsonFormatTestSupport.assertBoundedWithoutRepeatedInput;
import static org.tron.core.services.http.servlets.JsonFormatTestSupport.mergeAbi;
import static org.tron.core.services.http.servlets.JsonFormatTestSupport.repeat;

import org.junit.Test;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType;

public class JsonFormatIntegerTokenTest {

  private static void assertOutOfRangeWithoutRun(NumberFormatException error,
      String expectedMessage, char input) {
    assertEquals(expectedMessage, error.getMessage());
    assertBoundedWithoutRepeatedInput(error.getMessage(), input);
  }

  private static void assertWrappedOutOfRangeWithoutRun(JsonFormat.ParseException error,
      String expectedMessage, char input) {
    assertTrue("Missing semantic range error",
        error.getMessage().contains(expectedMessage));
    assertBoundedWithoutRepeatedInput(error.getMessage(), input);
  }

  @Test
  public void validAbiStillParses() throws Exception {
    ABI abi = mergeAbi("[{\"name\":\"x\",\"type\":\"Function\","
        + "\"stateMutability\":\"Payable\"}]");

    assertEquals(1, abi.getEntrysCount());
    Entry entry = abi.getEntrys(0);
    assertEquals(Entry.EntryType.Function, entry.getType());
    assertEquals(StateMutabilityType.Payable, entry.getStateMutability());
  }

  @Test
  public void numericEnumStillParses() throws Exception {
    ABI abi = mergeAbi("[{\"name\":\"x\",\"stateMutability\":4}]");

    assertEquals(StateMutabilityType.Payable, abi.getEntrys(0).getStateMutability());
  }

  @Test
  public void tokenAtRawLimitStillParses() throws Exception {
    String token = repeat('0', JsonFormat.MAX_INTEGER_TOKEN_LENGTH - 1) + "4";

    ABI abi = mergeAbi("[{\"name\":\"x\",\"stateMutability\":" + token + "}]");

    assertEquals(JsonFormat.MAX_INTEGER_TOKEN_LENGTH, token.length());
    assertEquals(StateMutabilityType.Payable, abi.getEntrys(0).getStateMutability());
  }

  @Test
  public void tokenOverRawLimitIsRejectedWithoutEcho() {
    String token = repeat('0', JsonFormat.MAX_INTEGER_TOKEN_LENGTH) + "4";

    JsonFormat.ParseException error = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"name\":\"x\",\"stateMutability\":" + token + "}]"));

    assertTrue(error.getMessage().contains("Integer token is too long"));
    assertBoundedWithoutRepeatedInput(error.getMessage(), '0');
  }

  @Test
  public void rawLimitRunsBeforeUnsignedSignValidation() {
    String token = "-" + repeat('9', 100_000);

    NumberFormatException error = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseUInt64(token));

    assertEquals("Integer token is too long: length 100001, max 256", error.getMessage());
    assertBoundedWithoutRepeatedInput(error.getMessage(), '9');
  }

  @Test
  public void unsignedNegativeErrorsDoNotReflectToken() {
    String token = "-" + repeat('9', 100);

    NumberFormatException uint32Error = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseUInt32(token));
    NumberFormatException uint64Error = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseUInt64(token));

    assertEquals("Number must be positive.", uint32Error.getMessage());
    assertEquals("Number must be positive.", uint64Error.getMessage());
    assertBoundedWithoutRepeatedInput(uint32Error.getMessage(), '9');
    assertBoundedWithoutRepeatedInput(uint64Error.getMessage(), '9');
  }

  @Test
  public void rawLimitIncludesRadixPrefixAndInnerSign() {
    String atLimit = "0x-" + repeat('0', JsonFormat.MAX_INTEGER_TOKEN_LENGTH - 4) + "1";
    String overLimit = "0x-" + repeat('0', JsonFormat.MAX_INTEGER_TOKEN_LENGTH - 3) + "1";

    assertEquals(JsonFormat.MAX_INTEGER_TOKEN_LENGTH, atLimit.length());
    assertEquals(-1L, JsonFormat.parseInt64(atLimit));
    NumberFormatException error = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(overLimit));
    assertTrue(error.getMessage().contains("Integer token is too long"));
  }

  @Test
  public void signedInt64ExactBoundariesArePreserved() {
    assertEquals(Long.MAX_VALUE, JsonFormat.parseInt64("9223372036854775807"));
    assertEquals(Long.MIN_VALUE, JsonFormat.parseInt64("-9223372036854775808"));

    NumberFormatException aboveMax = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64("9223372036854775808"));
    NumberFormatException belowMin = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64("-9223372036854775809"));

    assertEquals("Number out of range for 64-bit signed integer.", aboveMax.getMessage());
    assertEquals("Number out of range for 64-bit signed integer.", belowMin.getMessage());
  }

  @Test
  public void decimalRangeErrorsAreBounded() {
    String twentyDigitValue = repeat('9', 20);
    String twentyOneDigitValue = repeat('9', 21);
    NumberFormatException twentyDigitError = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(twentyDigitValue));
    NumberFormatException twentyOneDigitError = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(twentyOneDigitValue));

    String expectedMessage = "Number out of range for 64-bit signed integer.";
    assertOutOfRangeWithoutRun(twentyDigitError, expectedMessage, '9');
    assertOutOfRangeWithoutRun(twentyOneDigitError, expectedMessage, '9');
  }

  @Test
  public void octalRangeErrorsAreBounded() {
    String twentyTwoDigitValue = "0" + repeat('7', 22);
    String twentyThreeDigitValue = "0" + repeat('7', 23);
    NumberFormatException twentyTwoDigitError = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(twentyTwoDigitValue));
    NumberFormatException twentyThreeDigitError = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(twentyThreeDigitValue));

    String expectedMessage = "Number out of range for 64-bit signed integer.";
    assertOutOfRangeWithoutRun(twentyTwoDigitError, expectedMessage, '7');
    assertOutOfRangeWithoutRun(twentyThreeDigitError, expectedMessage, '7');
  }

  @Test
  public void hexadecimalRangeErrorsAreBounded() {
    String sixteenDigitValue = "0x" + repeat('F', 16);
    String seventeenDigitValue = "0x" + repeat('F', 17);
    NumberFormatException sixteenDigitError = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(sixteenDigitValue));
    NumberFormatException seventeenDigitError = assertThrows(NumberFormatException.class,
        () -> JsonFormat.parseInt64(seventeenDigitValue));

    String expectedMessage = "Number out of range for 64-bit signed integer.";
    assertOutOfRangeWithoutRun(sixteenDigitError, expectedMessage, 'F');
    assertOutOfRangeWithoutRun(seventeenDigitError, expectedMessage, 'F');
  }

  @Test
  public void enumFieldsAndUnknownFieldsUseTheGuardedParser() {
    String value = repeat('9', 21);

    JsonFormat.ParseException typeError = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"name\":\"x\",\"type\":" + value + "}]"));
    JsonFormat.ParseException mutabilityError = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"name\":\"x\",\"stateMutability\":" + value + "}]"));
    JsonFormat.ParseException unknownError = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"unknown\":" + value + "}]"));

    String enumMessage = "Number out of range for 32-bit signed integer.";
    String unknownFieldMessage = "Number out of range for 64-bit signed integer.";
    assertWrappedOutOfRangeWithoutRun(typeError, enumMessage, '9');
    assertWrappedOutOfRangeWithoutRun(mutabilityError, enumMessage, '9');
    assertWrappedOutOfRangeWithoutRun(unknownError, unknownFieldMessage, '9');
  }

  @Test
  public void invalidTrailingCharactersRemainSyntaxErrorsInBothParserBranches() {
    String[] values = {
        repeat('9', 14) + "z", // Long.parseLong branch: numberText length is below 16.
        repeat('9', 19) + "z", // BigInteger branch.
        repeat('9', 20) + "z",
        "0" + repeat('7', 21) + "z",
        "0" + repeat('7', 22) + "z",
        "0x" + repeat('F', 15) + "z",
        "0x" + repeat('F', 16) + "z"
    };

    for (String value : values) {
      JsonFormat.ParseException error = assertThrows(JsonFormat.ParseException.class,
          () -> mergeAbi("[{\"unknown\":" + value + "}]"));

      assertTrue(error.getMessage().contains("Couldn't parse integer"));
      assertFalse(error.getMessage().contains(
          "Number out of range for 64-bit signed integer."));
      assertTrue("Unexpectedly long error message", error.getMessage().length() < 512);
    }
  }

  @Test
  public void signsAndRadixPrefixesKeepTheirExistingSemantics() throws Exception {
    assertEquals(-Long.MAX_VALUE, JsonFormat.parseInt64("0x-7FFFFFFFFFFFFFFF"));
    assertEquals(-1L, JsonFormat.parseInt64("0x-0000000000000001"));
    assertEquals(Long.MAX_VALUE, JsonFormat.parseInt64("-0x-7FFFFFFFFFFFFFFF"));

    mergeAbi("[{\"unknown\":0x-7FFFFFFFFFFFFFFF}]");
    mergeAbi("[{\"unknown\":0x-0000000000000001}]");
    mergeAbi("[{\"unknown\":-0x-7FFFFFFFFFFFFFFF}]");

    ABI abi = mergeAbi("[{\"name\":\"x\",\"stateMutability\":+"
        + repeat('0', 20) + "4}]");
    assertEquals(StateMutabilityType.Payable, abi.getEntrys(0).getStateMutability());
  }

  @Test
  public void unicodeLeadingZerosKeepBigIntegerCompatibility() {
    String value = repeat((char) 0x0660, 20) + "4";

    assertEquals(4L, JsonFormat.parseInt64(value));
  }

  @Test
  public void largeAbiIntegerIsRejectedWithBoundedMessage() {
    String value = repeat('9', 100_000);

    JsonFormat.ParseException error = assertThrows(JsonFormat.ParseException.class,
        () -> mergeAbi("[{\"unknown\":" + value + "}]"));

    assertTrue(error.getMessage().contains("Integer token is too long"));
    assertBoundedWithoutRepeatedInput(error.getMessage(), '9');
  }
}
