package org.tron.core.services.admin.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.service.peermanagement.BlockedIpInfo;
import org.tron.core.net.service.peermanagement.PeerOperationResult;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.jsonrpc.JsonRpcMapper;
import org.tron.core.services.jsonrpc.JsonRpcMediaType;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;

public class AdminRpcServletTest {

  private static final int REQUEST_LIMIT = 1024;
  private static final ObjectMapper MAPPER = JsonRpcMapper.create();
  private static final String REQUEST_PREFIX =
      "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
          + "\"params\":[\"a\"],\"id\":7,\"extra\":";

  private AdminRpcServlet servlet;
  private AdminJsonRpc adminJsonRpc;

  @Before
  public void setUp() throws Exception {
    servlet = new AdminRpcServlet();
    adminJsonRpc = mock(AdminJsonRpc.class);
    setField("adminJsonRpc", adminJsonRpc);
    setField("interceptor", mock(JsonRpcInterceptor.class));
    servlet.init(new MockServletConfig());
    setVirtualHosts("localhost");
  }

  @Test
  public void publicRateLimitDoesNotBlockAdminRequests() throws Exception {
    when(adminJsonRpc.addPeer("left"))
        .thenReturn(new PeerOperationResult(false, false, 0, "left:right"));
    String body = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"left\"],\"id\":7}";
    CommonParameter parameter = CommonParameter.getInstance();
    boolean previousNonBlocking = parameter.isRateLimiterApiNonBlocking();
    try (MockedStatic<GlobalRateLimiter> global = mockStatic(GlobalRateLimiter.class)) {
      global.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(false);
      for (boolean nonBlocking : new boolean[]{false, true}) {
        parameter.setRateLimiterApiNonBlocking(nonBlocking);

        MockHttpServletResponse response = doPost(body, "application/json", "localhost:8575");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
        assertEquals("UTF-8", response.getCharacterEncoding());
        JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
        assertEquals("2.0", result.get("jsonrpc").asText());
        assertEquals(7, result.get("id").asInt());
        assertEquals("left:right", result.get("result").get("errorMessage").asText());
      }
      global.verifyNoInteractions();
      verify(adminJsonRpc, times(2)).addPeer("left");
    } finally {
      parameter.setRateLimiterApiNonBlocking(previousNonBlocking);
    }
  }

