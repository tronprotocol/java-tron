package org.tron.core.services.admin.ipc.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
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
import org.tron.core.net.service.peermanagement.PeerOperationResult;
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
  private static final String ENDPOINT = "192.0.2.20:18888";
  private static final String PARAMS = "[\"" + ENDPOINT + "\"]";
  private static final String REQUEST = "{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\","
      + "\"params\":" + PARAMS;

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
    Mockito.when(adminJsonRpc.addPeer(ENDPOINT))
        .thenReturn(new PeerOperationResult(true, true, 0, ""));
    if (http) {
      AdminRpcServlet servlet = new AdminRpcServlet();
      ReflectionTestUtils.setField(servlet, "adminJsonRpc", adminJsonRpc);
      servlet.init(new MockServletConfig());
      sender = body -> {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin");
        request.setServletPath("/admin");
        request.setContentType("application/json");
        request.addHeader("Host", "127.0.0.1:8575");
        request.setContent(body);
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
    for (String id : new String[] {"null", "true", "false", "{}", "[]"}) {
      assertError(sender.send(REQUEST + ",\"id\":" + id + "}"), -32600, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void numericIdsOutsideLongRangeRetainTheirValue() throws Exception {
    for (String id : new String[] {"9223372036854775808", "-9223372036854775809",
        "18446744073709551617", "9223372036854775807.1", "-9223372036854775808.1",
        "1e40", "-1e40"}) {
      JsonNode response = parse(sender.send(REQUEST + ",\"id\":" + id + "}"));
      assertId(id, response.get("id"));
      Assert.assertTrue(response.path("result").path("success").asBoolean());
      Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
      Mockito.clearInvocations(adminJsonRpc);
    }
  }

  @Test
  public void acceptedIdsRetainTheirValueAndType() throws Exception {
    for (String id : new String[] {"\"null\"", "\"\"", "\"请求-7\"", "7", "0",
        "9223372036854775807", "-9223372036854775808", "9007199254740993",
        "0.12345678901234567890123456789", "9223372036854775807.0", "-0.5", "1e-20"}) {
      JsonNode response = parse(sender.send(REQUEST + ",\"id\":" + id + "}"));
      assertId(id, response.get("id"));
      Assert.assertTrue(response.path("result").path("success").asBoolean());
      Assert.assertFalse(response.has("error"));
      Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
      Mockito.clearInvocations(adminJsonRpc);
    }
  }

  @Test
  public void invalidVersionsAreRejectedBeforeInvocation() throws Exception {
    for (String version : new String[] {"\"3.0\"", "2.0", "null", "true", "{}", "[]"}) {
      JsonNode response = assertError(
          sender.send(REQUEST.replace("\"2.0\"", version) + ",\"id\":7}"), -32600, "7");
      Assert.assertEquals("Invalid Request: jsonrpc must be \"2.0\"",
          response.path("error").path("message").asText());
    }
    JsonNode response = assertError(
        sender.send("{\"method\":\"admin_addPeer\",\"params\":" + PARAMS + ",\"id\":7}"),
        -32600, "7");
    Assert.assertEquals("Invalid Request: jsonrpc must be \"2.0\"",
        response.path("error").path("message").asText());
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void invalidMethodsAreRejectedBeforeInvocation() throws Exception {
    for (String method : new String[] {"\"\"", "7", "null", "true", "{}", "[]"}) {
      assertError(sender.send(REQUEST.replace("\"admin_addPeer\"", method) + ",\"id\":7}"),
          -32600, "7");
    }
    assertError(sender.send("{\"jsonrpc\":\"2.0\",\"id\":7}"), -32600, "7");
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void onlyAnnotatedMethodNamesAreDispatched() throws Exception {
    for (String[] method : new String[][] {
        {"addPeer", PARAMS},
        {"removePeer", PARAMS},
        {"disconnectPeer", PARAMS},
        {"listActivePeers", "[]"},
        {"blockIp", "[\"192.0.2.20\"]"},
        {"unblockIp", "[\"192.0.2.20\"]"},
        {"listBlockedIps", "[]"}}) {
      String request = "{\"jsonrpc\":\"2.0\",\"method\":\"" + method[0]
          + "\",\"params\":" + method[1];
      assertError(sender.send(request + ",\"id\":7}"), -32601, "7");
      Assert.assertEquals("", sender.send(request + "}"));
      Mockito.verifyNoInteractions(adminJsonRpc);

      String canonical = request.replace("\"method\":\"", "\"method\":\"admin_");
      JsonNode response = parse(sender.send(canonical + ",\"id\":7}"));
      Assert.assertTrue(response.has("result"));
      Assert.assertFalse(response.has("error"));
      Mockito.clearInvocations(adminJsonRpc);
    }
  }

  @Test
  public void scalarParamsAreRejectedBeforeInvocation() throws Exception {
    for (String params : new String[] {"\"text\"", "7", "true"}) {
      assertError(sender.send(REQUEST.replace(PARAMS, params) + ",\"id\":7}"),
          -32600, "7");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void nullParamsReachTheDispatcher() throws Exception {
    JsonNode response = parse(sender.send(REQUEST.replace(PARAMS, "null") + ",\"id\":7}"));
    Assert.assertNotEquals(-32600, response.path("error").path("code").asInt());
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
        REQUEST.replace(PARAMS, "\"text\"") + "}"}) {
      assertError(sender.send(body), -32600, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void successfulNotificationHasNoResponse() throws Exception {
    Assert.assertEquals("", sender.send(REQUEST + "}"));
    Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
  }

  @Test
  public void unknownMethodNotificationHasNoResponse() throws Exception {
    Assert.assertEquals("", sender.send(REQUEST.replace("admin_addPeer", "admin_missing") + "}"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void invalidMethodParamsNotificationHasNoResponse() throws Exception {
    Assert.assertEquals("", sender.send(REQUEST.replace(PARAMS, "[]") + "}"));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void parameterBindingFailuresRetainIdsAndSilenceNotifications() throws Exception {
    for (String params : new String[] {"[{}]", "{\"endpoint\":{}}"}) {
      String request = REQUEST.replace(PARAMS, params);
      for (String id : new String[] {"9", "\"request\"", "9223372036854775807",
          "0.12345678901234567890123456789"}) {
        JsonNode error = assertError(sender.send(request + ",\"id\":" + id + "}"), -32602, id)
            .get("error");
        Assert.assertEquals("Failed to read method parameter at index 0",
            error.path("message").asText());
        Assert.assertEquals(2, error.size());
      }
      Assert.assertEquals("", sender.send(request + "}"));
      Mockito.verifyNoInteractions(adminJsonRpc);
    }
  }

  @Test
  public void businessErrorNotificationHasNoResponse() throws Exception {
    Mockito.when(adminJsonRpc.addPeer(ENDPOINT))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));
    Assert.assertEquals("", sender.send(REQUEST + "}"));
    Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
  }

  @Test
  public void errorResponsesRetainAcceptedIds() throws Exception {
    Mockito.when(adminJsonRpc.addPeer(ENDPOINT))
        .thenThrow(new JsonRpcInvalidParamsException("Invalid admin parameters"));
    for (String id : new String[] {"\"null\"", "9223372036854775807",
        "0.12345678901234567890123456789"}) {
      JsonNode response = assertError(sender.send(REQUEST + ",\"id\":" + id + "}"), -32602, id);
      Assert.assertEquals("Invalid admin parameters", response.path("error").path("message")
          .asText());
      Assert.assertEquals("{}", response.path("error").path("data").asText());
      Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
      Mockito.clearInvocations(adminJsonRpc);
      assertError(sender.send(REQUEST.replace("admin_addPeer", "admin_missing")
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
    for (String id : new String[] {"9", "\"request\""}) {
      String body = sender.send(REQUEST + ",\"id\":" + id + "}");
      JsonNode response = assertError(body, -32603, id);
      Assert.assertEquals("Internal error", response.path("error").path("message").asText());
      Assert.assertFalse(body.contains("sensitive-detail"));
    }
    Assert.assertEquals("", sender.send(REQUEST + "}"));
    Mockito.verify(interceptor, Mockito.times(3)).preHandleJson(Mockito.any(JsonNode.class));
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void unmappedInvocationFailureIsSanitizedAndSilencesNotifications() throws Exception {
    for (Exception failure : new Exception[] {new IllegalStateException("sensitive-detail"),
        MismatchedInputException.from(null, String.class, "sensitive-detail")}) {
      Mockito.doAnswer(invocation -> {
        throw failure;
      }).when(adminJsonRpc).addPeer(ENDPOINT);
      for (String id : new String[] {"9", "\"request\""}) {
        String body = sender.send(REQUEST + ",\"id\":" + id + "}");
        JsonNode error = assertError(body, -32603, id).get("error");
        Assert.assertEquals("Internal error", error.path("message").asText());
        Assert.assertEquals(2, error.size());
        Assert.assertFalse(body.contains("sensitive-detail"));
        Assert.assertFalse(body.contains(failure.getClass().getName()));
      }
      Assert.assertEquals("", sender.send(REQUEST + "}"));
      Mockito.verify(adminJsonRpc, Mockito.times(3)).addPeer(ENDPOINT);
      Mockito.clearInvocations(adminJsonRpc);
    }
  }

  @Test
  public void serverMappingDefinitionFailureRemainsInternalError() throws Exception {
    JsonRpcInterceptor interceptor = Mockito.mock(JsonRpcInterceptor.class);
    Mockito.doAnswer(invocation -> {
      throw InvalidDefinitionException.from((JsonParser) null, "sensitive-detail",
          MAPPER.constructType(String.class));
    }).when(interceptor).postHandleJson(Mockito.any(JsonNode.class));
    requestHandler.setInterceptorList(Collections.singletonList(interceptor));

    String body = sender.send(REQUEST + ",\"id\":\"request\"}");
    JsonNode error = assertError(body, -32603, "\"request\"").get("error");
    Assert.assertEquals("Internal error", error.path("message").asText());
    Assert.assertEquals(2, error.size());
    Assert.assertFalse(body.contains("sensitive-detail"));
    Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
    Mockito.verify(interceptor, Mockito.atLeastOnce()).postHandleJson(Mockito.any(JsonNode.class));
  }

  @Test
  public void namedAndOmittedParamsRetainDispatchBehavior() throws Exception {
    String named = REQUEST.replace(PARAMS, "{\"endpoint\":\"" + ENDPOINT + "\"}");
    Assert.assertTrue(parse(sender.send(named + ",\"id\":7}"))
        .path("result").path("success").asBoolean());
    Mockito.verify(adminJsonRpc).addPeer(ENDPOINT);
    Mockito.clearInvocations(adminJsonRpc);
    assertError(sender.send("{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\",\"id\":7}"),
        -32602, "7");
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void malformedRequestBytesAreRejectedBeforeInvocation() throws Exception {
    String request = REQUEST + ",\"id\":1}";
    byte[] valid = request.getBytes(StandardCharsets.UTF_8);
    for (byte[] body : new byte[][] {
        withFirstParamBytes(0xff),
        withFirstParamBytes(0xc3),
        withFirstParamBytes(0xc1, 0xa1),
        withFirstParamBytes(0xc1, 0xbf),
        withFirstParamBytes(0xe0, 0x9f, 0xbf),
        withFirstParamBytes(0xed, 0xa0, 0x80),
        withFirstParamBytes(0xed, 0xbf, 0xbf),
        withFirstParamBytes(0xf0, 0x8f, 0xbf, 0xbf),
        withFirstParamBytes(0xf4, 0x90, 0x80, 0x80),
        concat(valid, new byte[] {0}),
        concat(new byte[] {0x0b}, valid),
        new byte[] {0x00, 0x7b, 0x00, 0x00},
        new byte[] {0x00, 0x00, 0x7b, 0x00},
        request.getBytes(StandardCharsets.UTF_16BE),
        request.getBytes(StandardCharsets.UTF_16LE),
        request.getBytes(Charset.forName("UTF-32BE")),
        request.getBytes(Charset.forName("UTF-32LE"))}) {
      assertError(sender.send(body), -32700, "null");
    }
    Mockito.verifyNoInteractions(adminJsonRpc);
  }

  @Test
  public void validUtf8BoundarySequencesReachTheMethod() throws Exception {
    for (String param : new String[] {"a\u0080", "aࠀ", "a퟿", "a",
        "a𐀀", "a􏿿"}) {
      Mockito.when(adminJsonRpc.addPeer(param))
          .thenReturn(new PeerOperationResult(true, true, 0, ""));
      String request = REQUEST.replace(PARAMS, "[\"" + param + "\"]") + ",\"id\":7}";
      Assert.assertTrue(parse(sender.send(request)).path("result").path("success").asBoolean());
      Mockito.verify(adminJsonRpc).addPeer(param);
    }
  }

  private byte[] withFirstParamBytes(int... sequence) throws IOException {
    byte[] bytes = new byte[sequence.length];
    for (int i = 0; i < sequence.length; i++) {
      bytes[i] = (byte) sequence[i];
    }
    return concat("{\"jsonrpc\":\"2.0\",\"method\":\"admin_addPeer\",\"params\":[\"a"
        .getBytes(StandardCharsets.UTF_8), bytes, "\"],\"id\":1}"
        .getBytes(StandardCharsets.UTF_8));
  }

  private byte[] concat(byte[]... parts) throws IOException {
    ByteArrayOutputStream joined = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      joined.write(part);
    }
    return joined.toByteArray();
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
    String send(byte[] body) throws Exception;

    default String send(String body) throws Exception {
      return send(body.getBytes(StandardCharsets.UTF_8));
    }
  }
}
