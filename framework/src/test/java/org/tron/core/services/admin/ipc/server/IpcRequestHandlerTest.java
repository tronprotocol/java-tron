package org.tron.core.services.admin.ipc.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.tron.core.Constant;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.AdminJsonRpcImpl;
import org.tron.core.services.admin.ipc.server.IpcRequestHandler.RequestTooLargeException;

public class IpcRequestHandlerTest {

  private static final int MAX_REQUEST_SIZE = 128;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String REQUEST_PREFIX =
      "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
          + "\"params\":[\"a\",\"b\"],\"id\":11,\"extra\":";
  private final IpcRequestHandler handler =
      new IpcRequestHandler(new AdminJsonRpcImpl(), MAX_REQUEST_SIZE);

  @Test
  public void testHandleCommandReturnsSingleLineJsonResponse() throws Exception {
    String response = handler.handleCommand(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
            + "\"params\":[\"a\\nb\",\"c\\rd\"],\"id\":7}");

    Assert.assertFalse(response, response.contains("\n"));
    Assert.assertFalse(response, response.contains("\r"));
    JsonNode result = OBJECT_MAPPER.readTree(response);
    Assert.assertEquals("a\nb:c\rd", result.get("result").asText());
    Assert.assertEquals(7, result.get("id").asInt());
  }

  @Test
  public void testHandleCommandReturnsJsonRpcErrorOnDispatcherFailure() throws Exception {
    try (MockedConstruction<JsonRpcServer> servers = Mockito.mockConstruction(JsonRpcServer.class,
        (server, context) -> Mockito.doThrow(new IOException("sensitive-detail"))
            .when(server).handleRequest(Mockito.any(InputStream.class),
                Mockito.any(OutputStream.class)))) {
      IpcRequestHandler failingHandler =
          new IpcRequestHandler(new AdminJsonRpcImpl(), MAX_REQUEST_SIZE);
      String response = failingHandler.handleCommand(
          "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
              + "\"params\":[\"a\",\"b\"],\"id\":9}");
      JsonNode responseNode = OBJECT_MAPPER.readTree(response);

      Assert.assertEquals(1, servers.constructed().size());
      Assert.assertEquals("2.0", responseNode.get("jsonrpc").asText());
      Assert.assertEquals(-32603, responseNode.get("error").get("code").asInt());
      Assert.assertEquals("Internal error", responseNode.get("error").get("message").asText());
      Assert.assertEquals(9, responseNode.get("id").asInt());
      Assert.assertFalse(response, response.contains("sensitive-detail"));
      Assert.assertFalse(response, response.contains("\n"));

    }
  }

