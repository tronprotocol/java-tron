package org.tron.core.services.admin.ipc.client;

import org.junit.Assert;
import org.junit.Test;

public class IpcResponseTest {

  private static final String RESPONSE_PREFIX = "{\"jsonrpc\":\"2.0\",\"id\":1,";

  @Test
  public void testTextResultIsUnquoted() {
    assertResponse("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"done\"}", "done", true);
  }

  @Test
  public void testStructuredResultIsPrettyPrinted() {
    IpcResponse response = IpcResponse.parse(
        "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"height\":10,\"ready\":true}}");

    Assert.assertTrue(response.isSuccessful());
    Assert.assertEquals(String.join(System.lineSeparator(),
        "{", "  \"height\" : 10,", "  \"ready\" : true", "}"), response.getFormatted());
  }

  @Test
  public void testNullAndScalarResultsAreSuccessful() {
    assertResponse(RESPONSE_PREFIX + "\"result\":null}", "null", true);
    assertResponse(RESPONSE_PREFIX + "\"result\":false}", "false", true);
    assertResponse(RESPONSE_PREFIX + "\"result\":42}", "42", true);
  }

  @Test
  public void testValidRpcErrorsAreFormattedAndFail() {
    assertResponse(RESPONSE_PREFIX
        + "\"error\":{\"code\":-32602,\"message\":\"Invalid params\",\"data\":{}}}",
        "Error -32602: Invalid params", false);
    assertResponse("{\"jsonrpc\":\"2.0\",\"id\":null,"
        + "\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}",
        "Error -32700: Parse error", false);
  }

  @Test
  public void testMalformedResponseUsesFixedMessageAndFails() {
    for (String input : new String[] {"", "  ", "response", "{broken json"}) {
      assertInvalidResponse(input);
    }
  }

  @Test
  public void testInvalidEnvelopeIsRejected() {
    for (String input : new String[] {"null", "[]", "true", "\"message\"", "{\"result\":true}",
        "{\"id\":1,\"result\":true}", "{\"jsonrpc\":\"2.0\",\"result\":true}",
        "{\"jsonrpc\":\"1.0\",\"id\":1,\"result\":true}",
        "{\"jsonrpc\":2.0,\"id\":1,\"result\":true}",
        "{\"jsonrpc\":\"2.0\",\"id\":1}",
        RESPONSE_PREFIX + "\"result\":true,\"error\":null}",
        RESPONSE_PREFIX + "\"result\":null,\"error\":{\"code\":1,\"message\":\"error\"}}"}) {
      assertInvalidResponse(input);
    }
    for (String id : new String[] {"true", "{}", "[]"}) {
      assertInvalidResponse("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":true}");
    }
  }

  @Test
  public void testErrorRequiresIntegerCodeAndStringMessage() {
    for (String error : new String[] {"null", "false", "[]", "\"error\"", "{}",
        "{\"code\":1}", "{\"message\":\"Failed\"}",
        "{\"code\":\"1\",\"message\":\"Failed\"}",
        "{\"code\":1.5,\"message\":\"Failed\"}",
        "{\"code\":1,\"message\":null}", "{\"code\":1,\"message\":7}"}) {
      assertInvalidResponse(RESPONSE_PREFIX + "\"error\":" + error + "}");
    }
  }

  @Test
  public void testTrailingContentIsRejected() {
    String valid = RESPONSE_PREFIX + "\"result\":true}";
    assertResponse(valid + " \t\r\n", "true", true);
    for (String suffix : new String[] {" garbage", " {}", " null", " " + valid}) {
      assertInvalidResponse(valid + suffix);
    }
  }

  @Test
  public void testExpectedIdMustMatchWithoutCoercion() {
    for (String id : new String[] {"2", "\"1\"", "null", "4294967297", "1.5"}) {
      IpcResponse response = IpcResponse.parse(
          "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":true}", 1);
      Assert.assertFalse(response.isSuccessful());
      Assert.assertEquals("IPC response ID does not match request.", response.getFormatted());
    }
    IpcResponse matching = IpcResponse.parse(
        "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":null}", 2);
    Assert.assertTrue(matching.isSuccessful());
    Assert.assertEquals("null", matching.getFormatted());
    assertResponse("{\"jsonrpc\":\"2.0\",\"id\":\"one\",\"result\":true}", "true", true);
  }

  @Test
  public void testErrorIdMustAlsoMatchRequest() {
    String error = "{\"jsonrpc\":\"2.0\",\"id\":2,"
        + "\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}";
    IpcResponse mismatched = IpcResponse.parse(error, 1);
    Assert.assertFalse(mismatched.isSuccessful());
    Assert.assertEquals("IPC response ID does not match request.", mismatched.getFormatted());
    IpcResponse matching = IpcResponse.parse(error, 2);
    Assert.assertFalse(matching.isSuccessful());
    Assert.assertEquals("Error -32602: Invalid params", matching.getFormatted());
  }

  private void assertInvalidResponse(String input) {
    assertResponse(input, "Invalid IPC response.", false);
    IpcResponse response = IpcResponse.parse(input, 1);
    Assert.assertFalse(response.isSuccessful());
    Assert.assertEquals("Invalid IPC response.", response.getFormatted());
  }

  private void assertResponse(String input, String formatted, boolean successful) {
    IpcResponse response = IpcResponse.parse(input);
    Assert.assertEquals(formatted, response.getFormatted());
    Assert.assertEquals(successful, response.isSuccessful());
  }
}
