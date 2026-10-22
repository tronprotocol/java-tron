package org.tron.core.services.admin.ipc.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.core.Constant;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.service.peermanagement.BlockedIpInfo;
import org.tron.core.net.service.peermanagement.PeerManagementService;
import org.tron.core.net.service.peermanagement.PeerOperationResult;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.AdminJsonRpcImpl;
import org.tron.core.services.admin.ipc.server.IpcRequestHandler.RequestTooLargeException;

public class IpcRequestHandlerTest {

  private static final int MAX_REQUEST_SIZE = 128;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String REQUEST_PREFIX =
      "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
          + "\"params\":[\"a\"],\"id\":11,\"extra\":";
  private final IpcRequestHandler handler =
      new IpcRequestHandler(Mockito.mock(AdminJsonRpc.class), MAX_REQUEST_SIZE);

  @Test
  public void testHandleCommandReturnsSingleLineJsonResponse() throws Exception {
    AdminJsonRpc api = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(api.addPeer("192.0.2.20:18888"))
        .thenReturn(new PeerOperationResult(false, false, 0, "a\nb:c\rd"));
    IpcRequestHandler handler = new IpcRequestHandler(api, MAX_REQUEST_SIZE);
    String response = handle(handler,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"192.0.2.20:18888\"],\"id\":7}");

    Assert.assertFalse(response, response.contains("\n"));
    Assert.assertFalse(response, response.contains("\r"));
    JsonNode result = OBJECT_MAPPER.readTree(response);
    Assert.assertEquals("a\nb:c\rd", result.get("result").get("errorMessage").asText());
    Assert.assertFalse(result.get("result").get("success").asBoolean());
    Assert.assertFalse(result.get("result").has("message"));
    Assert.assertFalse(result.has("error"));
    Assert.assertEquals(7, result.get("id").asInt());
  }

  @Test
  public void testBlockedIpQueryIncludesCreationTimeOverIpc() throws Exception {
    PeerManagementService peerManagementService = Mockito.mock(PeerManagementService.class);
    Mockito.when(peerManagementService.listBlockedIps()).thenReturn(Collections.singletonList(
        new BlockedIpInfo("192.0.2.20", 1_790_000_000_000L)));
    IpcRequestHandler handler = new IpcRequestHandler(
        new AdminJsonRpcImpl(peerManagementService), MAX_REQUEST_SIZE);

    JsonNode response = OBJECT_MAPPER.readTree(handle(handler,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":8}"));

    Assert.assertEquals(8, response.get("id").asInt());
    Assert.assertEquals(OBJECT_MAPPER.readTree(
        "[{\"ip\":\"192.0.2.20\",\"blockedAtMillis\":1790000000000}]"), response.get("result"));
    Mockito.verify(peerManagementService).listBlockedIps();
  }