  @Test
  public void testHandleCommandUsesAnnotatedErrorResolver() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.adminExample("a", "b"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));
    IpcRequestHandler errorHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);

    String response = errorHandler.handleCommand(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
            + "\"params\":[\"a\",\"b\"],\"id\":10}");
    JsonNode responseNode = OBJECT_MAPPER.readTree(response);

    Assert.assertEquals(-32602, responseNode.get("error").get("code").asInt());
    Assert.assertEquals("Invalid admin parameters",
        responseNode.get("error").get("message").asText());
    Assert.assertEquals(10, responseNode.get("id").asInt());
  }

  @Test
  public void testNotificationInvokesMethodWithoutResponse() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    IpcRequestHandler notificationHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);

    Assert.assertEquals("", notificationHandler.handleCommand(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"params\":[\"a\",\"b\"]}"));
    Mockito.verify(adminJsonRpc).adminExample("a", "b");
  }

  @Test
  public void testEmptyBatchIsRejectedBeforeInvocation() throws Exception {
    assertBatchRejected("[]");
  }

  @Test
  public void testBatchRequestsAreRejectedBeforeInvocation() throws Exception {
    String first = REQUEST_PREFIX + "0}";
    String second = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"c\",\"d\"],\"id\":12}";

    assertBatchRejected("[" + first + "]");
    assertBatchRejected("[" + first + "," + second + "]");
  }

  @Test
  public void testNotificationOnlyBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"a\",\"b\"]}";

    assertBatchRejected("[" + notification + "]");
    assertBatchRejected("[" + notification + "," + notification + "]");
  }

  @Test
  public void testMixedBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"c\",\"d\"]}";
    String request = REQUEST_PREFIX + "0}";

    assertBatchRejected("[" + notification + "," + request + "]");
    assertBatchRejected("[" + request + "," + notification + "]");
  }

  @Test
  public void testIpcMapperRejectsExcessiveNestingBeforeInvocation() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
    IpcRequestHandler constrainedHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);
    JsonNode valid = OBJECT_MAPPER.readTree(
        constrainedHandler.handleCommand(REQUEST_PREFIX + "[0]}"));
    Assert.assertEquals("a:b", valid.get("result").asText());
    Mockito.verify(adminJsonRpc).adminExample("a", "b");
    Mockito.clearInvocations(adminJsonRpc);

    StringBuilder nested = new StringBuilder();
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      nested.append('[');
    }
    nested.append('0');
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      nested.append(']');
    }
    assertParseError(constrainedHandler.handleCommand(REQUEST_PREFIX + nested + "}"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void testIpcMapperRejectsExcessiveTokensBeforeInvocation() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
    IpcRequestHandler constrainedHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);
    JsonNode valid = OBJECT_MAPPER.readTree(
        constrainedHandler.handleCommand(REQUEST_PREFIX + "[0]}"));
    Assert.assertEquals("a:b", valid.get("result").asText());
    Mockito.verify(adminJsonRpc).adminExample("a", "b");
    Mockito.clearInvocations(adminJsonRpc);

    StringBuilder request = new StringBuilder(REQUEST_PREFIX + "[");
    for (int i = 0; i < Constant.MAX_TOKEN_COUNT; i++) {
      if (i > 0) {
        request.append(',');
      }
      request.append('0');
    }
    request.append("]}");

    assertParseError(constrainedHandler.handleCommand(request.toString()));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void testInvalidJsonReturnsParseErrorBeforeInvocation() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    IpcRequestHandler constrainedHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);
    String valid = REQUEST_PREFIX + "0}";
    for (String body : new String[] {"", " \t\r\n", "{broken-sensitive-detail", REQUEST_PREFIX,
        valid + " {broken", valid + " " + valid, "[" + valid,
        "[" + valid + ",{broken]", "[" + valid + "] " + valid}) {
      assertParseError(constrainedHandler.handleCommand(body));
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void testStringNullRequestIdIsPreserved() throws Exception {
    JsonNode response = OBJECT_MAPPER.readTree(handler.handleCommand(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
            + "\"params\":[\"a\",\"b\"],\"id\":\"null\"}"));

    Assert.assertTrue(response.get("id").isTextual());
    Assert.assertEquals("null", response.get("id").asText());
    Assert.assertEquals("a:b", response.get("result").asText());
  }

  @Test
  public void testReadRequestAcceptsMaximumSize() throws Exception {
    byte[] request = new byte[MAX_REQUEST_SIZE + 1];
    Arrays.fill(request, 0, MAX_REQUEST_SIZE, (byte) '1');
    request[MAX_REQUEST_SIZE] = '\n';

    Assert.assertEquals(MAX_REQUEST_SIZE,
        handler.readRequest(new ByteArrayInputStream(request)).length());
  }

  @Test(expected = RequestTooLargeException.class)
  public void testReadRequestRejectsOversizedInputWithoutNewline() throws Exception {
    handler.readRequest(new ByteArrayInputStream(new byte[MAX_REQUEST_SIZE + 1]));
  }

  @Test
  public void testReadRequestPreservesFramesAndDistinguishesEmptyLineFromEof() throws Exception {
    ByteArrayInputStream input = new ByteArrayInputStream(
        "first\r\n\n  second  \nlast".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals("first", handler.readRequest(input));
    Assert.assertEquals("", handler.readRequest(input));
    Assert.assertEquals("  second  ", handler.readRequest(input));
    Assert.assertEquals("last", handler.readRequest(input));
    Assert.assertNull(handler.readRequest(input));
  }

  @Test
  public void testReadRequestCountsBytesAndDecodesUtf8() throws Exception {
    IpcRequestHandler limited = new IpcRequestHandler(new AdminJsonRpcImpl(), 3);
    Assert.assertEquals("中", limited.readRequest(new ByteArrayInputStream(
        "中\n".getBytes(StandardCharsets.UTF_8))));
    try {
      limited.readRequest(new ByteArrayInputStream("中文\n".getBytes(StandardCharsets.UTF_8)));
      Assert.fail("Expected multi-byte input to exceed the byte limit");
    } catch (RequestTooLargeException expected) {
      // A character count would incorrectly accept both characters.
    }
  }

  @Test
  public void testZeroLimitOnlyAcceptsEmptyLines() throws Exception {
    IpcRequestHandler zeroLimit = new IpcRequestHandler(new AdminJsonRpcImpl(), 0);
    Assert.assertEquals(0, zeroLimit.getMaxRequestSize());
    Assert.assertEquals("", zeroLimit.readRequest(new ByteArrayInputStream(new byte[] {'\n'})));
    Assert.assertNull(zeroLimit.readRequest(new ByteArrayInputStream(new byte[0])));
    try {
      zeroLimit.readRequest(new ByteArrayInputStream(new byte[] {'a'}));
      Assert.fail("Expected a configured zero limit to reject nonempty input");
    } catch (RequestTooLargeException expected) {
      // Zero must not silently fall back to a default limit.
    }
  }

  private void assertParseError(String response) throws Exception {
    Assert.assertFalse(response.contains("\n"));
    Assert.assertFalse(response.contains("\r"));
    JsonNode error = OBJECT_MAPPER.readTree(response);
    Assert.assertEquals("2.0", error.get("jsonrpc").asText());
    Assert.assertTrue(error.get("id").isNull());
    Assert.assertEquals(-32700, error.get("error").get("code").asInt());
    Assert.assertEquals("JSON parse error", error.get("error").get("message").asText());
    Assert.assertEquals(2, error.get("error").size());
    Assert.assertFalse(error.has("result"));
  }

  private void assertBatchRejected(String body) throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    IpcRequestHandler rejectingHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);

    String response = rejectingHandler.handleCommand(body);

    Assert.assertFalse(response.contains("\n"));
    Assert.assertFalse(response.contains("\r"));
    JsonNode error = OBJECT_MAPPER.readTree(response);
    Assert.assertTrue(error.isObject());
    Assert.assertEquals("2.0", error.get("jsonrpc").asText());
    Assert.assertTrue(error.get("id").isNull());
    Assert.assertEquals(-32600, error.get("error").get("code").asInt());
    Assert.assertEquals("Batch requests are not supported",
        error.get("error").get("message").asText());
    Assert.assertEquals(2, error.get("error").size());
    Assert.assertFalse(error.has("result"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }
}