  @Test
  public void excessivelyNestedRequestIsRejected() throws Exception {
    assertValidRequestWithExtra();
    StringBuilder request = new StringBuilder();
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      request.append('[');
    }
    request.append('0');
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      request.append(']');
    }

    assertParseError(doPost(REQUEST_PREFIX + request + "}"));
  }

  @Test
  public void requestWithTooManyTokensIsRejected() throws Exception {
    assertValidRequestWithExtra();
    StringBuilder request = new StringBuilder(REQUEST_PREFIX + "[");
    for (int i = 0; i < Constant.MAX_TOKEN_COUNT; i++) {
      if (i > 0) {
        request.append(',');
      }
      request.append('0');
    }
    request.append("]}");

    assertParseError(doPost(request.toString()));
  }

  @Test
  public void invalidJsonReturnsParseErrorBeforeInvocation() throws Exception {
    String valid = REQUEST_PREFIX + "0}";
    for (String body : new String[] {"", " \t\r\n", "{broken-sensitive-detail", REQUEST_PREFIX,
        valid + " {broken", valid + " " + valid, "[" + valid,
        "[" + valid + ",{broken]", "[" + valid + "] " + valid}) {
      assertParseError(doPost(body));
    }
  }

  @Test
  public void emptyBatchIsRejectedBeforeInvocation() throws Exception {
    assertBatchRejected("[]");
  }

  @Test
  public void batchRequestsAreRejectedBeforeInvocation() throws Exception {
    String first = REQUEST_PREFIX + "0}";
    String second = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"c\"],\"id\":8}";

    assertBatchRejected("[" + first + "]");
    assertBatchRejected("[" + first + "," + second + "]");
  }

  @Test
  public void notificationOnlyBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"a\"]}";

    assertBatchRejected("[" + notification + "]");
    assertBatchRejected("[" + notification + "," + notification + "]");
  }

  @Test
  public void mixedBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
        + "\"params\":[\"c\"]}";
    String request = REQUEST_PREFIX + "0}";

    assertBatchRejected("[" + notification + "," + request + "]");
    assertBatchRejected("[" + request + "," + notification + "]");
  }

  @Test
  public void stringNullRequestIdIsPreserved() throws Exception {
    when(adminJsonRpc.addPeer("a"))
        .thenReturn(new PeerOperationResult(false, false, 0, "a:b"));
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"a\"],\"id\":\"null\"}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertTrue(result.get("id").isTextual());
    assertEquals("null", result.get("id").asText());
    assertEquals("a:b", result.get("result").get("errorMessage").asText());
    verify(adminJsonRpc).addPeer("a");
  }

  @Test
  public void notificationInvokesMethodWithoutResponse() throws Exception {
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\",\"params\":[\"a\"]}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
    assertEquals(0, response.getContentLength());
    assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
    verify(adminJsonRpc).addPeer("a");
  }

  @Test
  public void utf8ResponseUsesByteContentLength() throws Exception {
    when(adminJsonRpc.addPeer("左"))
        .thenReturn(new PeerOperationResult(false, false, 0, "左:右"));

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"左\"],\"id\":8}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
    assertEquals("UTF-8", response.getCharacterEncoding());
    assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsByteArray());
    assertEquals("左:右", result.get("result").get("errorMessage").asText());
    assertEquals(8, result.get("id").asInt());
    verify(adminJsonRpc).addPeer("左");
  }

  @Test
  public void streamDispatchRetainsConfiguredInterceptor() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    boolean previousMetricsEnabled = parameter.isMetricsPrometheusEnable();
    JsonRpcInterceptor interceptor = mock(JsonRpcInterceptor.class);
    try {
      parameter.setMetricsPrometheusEnable(true);
      setField("interceptor", interceptor);
      servlet.init(new MockServletConfig());
      assertValidRequestWithExtra();

      verify(interceptor).preHandleJson(any(JsonNode.class));
    } finally {
      parameter.setMetricsPrometheusEnable(previousMetricsEnabled);
    }
  }

  @Test
  public void annotatedBusinessErrorIsPreserved() throws Exception {
    when(adminJsonRpc.addPeer("a"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));

    MockHttpServletResponse response = doPost(REQUEST_PREFIX + "0}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertEquals(-32602, result.get("error").get("code").asInt());
    assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    assertEquals("Invalid admin parameters", result.get("error").get("message").asText());
    assertEquals(7, result.get("id").asInt());
    assertFalse(result.has("result"));
    verify(adminJsonRpc).addPeer("a");
  }

  @Test
  public void transportReadFailureIsNotReportedAsParseError() throws Exception {
    ServletInputStream input = mock(ServletInputStream.class);
    IOException failure = new IOException("transport read failed");
    when(input.read(any(byte[].class), anyInt(), anyInt())).thenThrow(failure);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin") {
      @Override
      public ServletInputStream getInputStream() {
        return input;
      }
    };
    request.setContentType("application/json");
    MockHttpServletResponse response = new MockHttpServletResponse();
    try {
      servlet.service(request, response);
      fail("Expected the transport failure to propagate");
    } catch (IOException e) {
      assertSame(failure, e);
    }
    assertEquals(0, response.getContentAsByteArray().length);
    verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void nonJsonContentTypeIsRejected() throws Exception {
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}",
        "text/plain");

    assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  public void missingContentTypeIsRejected() throws Exception {
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}", null);

    assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  public void jsonContentTypesAreAccepted() throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}";

    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/json; charset=UTF-8").getStatus());
    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/json-rpc").getStatus());
    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/vnd.tron+json").getStatus());
  }

  @Test
  public void unlistedVirtualHostIsRejected() throws Exception {
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}",
        "application/json", "evil.example:8575");

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  public void listedVirtualHostIsAcceptedCaseInsensitivelyAndWithoutPort() throws Exception {
    setVirtualHosts("admin.example.com");

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}",
        "application/json", "ADMIN.EXAMPLE.COM:8575");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  public void ipLiteralHostsAreAccepted() throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}";

    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/json", "127.0.0.1:8575").getStatus());
    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/json", "[::1]:8575").getStatus());
  }

  @Test
  public void wildcardVirtualHostAcceptsAnyHostname() throws Exception {
    setVirtualHosts("*");

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":1}",
        "application/json", "any.example:8575");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  public void peerManagementMethodIsAvailableOverHttp() throws Exception {
    Mockito.when(adminJsonRpc.addPeer("192.0.2.20:18888"))
        .thenReturn(new PeerOperationResult(true, true, 0, ""));

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"192.0.2.20:18888\"],\"id\":8}");
    JsonNode result = new ObjectMapper().readTree(response.getContentAsByteArray()).get("result");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(true, result.get("success").asBoolean());
    assertEquals(true, result.get("changed").asBoolean());
    assertEquals("", result.get("errorMessage").asText());
    assertFalse(result.has("message"));
    Mockito.verify(adminJsonRpc).addPeer("192.0.2.20:18888");
  }

  @Test
  public void peerOperationFailureUsesErrorMessageOverHttp() throws Exception {
    String errorMessage = "Peer modification is unavailable; update node.active instead.";
    Mockito.when(adminJsonRpc.addPeer("192.0.2.20:18888"))
        .thenReturn(new PeerOperationResult(false, false, 0, errorMessage));

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
            + "\"params\":[\"192.0.2.20:18888\"],\"id\":10}");
    JsonNode responseNode = new ObjectMapper().readTree(response.getContentAsByteArray());
    JsonNode result = responseNode.get("result");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(10, responseNode.get("id").asInt());
    assertEquals(false, result.get("success").asBoolean());
    assertEquals(false, result.get("changed").asBoolean());
    assertEquals(0, result.get("disconnectedCount").asInt());
    assertEquals(errorMessage, result.get("errorMessage").asText());
    assertFalse(result.has("message"));
    assertFalse(responseNode.has("error"));
    Mockito.verify(adminJsonRpc).addPeer("192.0.2.20:18888");
  }

  @Test
  public void fatalDispatchCommitsEmpty500AndRethrowsOriginal() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFatalDispatch(new StackOverflowError("fatal-dispatch-marker"), response);

    assertEmptyInternalServerError(response);
  }

  @Test
  public void fatalReadCommitsEmpty500BeforeDispatch() throws Exception {
    ServletInputStream input = mock(ServletInputStream.class);
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-read-marker");
    when(input.read(any(byte[].class), anyInt(), anyInt())).thenThrow(fatal);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin") {
      @Override
      public ServletInputStream getInputStream() {
        return input;
      }
    };
    request.setContentType("application/json");
    MockHttpServletResponse response = new MockHttpServletResponse();

    Error thrown = assertThrows(Error.class, () -> servlet.service(request, response));

    assertSame(fatal, thrown);
    assertEmptyInternalServerError(response);
    verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void cleanupIOExceptionDoesNotReplaceFatalError() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    doThrow(new IOException("cleanup-io-marker")).when(response).flushBuffer();

    assertFatalDispatch(new StackOverflowError("original-fatal-marker"), response);

    verify(response).flushBuffer();
  }

  @Test
  public void cleanupErrorDoesNotReplaceFatalError() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    doThrow(new OutOfMemoryError("cleanup-error-marker")).when(response).flushBuffer();

    assertFatalDispatch(new StackOverflowError("original-fatal-marker"), response);

    verify(response).flushBuffer();
  }

  @Test
  public void fatalErrorDiscardsUncommittedOutput() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    byte[] partial = "partial-response-marker".getBytes(StandardCharsets.UTF_8);
    response.setContentLength(partial.length);
    response.getOutputStream().write(partial);
    assertFalse(response.isCommitted());

    assertFatalDispatch(new StackOverflowError("fatal-marker"), response);

    assertEmptyInternalServerError(response);
  }

  @Test
  public void fatalErrorLeavesCommittedResponseUntouched() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(true);

    assertFatalDispatch(new StackOverflowError("fatal-marker"), response);

    verify(response, never()).resetBuffer();
    verify(response, never()).setStatus(anyInt());
    verify(response, never()).setContentLength(anyInt());
    verify(response, never()).flushBuffer();
  }

  @Test(timeout = 30_000)
  public void fatalErrorWithProductionFilterReturnsEmpty500() throws Exception {
    TestAdminRpcHttpService service = new TestAdminRpcHttpService();
    service.setMaxRequestSize(REQUEST_LIMIT);
    ReflectionTestUtils.setField(service, "adminRpcServlet", servlet);
    try {
      service.start().get(5, TimeUnit.SECONDS);
      for (String accept : new String[] {"application/json", "text/html", "text/plain"}) {
        clearInvocations(adminJsonRpc);
        when(adminJsonRpc.addPeer("a"))
            .thenThrow(new StackOverflowError("fatal-response-marker"))
            .thenReturn(new PeerOperationResult(true, true, 0, ""));

        String response = service.post(REQUEST_PREFIX + "0}", false, accept);

        assertTrue(response, response.startsWith("HTTP/1.1 500 "));
        assertTrue(response, response.contains("\r\nContent-Length: 0\r\n"));
        int headerEnd = response.indexOf("\r\n\r\n");
        assertTrue("Response must include complete headers", headerEnd >= 0);
        assertEquals(accept, "", response.substring(headerEnd + 4));
        verify(adminJsonRpc).addPeer("a");

        // A later successful request must still return its JSON-RPC result.
        String success = service.post(REQUEST_PREFIX + "0}", false, accept);
        assertTrue(success, success.startsWith("HTTP/1.1 200 "));
        int successHeaderEnd = success.indexOf("\r\n\r\n");
        assertTrue("Response must include complete headers", successHeaderEnd >= 0);
        JsonNode result = MAPPER.readTree(success.substring(successHeaderEnd + 4));
        assertTrue(result.path("result").path("success").asBoolean());
        assertEquals(7, result.path("id").asInt());
        verify(adminJsonRpc, times(2)).addPeer("a");
        verifyNoMoreInteractions(adminJsonRpc);
      }
    } finally {
      service.stop().get(5, TimeUnit.SECONDS);
    }
  }

  @Test(timeout = 10_000)
  public void testContentLengthRequestsUseJsonRpcLimit() throws Exception {
    assertRequestLimit(false, REQUEST_LIMIT - 1, REQUEST_LIMIT, REQUEST_LIMIT + 1);
  }

  @Test(timeout = 10_000)
  public void testChunkedRequestsUseJsonRpcLimit() throws Exception {
    // Jetty 9's streaming interceptor can count the same content more than once.
    // Check a small valid body and an oversized body; exact boundaries are covered above.
    assertRequestLimit(true, REQUEST_LIMIT / 4, REQUEST_LIMIT + 1);
  }

  private void assertRequestLimit(boolean chunked, int... sizes) throws Exception {
    CommonParameter parameter = Args.getInstance();
    long originalJsonRpcLimit = parameter.getJsonRpcMaxMessageSize();
    long originalHttpLimit = parameter.getHttpMaxMessageSize();
    TestAdminRpcHttpService service = null;
    try {
      parameter.setJsonRpcMaxMessageSize(REQUEST_LIMIT);
      parameter.setHttpMaxMessageSize(REQUEST_LIMIT * 2);
      when(adminJsonRpc.addPeer("a")).thenReturn(new PeerOperationResult(true, true, 0, ""));
      service = new TestAdminRpcHttpService();
      ReflectionTestUtils.setField(service, "adminRpcServlet", servlet);
      service.start().get(5, TimeUnit.SECONDS);

      for (int size : sizes) {
        clearInvocations(adminJsonRpc);
        String response = service.post(requestBody(size), chunked);
        if (size <= REQUEST_LIMIT) {
          assertTrue(response, response.startsWith("HTTP/1.1 200 "));
          int headerEnd = response.indexOf("\r\n\r\n");
          assertTrue("Response must include complete headers", headerEnd >= 0);
          JsonNode result = MAPPER.readTree(response.substring(headerEnd + 4));
          assertEquals(1, result.path("id").asInt());
          assertTrue(result.path("result").path("success").asBoolean());
          verify(adminJsonRpc).addPeer("a");
          verifyNoMoreInteractions(adminJsonRpc);
        } else {
          assertTrue(response, response.startsWith("HTTP/1.1 413 "));
          verifyNoInteractions(adminJsonRpc);
        }
      }
    } finally {
      try {
        if (service != null) {
          service.stop().get(5, TimeUnit.SECONDS);
        }
      } finally {
        parameter.setJsonRpcMaxMessageSize(originalJsonRpcLimit);
        parameter.setHttpMaxMessageSize(originalHttpLimit);
      }
    }
  }

  private String requestBody(int size) throws Exception {
    ObjectNode request = MAPPER.createObjectNode();
    request.put("jsonrpc", "2.0");
    request.put("method", "admin_addPeer");
    request.putArray("params").add("a");
    request.put("id", 1);
    String json = MAPPER.writeValueAsString(request);
    // The ASCII body is padded with valid JSON whitespace to exercise exact byte boundaries.
    return json + Strings.repeat(" ", size - json.length());
  }

  private void assertFatalDispatch(Error fatal, HttpServletResponse response) throws Exception {
    when(adminJsonRpc.addPeer("a")).thenThrow(fatal);

    Error thrown = assertThrows(Error.class,
        () -> doPost(REQUEST_PREFIX + "0}", "application/json", "localhost", response));

    assertSame(fatal, thrown);
    verify(adminJsonRpc).addPeer("a");
  }

  private void assertEmptyInternalServerError(MockHttpServletResponse response) {
    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals(0, response.getContentLength());
    assertEquals(0, response.getContentAsByteArray().length);
    assertTrue(response.isCommitted());
  }

  private void setField(String name, Object value) throws Exception {
    Field field = AdminRpcServlet.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(servlet, value);
  }

  @Test
  public void blockedIpQueryIncludesCreationTimeOverHttp() throws Exception {
    Mockito.when(adminJsonRpc.listBlockedIps()).thenReturn(Arrays.asList(
        new BlockedIpInfo("192.0.2.20", 1_790_000_000_000L),
        new BlockedIpInfo("2001:db8:0:0:0:0:0:20", 1_790_000_001_000L)));

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_listBlockedIps\",\"id\":9}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode result = mapper.readTree(response.getContentAsByteArray());

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(9, result.get("id").asInt());
    assertEquals(mapper.readTree("[{\"ip\":\"192.0.2.20\",\"blockedAtMillis\":1790000000000},"
        + "{\"ip\":\"2001:db8:0:0:0:0:0:20\",\"blockedAtMillis\":1790000001000}]"),
        result.get("result"));
    Mockito.verify(adminJsonRpc).listBlockedIps();
  }

  private void assertValidRequestWithExtra() throws Exception {
    when(adminJsonRpc.addPeer("a"))
        .thenReturn(new PeerOperationResult(false, false, 0, "a:b"));
    MockHttpServletResponse response = doPost(REQUEST_PREFIX + "[0]}");
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertEquals("a:b", result.get("result").get("errorMessage").asText());
    assertEquals(7, result.get("id").asInt());
    verify(adminJsonRpc).addPeer("a");
    clearInvocations(adminJsonRpc);
  }

  private void assertParseError(MockHttpServletResponse response) throws Exception {
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
    assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    JsonNode error = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertEquals("2.0", error.get("jsonrpc").asText());
    assertTrue(error.get("id").isNull());
    assertEquals(-32700, error.get("error").get("code").asInt());
    assertEquals("JSON parse error", error.get("error").get("message").asText());
    assertEquals(2, error.get("error").size());
    assertFalse(error.has("result"));
    verifyNoInteractions(adminJsonRpc);
  }

  private void assertBatchRejected(String body) throws Exception {
    MockHttpServletResponse response = doPost(body);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
    assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    JsonNode error = JsonRpcMapper.create().readTree(response.getContentAsByteArray());
    assertTrue(error.isObject());
    assertEquals("2.0", error.get("jsonrpc").asText());
    assertTrue(error.get("id").isNull());
    assertEquals(-32600, error.get("error").get("code").asInt());
    assertEquals("Batch requests are not supported", error.get("error").get("message").asText());
    assertEquals(2, error.get("error").size());
    assertFalse(error.has("result"));
    verifyNoInteractions(adminJsonRpc);
  }

  private MockHttpServletResponse doPost(String body) throws Exception {
    return doPost(body, "application/json");
  }

  private MockHttpServletResponse doPost(String body, String contentType) throws Exception {
    return doPost(body, contentType, null);
  }

  private MockHttpServletResponse doPost(String body, String contentType, String host)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    doPost(body, contentType, host, response);
    return response;
  }

  private void doPost(String body, String contentType, String host, HttpServletResponse response)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin");
    request.setServletPath("/admin");
    request.setRemoteAddr("127.0.0.1");
    request.setContentType(contentType);
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    if (host != null) {
      request.addHeader("Host", host);
    }
    servlet.service(request, response);
  }

  private void setVirtualHosts(String... hosts) throws Exception {
    setField("virtualHostValidator", new VirtualHostValidator(Arrays.asList(hosts)));
  }

  private static class TestAdminRpcHttpService extends AdminRpcHttpService {

    private LocalConnector connector;

    @Override
    protected void initServer() {
      super.initServer();
      // Keep the production handler, servlet and filters, without binding a network port.
      connector = new LocalConnector(apiServer);
      apiServer.setConnectors(new Connector[] {connector});
    }

    String post(String body, boolean chunked) throws Exception {
      return post(body, chunked, null);
    }

    String post(String body, boolean chunked, String accept) throws Exception {
      String framing = chunked ? "Transfer-Encoding: chunked\r\n"
          : "Content-Length: " + body.length() + "\r\n";
      String content = chunked ? Integer.toHexString(body.length()) + "\r\n"
          + body + "\r\n0\r\n\r\n" : body;
      String request = "POST /admin HTTP/1.1\r\n"
          + "Host: 127.0.0.1\r\n"
          + "Content-Type: application/json\r\n"
          + (accept == null ? "" : "Accept: " + accept + "\r\n")
          + "Connection: close\r\n" + framing + "\r\n" + content;
      return connector.getResponse(request, 5, TimeUnit.SECONDS);
    }
  }



}