  @Test
  public void testHandleCommandUsesAnnotatedErrorResolver() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.addPeer("invalid"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));
    IpcRequestHandler errorHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);

    String response = handle(errorHandler,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"invalid\"],\"id\":10}");
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

    Assert.assertEquals("", handle(notificationHandler,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"params\":[]}"));
    Mockito.verify(adminJsonRpc).listBlockedIps();
  }

  @Test
  public void testEmptyBatchIsRejectedBeforeInvocation() throws Exception {
    assertBatchRejected("[]");
  }

  @Test
  public void testBatchRequestsAreRejectedBeforeInvocation() throws Exception {
    String first = REQUEST_PREFIX + "0}";
    String second = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"c\"],\"id\":12}";

    assertBatchRejected("[" + first + "]");
    assertBatchRejected("[" + first + "," + second + "]");
  }

  @Test
  public void testNotificationOnlyBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"a\"]}";

    assertBatchRejected("[" + notification + "]");
    assertBatchRejected("[" + notification + "," + notification + "]");
  }

  @Test
  public void testMixedBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"c\"]}";
    String request = REQUEST_PREFIX + "0}";

    assertBatchRejected("[" + notification + "," + request + "]");
    assertBatchRejected("[" + request + "," + notification + "]");
  }

  @Test
  public void testIpcMapperRejectsExcessiveNestingBeforeInvocation() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.listBlockedIps()).thenReturn(Collections.singletonList(
        new BlockedIpInfo("192.0.2.20", 1_790_000_000_000L)));
    IpcRequestHandler constrainedHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);
    String requestPrefix = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\","
        + "\"params\":[],\"id\":11,\"extra\":";
    JsonNode valid = OBJECT_MAPPER.readTree(
        handle(constrainedHandler, requestPrefix + "[0]}"));
    Assert.assertEquals("192.0.2.20", valid.get("result").get(0).get("ip").asText());
    Mockito.verify(adminJsonRpc).listBlockedIps();
    Mockito.clearInvocations(adminJsonRpc);

    StringBuilder nested = new StringBuilder();
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      nested.append('[');
    }
    nested.append('0');
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      nested.append(']');
    }
    assertParseError(handle(constrainedHandler, requestPrefix + nested + "}"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void testHandleCommandDispatchesPeerManagementMethod() throws Exception {
    PeerManagementService peerManagementService = Mockito.mock(PeerManagementService.class);
    Mockito.when(peerManagementService.addPeer("192.0.2.20:18888"))
        .thenReturn(new PeerOperationResult(true, true, 0, ""));
    IpcRequestHandler service = new IpcRequestHandler(
        new AdminJsonRpcImpl(peerManagementService), MAX_REQUEST_SIZE);

    String response = handle(service,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"192.0.2.20:18888\"],\"id\":8}");
    JsonNode result = new ObjectMapper().readTree(response).get("result");

    Assert.assertTrue(result.get("success").asBoolean());
    Assert.assertTrue(result.get("changed").asBoolean());
    Assert.assertEquals(0, result.get("disconnectedCount").asInt());
    Assert.assertEquals("", result.get("errorMessage").asText());
    Assert.assertFalse(result.has("message"));
    Mockito.verify(peerManagementService).addPeer("192.0.2.20:18888");
  }

  @Test
  public void testPeerManagementErrorsUseAnnotatedJsonRpcCodes() throws Exception {
    PeerManagementService peerManagementService = Mockito.mock(PeerManagementService.class);
    Mockito.when(peerManagementService.addPeer("invalid"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid peer endpoint"));
    Mockito.when(peerManagementService.addPeer("192.0.2.20:18888"))
        .thenThrow(new JsonRpcInternalException("P2P service is not ready"));
    IpcRequestHandler service = new IpcRequestHandler(
        new AdminJsonRpcImpl(peerManagementService), MAX_REQUEST_SIZE);

    JsonNode invalidParams = new ObjectMapper().readTree(handle(service,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"invalid\"],\"id\":11}"));
    JsonNode internalError = new ObjectMapper().readTree(handle(service,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"192.0.2.20:18888\"],\"id\":12}"));

    Assert.assertEquals(-32602, invalidParams.get("error").get("code").asInt());
    Assert.assertEquals("Invalid peer endpoint",
        invalidParams.get("error").get("message").asText());
    Assert.assertEquals(-32000, internalError.get("error").get("code").asInt());
    Assert.assertEquals("P2P service is not ready",
        internalError.get("error").get("message").asText());
  }

  @Test
  public void testIpcMapperRejectsExcessiveTokensBeforeInvocation() throws Exception {
    AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.addPeer("a"))
        .thenReturn(new PeerOperationResult(false, false, 0, "a:b"));
    IpcRequestHandler constrainedHandler = new IpcRequestHandler(adminJsonRpc, MAX_REQUEST_SIZE);
    JsonNode valid = OBJECT_MAPPER.readTree(
        handle(constrainedHandler, REQUEST_PREFIX + "[0]}"));
    Assert.assertEquals("a:b", valid.get("result").get("errorMessage").asText());
    Mockito.verify(adminJsonRpc).addPeer("a");
    Mockito.clearInvocations(adminJsonRpc);

    StringBuilder request = new StringBuilder(REQUEST_PREFIX + "[");
    for (int i = 0; i < Constant.MAX_TOKEN_COUNT; i++) {
      if (i > 0) {
        request.append(',');
      }
      request.append('0');
    }
    request.append("]}");

    assertParseError(handle(constrainedHandler, request.toString()));
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
      assertParseError(handle(constrainedHandler, body));
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void testStringNullRequestIdIsPreserved() throws Exception {
    JsonNode response = OBJECT_MAPPER.readTree(handle(handler,
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"a\"],\"id\":\"null\"}"));

    Assert.assertTrue(response.get("id").isTextual());
    Assert.assertEquals("null", response.get("id").asText());
    Assert.assertTrue(response.has("result"));
    Assert.assertFalse(response.has("error"));
  }

  @Test
  public void testReadRequestAcceptsMaximumSize() throws Exception {
    byte[] payload = new byte[MAX_REQUEST_SIZE];
    Arrays.fill(payload, (byte) '1');
    for (String terminator : new String[] {"\n", "\r\n"}) {
      String frames = new String(payload, StandardCharsets.UTF_8) + terminator + "next\n";
      ByteArrayInputStream input =
          new ByteArrayInputStream(frames.getBytes(StandardCharsets.UTF_8));

      Assert.assertEquals(MAX_REQUEST_SIZE, read(handler, input).length());
      Assert.assertEquals("next", read(handler, input));
      Assert.assertNull(read(handler, input));
    }
  }

  @Test(expected = RequestTooLargeException.class)
  public void testReadRequestRejectsOversizedInputWithoutNewline() throws Exception {
    read(handler, new ByteArrayInputStream(new byte[MAX_REQUEST_SIZE + 1]));
  }

  @Test
  public void testReadRequestPreservesFramesAndDistinguishesEmptyLineFromEof() throws Exception {
    ByteArrayInputStream input = new ByteArrayInputStream(
        "first\r\n\n  second  \nlast".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals("first", read(handler, input));
    Assert.assertEquals("", read(handler, input));
    Assert.assertEquals("  second  ", read(handler, input));
    Assert.assertEquals("last", read(handler, input));
    Assert.assertNull(read(handler, input));
  }

  @Test
  public void testReadRequestReturnsBytesUnchanged() throws Exception {
    ByteArrayInputStream input = new ByteArrayInputStream(
        new byte[] {0x0b, 'a', (byte) 0xff, (byte) 0xc3, 0, '\r', '\n', ' ', '\n'});

    Assert.assertArrayEquals(new byte[] {0x0b, 'a', (byte) 0xff, (byte) 0xc3, 0},
        handler.readRequest(input));
    Assert.assertArrayEquals(new byte[] {' '}, handler.readRequest(input));
    Assert.assertNull(handler.readRequest(input));
  }

  @Test
  public void testReadRequestCountsUtf8Bytes() throws Exception {
    IpcRequestHandler limited = new IpcRequestHandler(Mockito.mock(AdminJsonRpc.class), 3);
    for (String terminator : new String[] {"\n", "\r\n"}) {
      Assert.assertEquals("中", read(limited, new ByteArrayInputStream(
          ("中" + terminator).getBytes(StandardCharsets.UTF_8))));
    }
    try {
      read(limited, new ByteArrayInputStream("中文\n".getBytes(StandardCharsets.UTF_8)));
      Assert.fail("Expected multi-byte input to exceed the byte limit");
    } catch (RequestTooLargeException expected) {
      // A character count would incorrectly accept both characters.
    }
  }

  @Test
  public void testReadRequestCountsCarriageReturnsOutsideCrlf() throws Exception {
    IpcRequestHandler limited = new IpcRequestHandler(Mockito.mock(AdminJsonRpc.class), 3);
    for (String terminator : new String[] {"\n", "\r\n", ""}) {
      Assert.assertEquals("a\rb", read(limited, new ByteArrayInputStream(
          ("a\rb" + terminator).getBytes(StandardCharsets.UTF_8))));
    }
    Assert.assertEquals("ab\r", read(limited, new ByteArrayInputStream(
        "ab\r".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void testReadRequestRejectsOversizedPayloadsWithCarriageReturns() {
    IpcRequestHandler limited = new IpcRequestHandler(Mockito.mock(AdminJsonRpc.class), 3);
    for (String frame : new String[] {"abcd\n", "abcd\r\n", "abc\rX\n", "abc\r\r\n",
        "abc\r", "abcd"}) {
      Assert.assertThrows(RequestTooLargeException.class, () -> read(limited,
          new ByteArrayInputStream(frame.getBytes(StandardCharsets.UTF_8))));
    }
  }

  @Test
  public void testZeroLimitOnlyAcceptsEmptyLines() throws Exception {
    IpcRequestHandler zeroLimit = new IpcRequestHandler(Mockito.mock(AdminJsonRpc.class), 0);
    Assert.assertEquals(0, zeroLimit.getMaxRequestSize());
    Assert.assertEquals("", read(zeroLimit, new ByteArrayInputStream(new byte[] {'\n'})));
    Assert.assertEquals("", read(zeroLimit,
        new ByteArrayInputStream(new byte[] {'\r', '\n'})));
    Assert.assertNull(read(zeroLimit, new ByteArrayInputStream(new byte[0])));
    for (String frame : new String[] {"a", "\r", "\r\r\n", "a\r\n"}) {
      Assert.assertThrows(RequestTooLargeException.class, () -> read(zeroLimit,
          new ByteArrayInputStream(frame.getBytes(StandardCharsets.UTF_8))));
    }
  }

  private static String handle(IpcRequestHandler handler, String request) throws IOException {
    return handler.handleCommand(request.getBytes(StandardCharsets.UTF_8));
  }

  private static String read(IpcRequestHandler handler, InputStream input) throws IOException {
    byte[] request = handler.readRequest(input);
    return request == null ? null : new String(request, StandardCharsets.UTF_8);
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

    String response = handle(rejectingHandler, body);

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
