package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

final class JsonFormatTestSupport {

  private static final int MAX_ERROR_MESSAGE_LENGTH = 512;
  private static final int REFLECTED_INPUT_RUN_LENGTH = 8;

  private JsonFormatTestSupport() {
  }

  static String repeat(char value, int count) {
    return Strings.repeat(String.valueOf(value), count);
  }

  static ABI mergeAbi(String entries) throws JsonFormat.ParseException {
    ABI.Builder builder = ABI.newBuilder();
    JsonFormat.merge("{\"entrys\":" + entries + "}", builder, false);
    return builder.build();
  }

  static void assertBoundedWithoutRepeatedInput(String message, char input) {
    assertTrue("Unexpectedly long error message", message.length() < MAX_ERROR_MESSAGE_LENGTH);
    assertFalse("Error message contains the input prefix",
        message.contains(repeat(input, REFLECTED_INPUT_RUN_LENGTH)));
  }
}
