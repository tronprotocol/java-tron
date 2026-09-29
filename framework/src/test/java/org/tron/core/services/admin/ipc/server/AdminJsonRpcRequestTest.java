package org.tron.core.services.admin.ipc.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.AdminJsonRpcRequestHandler;
import org.tron.core.services.admin.http.AdminRpcServlet;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.core.services.jsonrpc.JsonRpcMediaType;

/** Runs identical protocol assertions through the HTTP and IPC entry points. */
@RunWith(Parameterized.class)
public class AdminJsonRpcRequestTest {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
  private static final String REQUEST = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
      + "\"params\":[\"a\",\"b\"]";

  private final boolean http;
  private AdminJsonRpc adminJsonRpc;
  private RequestSender sender;
  private AdminJsonRpcRequestHandler requestHandler;

  public AdminJsonRpcRequestTest(String transport) {
    http = "HTTP".equals(transport);
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> transports() {
    return Arrays.asList(new Object[][] {{"HTTP"}, {"IPC"}});
  }

  @Before
  public void setUp() throws Exception {
    adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
    Mockito.when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
    if (http) {
      AdminRpcServlet servlet = new AdminRpcServlet();
      ReflectionTestUtils.setField(servlet, "adminJsonRpc", adminJsonRpc);
      servlet.init(new MockServletConfig());
      sender = body -> {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin");
        request.setServletPath("/admin");
        request.setContentType("application/json");
        request.addHeader("Host", "127.0.0.1:8575");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new HttpInterceptor().doFilter(request, response, servlet::service);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(JsonRpcMediaType.isSupported(response.getContentType()));
        Assert.assertEquals("UTF-8", response.getCharacterEncoding());
        Assert.assertEquals(response.getContentAsByteArray().length, response.getContentLength());
        return response.getContentAsString();
      };
      requestHandler = (AdminJsonRpcRequestHandler)
          ReflectionTestUtils.getField(servlet, "requestHandler");
    } else {
      IpcRequestHandler handler = new IpcRequestHandler(adminJsonRpc, 4194304);
      sender = handler::handleCommand;
      requestHandler = (AdminJsonRpcRequestHandler)
          ReflectionTestUtils.getField(handler, "requestHandler");
    }
  }

  @Test
  public void invalidIdTypesAreRejectedBeforeInvocation() throws Exception {
    for (String id : new String[] {"true", "false", "{}", "[]"}) {
      assertError(sender.send(REQUEST + ",\"id\":" + id + "}"), -32600, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void outOfRangeNumericIdsAreRejectedBeforeInvocation() throws Exception {
    for (String id : new String[] {"9223372036854775808", "-9223372036854775809",
        "18446744073709551617", "9223372036854775807.1", "-9223372036854775808.1",
        "1e40", "-1e40"}) {
      assertError(sender.send(REQUEST + ",\"id\":" + id + "}"), -32600, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void acceptedIdsRetainTheirValueAndType() throws Exception {
    for (String id : new String[] {"null", "\"null\"", "\"\"", "\"请求-7\"", "7", "0",
        "9223372036854775807", "-9223372036854775808", "9007199254740993",
        "0.12345678901234567890123456789", "9223372036854775807.0", "-0.5", "1e-20"}) {
      JsonNode response = parse(sender.send(REQUEST + ",\"id\":" + id + "}"));
      assertId(id, response.get("id"));
      Assert.assertEquals("a:b", response.path("result").asText());
      Assert.assertFalse(response.has("error"));
      Mockito.verify(adminJsonRpc).adminExample("a", "b");
      Mockito.clearInvocations(adminJsonRpc);
    }
  }

  @Test
  public void invalidVersionsAreRejectedBeforeInvocation() throws Exception {
    for (String version : new String[] {"\"3.0\"", "2.0", "null", "true", "{}", "[]"}) {
      assertError(sender.send(REQUEST.replace("\"2.0\"", version) + ",\"id\":7}"),
          -32600, "7");
    }
    assertError(sender.send("{\"method\":\"admin_example\",\"params\":[\"a\",\"b\"],\"id\":7}"),
        -32600, "7");
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void invalidMethodsAreRejectedBeforeInvocation() throws Exception {
    for (String method : new String[] {"7", "null", "true", "{}", "[]"}) {
      assertError(sender.send(REQUEST.replace("\"admin_example\"", method) + ",\"id\":7}"),
          -32600, "7");
    }
    assertError(sender.send("{\"jsonrpc\":\"2.0\",\"id\":7}"), -32600, "7");
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void scalarParamsAreRejectedBeforeInvocation() throws Exception {
    for (String params : new String[] {"\"text\"", "null", "7", "true"}) {
      assertError(sender.send(REQUEST.replace("[\"a\",\"b\"]", params) + ",\"id\":7}"),
          -32600, "7");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void nonObjectInputsReturnJsonNullId() throws Exception {
    for (String body : new String[] {"null", "true", "7", "\"text\""}) {
      assertError(sender.send(body), -32600, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void invalidRequestsWithoutIdAreNotTreatedAsNotifications() throws Exception {
    for (String body : new String[] {"{}", REQUEST.replace("2.0", "3.0") + "}",
        REQUEST.replace("[\"a\",\"b\"]", "\"text\"") + "}"}) {
      assertError(sender.send(body), -32600, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void successfulNotificationHasNoResponse() throws Exception {
    Assert.assertEquals("", sender.send(REQUEST + "}"));
    Mockito.verify(adminJsonRpc).adminExample("a", "b");
  }

  @Test
  public void unknownMethodNotificationHasNoResponse() throws Exception {
    Assert.assertEquals("", sender.send(REQUEST.replace("admin_example", "admin_missing") + "}"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void invalidMethodParamsNotificationHasNoResponse() throws Exception {
    Assert.assertEquals("", sender.send(REQUEST.replace("[\"a\",\"b\"]", "[]") + "}"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void businessErrorNotificationHasNoResponse() throws Exception {
    Mockito.when(adminJsonRpc.adminExample("a", "b"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));
    Assert.assertEquals("", sender.send(REQUEST + "}"));
    Mockito.verify(adminJsonRpc).adminExample("a", "b");
  }

  @Test
  public void errorResponsesRetainAcceptedIds() throws Exception {
    Mockito.when(adminJsonRpc.adminExample("a", "b"))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));
    for (String id : new String[] {"null", "\"null\"", "9223372036854775807",
        "0.12345678901234567890123456789"}) {
      JsonNode response = assertError(sender.send(REQUEST + ",\"id\":" + id + "}"), -32602, id);
      Assert.assertEquals("Invalid admin parameters", response.path("error").path("message")
          .asText());
      Mockito.verify(adminJsonRpc).adminExample("a", "b");
      Mockito.clearInvocations(adminJsonRpc);
      assertError(sender.send(REQUEST.replace("admin_example", "admin_missing")
          + ",\"id\":" + id + "}"), -32601, id);
      Mockito.verifyNoInteractions(adminJsonRpc);
    }
  }

  @Test
  public void dispatcherFailureReturnsSanitizedErrorAndSilencesNotifications() throws Exception {
    JsonRpcInterceptor interceptor = Mockito.mock(JsonRpcInterceptor.class);
    Mockito.doThrow(new IllegalStateException("sensitive-detail"))
        .when(interceptor).preHandleJson(Mockito.any(JsonNode.class));
    requestHandler.setInterceptorList(Collections.singletonList(interceptor));
    for (String id : new String[] {"9", "null", "\"request\""}) {
      String body = sender.send(REQUEST + ",\"id\":" + id + "}");
      JsonNode response = assertError(body, -32603, id);
      Assert.assertEquals("Internal error", response.path("error").path("message").asText());
      Assert.assertFalse(body.contains("sensitive-detail"));
    }
    Assert.assertEquals("", sender.send(REQUEST + "}"));
    Mockito.verify(interceptor, Mockito.times(4)).preHandleJson(Mockito.any(JsonNode.class));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void namedAndOmittedParamsRetainDispatchBehavior() throws Exception {
    String named = REQUEST.replace("[\"a\",\"b\"]", "{\"param1\":\"a\",\"param2\":\"b\"}");
    Assert.assertEquals("a:b", parse(sender.send(named + ",\"id\":7}")).path("result").asText());
    Mockito.verify(adminJsonRpc).adminExample("a", "b");
    Mockito.clearInvocations(adminJsonRpc);
    assertError(sender.send("{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\",\"id\":7}"),
        -32602, "7");
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  private JsonNode assertError(String body, int code, String id) throws Exception {
    JsonNode response = parse(body);
    Assert.assertEquals(code, response.path("error").path("code").asInt());
    assertId(id, response.get("id"));
    Assert.assertFalse(response.has("result"));
    return response;
  }

  private JsonNode parse(String body) throws Exception {
    Assert.assertFalse("Expected a JSON-RPC response", body.isEmpty());
    Assert.assertFalse(body.contains("\n"));
    Assert.assertFalse(body.contains("\r"));
    JsonNode response = MAPPER.readTree(body);
    Assert.assertTrue(response.isObject());
    Assert.assertEquals("2.0", response.path("jsonrpc").asText());
    return response;
  }

  private void assertId(String id, JsonNode actual) throws Exception {
    JsonNode expected = MAPPER.readTree(id);
    Assert.assertNotNull(actual);
    if (expected.isNumber()) {
      Assert.assertTrue(actual.isNumber());
      Assert.assertEquals(0, expected.decimalValue().compareTo(actual.decimalValue()));
    } else {
      Assert.assertEquals(expected, actual);
    }
  }

  @FunctionalInterface
  private interface RequestSender {
    String send(String body) throws Exception;
  }
}
