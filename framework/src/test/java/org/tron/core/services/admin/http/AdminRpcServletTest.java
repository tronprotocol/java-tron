package org.tron.core.services.admin.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.jsonrpc.JsonRpcMapper;
import org.tron.core.services.jsonrpc.JsonRpcMediaType;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;

public class AdminRpcServletTest {

  private static final String REQUEST_PREFIX =
      "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
          + "\"params\":[\"a\",\"b\"],\"id\":7,\"extra\":";

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
    when(adminJsonRpc.adminExample("left", "right")).thenReturn("left:right");
    String body = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"left\",\"right\"],\"id\":7}";
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
        assertEquals("left:right", result.get("result").asText());
      }
      global.verifyNoInteractions();
      verify(adminJsonRpc, times(2)).adminExample("left", "right");
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
    String second = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"c\",\"d\"],\"id\":8}";

    assertBatchRejected("[" + first + "]");
    assertBatchRejected("[" + first + "," + second + "]");
  }

  @Test
  public void notificationOnlyBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"a\",\"b\"]}";

    assertBatchRejected("[" + notification + "]");
    assertBatchRejected("[" + notification + "," + notification + "]");
  }

  @Test
  public void mixedBatchIsRejectedBeforeInvocation() throws Exception {
    String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"c\",\"d\"]}";
    String request = REQUEST_PREFIX + "0}";

    assertBatchRejected("[" + notification + "," + request + "]");
    assertBatchRejected("[" + request + "," + notification + "]");
  }

  @Test
  public void stringNullRequestIdIsPreserved() throws Exception {
    when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
            + "\"params\":[\"a\",\"b\"],\"id\":\"null\"}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertTrue(result.get("id").isTextual());
    assertEquals("null", result.get("id").asText());
    assertEquals("a:b", result.get("result").asText());
    verify(adminJsonRpc).adminExample("a", "b");
  }

  @Test
  public void notificationInvokesMethodWithoutResponse() throws Exception {
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"params\":[\"a\",\"b\"]}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
    assertEquals(0, response.getContentLength());
    assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
    verify(adminJsonRpc).adminExample("a", "b");
  }

  @Test
  public void utf8ResponseUsesByteContentLength() throws Exception {
    when(adminJsonRpc.adminExample("左", "右")).thenReturn("左:右");

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
            + "\"params\":[\"左\",\"右\"],\"id\":8}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
    assertEquals("UTF-8", response.getCharacterEncoding());
    assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsByteArray());
    assertEquals("左:右", result.get("result").asText());
    assertEquals(8, result.get("id").asInt());
    verify(adminJsonRpc).adminExample("左", "右");
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
    when(adminJsonRpc.adminExample("a", "b"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));

    MockHttpServletResponse response = doPost(REQUEST_PREFIX + "0}");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertEquals(-32602, result.get("error").get("code").asInt());
    assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    assertEquals("Invalid admin parameters", result.get("error").get("message").asText());
    assertEquals(7, result.get("id").asInt());
    assertFalse(result.has("result"));
    verify(adminJsonRpc).adminExample("a", "b");
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
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}", "text/plain");

    assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  public void missingContentTypeIsRejected() throws Exception {
    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}", null);

    assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  public void jsonContentTypesAreAccepted() throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}";

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
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}",
        "application/json", "evil.example:8575");

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  public void listedVirtualHostIsAcceptedCaseInsensitivelyAndWithoutPort() throws Exception {
    setVirtualHosts("admin.example.com");

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}",
        "application/json", "ADMIN.EXAMPLE.COM:8575");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  public void ipLiteralHostsAreAccepted() throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}";

    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/json", "127.0.0.1:8575").getStatus());
    assertEquals(HttpServletResponse.SC_OK,
        doPost(body, "application/json", "[::1]:8575").getStatus());
  }

  @Test
  public void wildcardVirtualHostAcceptsAnyHostname() throws Exception {
    setVirtualHosts("*");

    MockHttpServletResponse response = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":1}",
        "application/json", "any.example:8575");

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  private void setField(String name, Object value) throws Exception {
    Field field = AdminRpcServlet.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(servlet, value);
  }

  private void assertValidRequestWithExtra() throws Exception {
    when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
    MockHttpServletResponse response = doPost(REQUEST_PREFIX + "[0]}");
    JsonNode result = JsonRpcMapper.create().readTree(response.getContentAsString());
    assertEquals("a:b", result.get("result").asText());
    assertEquals(7, result.get("id").asInt());
    verify(adminJsonRpc).adminExample("a", "b");
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
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin");
    request.setServletPath("/admin");
    request.setRemoteAddr("127.0.0.1");
    request.setContentType(contentType);
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    if (host != null) {
      request.addHeader("Host", host);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    servlet.service(request, response);
    return response;
  }

  private void setVirtualHosts(String... hosts) throws Exception {
    setField("virtualHostValidator", new VirtualHostValidator(Arrays.asList(hosts)));
  }

}
