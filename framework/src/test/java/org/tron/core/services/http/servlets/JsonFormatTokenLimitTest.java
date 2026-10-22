package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import org.junit.Test;
import org.tron.core.Constant;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

public class JsonFormatTokenLimitTest {

  @Test
  public void testExactLimitWithStringSeparatorsAndIndependentMerges() throws Exception {
    // The object, field name and array consume five tokens. Each quoted value is one token,
    // even when its contents are a comma or colon.
    String input = "{\"unknown\":["
        + repeat("\",:\"", Constant.MAX_TOKEN_COUNT - 5) + "]}";
    assertEquals(Constant.MAX_TOKEN_COUNT, jacksonTokenCount(input));

    JsonFormat.merge(input, Protocol.HelloMessage.newBuilder());
    // A fresh merge gets a fresh budget; whitespace and EOF must not consume it.
    JsonFormat.merge(new StringReader(input + " \n\t"), Protocol.HelloMessage.newBuilder());
  }

  @Test
  public void testOneTokenOverLimitAndRecovery() throws Exception {
    String input = "{\"unknown\":["
        + repeat("0", Constant.MAX_TOKEN_COUNT - 4) + "]}";
    assertEquals(Constant.MAX_TOKEN_COUNT + 1, jacksonTokenCount(input));

    assertTokenLimit(assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge(input, Protocol.HelloMessage.newBuilder())));
    assertTokenLimit(assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge(new StringReader(input), Protocol.HelloMessage.newBuilder())));

    String atLimit = "{\"unknown\":["
        + repeat("0", Constant.MAX_TOKEN_COUNT - 5) + "]}";
    JsonFormat.merge(atLimit, Protocol.HelloMessage.newBuilder());
  }

  @Test
  public void testFieldNamesAndContainerBoundariesCount() throws Exception {
    String atLimit = "{" + repeat("\"unknown\":0", (Constant.MAX_TOKEN_COUNT - 2) / 2) + "}";
    assertEquals(Constant.MAX_TOKEN_COUNT, jacksonTokenCount(atLimit));
    JsonFormat.merge(atLimit, Protocol.HelloMessage.newBuilder());

    // Wrapping an existing scalar adds exactly one START_ARRAY and one END_ARRAY.
    String overLimit = atLimit.replaceFirst(":0", ":[0]");
    assertEquals(Constant.MAX_TOKEN_COUNT + 2, jacksonTokenCount(overLimit));
    assertTokenLimit(assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge(overLimit, Protocol.HelloMessage.newBuilder())));
  }

  @Test
  public void testWideAbiStopsBeforeAllEntriesAreBuilt() throws Exception {
    int entryCount = Constant.MAX_TOKEN_COUNT / 2;
    String input = "{\"entrys\":[" + repeat("{}", entryCount) + "]}";
    assertEquals(Constant.MAX_TOKEN_COUNT + 5, jacksonTokenCount(input));
    ABI.Builder builder = ABI.newBuilder();

    assertTokenLimit(assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge(input, builder)));
    assertTrue(builder.getEntrysCount() > 0);
    assertTrue(builder.getEntrysCount() < entryCount);
  }

  @Test
  public void testNestedKnownAndUnknownFieldsShareBudget() throws Exception {
    String entry = "{\"inputs\":["
        + repeat("{\"name\":\"p\",\"type\":\"uint256\"}", Constant.MAX_TOKEN_COUNT / 10)
        + "]}";
    String knownFields = "{\"entrys\":[" + entry + "]}";
    assertTrue(jacksonTokenCount(knownFields) < Constant.MAX_TOKEN_COUNT);
    JsonFormat.merge(knownFields, ABI.newBuilder());

    String input = "{\"entrys\":[" + entry + "],\"unknown\":["
        + repeat("0", Constant.MAX_TOKEN_COUNT / 2) + "]}";
    assertTrue(jacksonTokenCount(input) > Constant.MAX_TOKEN_COUNT);
    assertTokenLimit(assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge(input, ABI.newBuilder())));
  }

  private static String repeat(String value, int count) {
    return String.join(",", Collections.nCopies(count, value));
  }

  private static long jacksonTokenCount(String input) throws IOException {
    long count = 0;
    try (JsonParser parser = new JsonFactory().createParser(input)) {
      while (parser.nextToken() != null) {
        count++;
      }
    }
    return count;
  }

  private static void assertTokenLimit(JsonFormat.ParseException exception) {
    assertEquals(JsonFormat.ParseException.class, exception.getClass());
    assertEquals("Token count exceeds the maximum allowed (" + Constant.MAX_TOKEN_COUNT + ").",
        exception.getMessage());
  }
}
