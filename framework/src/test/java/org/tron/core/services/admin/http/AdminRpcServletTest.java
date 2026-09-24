package org.tron.core.services.admin.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.jsonrpc.JsonRpcMapper;
import org.tron.core.services.jsonrpc.JsonRpcMediaType;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;

public class AdminRpcServletTest {

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
    StringBuilder request = new StringBuilder();
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      request.append('[');
    }
    request.append('0');
    for (int i = 0; i <= Constant.MAX_NESTING_DEPTH; i++) {
      request.append(']');
    }

    MockHttpServletResponse response = doPost(request.toString());
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  public void requestWithTooManyTokensIsRejected() throws Exception {
    StringBuilder request = new StringBuilder("{\"params\":[");
    for (int i = 0; i < Constant.MAX_TOKEN_COUNT; i++) {
      if (i > 0) {
        request.append(',');
      }
      request.append('0');
    }
    request.append("]}");

    MockHttpServletResponse response = doPost(request.toString());
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
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
