package org.tron.core.services.admin.ipc.client;

import org.junit.Assert;
import org.junit.Test;

public class IpcResponseTest {

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
    assertResponse("{\"result\":null}", "null", true);
    assertResponse("{\"result\":false}", "false", true);
    assertResponse("{\"result\":42,\"error\":null}", "42", true);
  }

  @Test
  public void testErrorTakesPrecedenceOverResult() {
    assertResponse("{\"result\":\"ignored\","
        + "\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}",
        "Error -32602: Invalid params", false);
    assertResponse("{\"error\":{\"message\":\"Failed\"}}", "Error: Failed", false);
    assertResponse("{\"error\":{}}", "Error: Unknown error", false);
  }

  @Test
  public void testMalformedResponsePreservesTextAndFails() {
    for (String input : new String[] {"", "  ", "response", "{broken json"}) {
      assertResponse(input, input, false);
    }
  }

  @Test
  public void testResponseWithoutResultFails() {
    assertResponse("null", "null", false);
    assertResponse("\"message\"", "message", false);
    assertResponse("{\"id\":1}", String.join(System.lineSeparator(),
        "{", "  \"id\" : 1", "}"), false);
  }

  private void assertResponse(String input, String formatted, boolean successful) {
    IpcResponse response = IpcResponse.parse(input);
    Assert.assertEquals(formatted, response.getFormatted());
    Assert.assertEquals(successful, response.isSuccessful());
  }
}
