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

      JsonNode malformed = OBJECT_MAPPER.readTree(failingHandler.handleCommand("{broken"));
      Assert.assertEquals(-32603, malformed.get("error").get("code").asInt());
      Assert.assertTrue(malformed.get("id").isNull());
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
  public void testIpcMapperRejectsExcessiveNestingBeforeInvocation() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
    IpcRequestHandler constrainedHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);
    String requestPrefix = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"a\",\"b\"],\"id\":11,\"extra\":";
    JsonNode valid = OBJECT_MAPPER.readTree(
        constrainedHandler.handleCommand(requestPrefix + "[0]}"));
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
    JsonNode rejected = OBJECT_MAPPER.readTree(
        constrainedHandler.handleCommand(requestPrefix + nested + "}"));
    Assert.assertTrue(rejected.toString(), rejected.has("error"));
    Assert.assertFalse(rejected.has("result"));
    Mockito.verifyNoInteractions(adminJsonRpc);
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
}
