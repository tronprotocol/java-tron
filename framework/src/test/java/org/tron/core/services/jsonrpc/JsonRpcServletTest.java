package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.services.filter.CharResponseWrapper;

public class JsonRpcServletTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private TestableServlet servlet;
  private JsonRpcServer mockRpcServer;
  private int savedMaxBatchSize;
  private int savedMaxResponseSize;

  @Before
  public void setUp() throws Exception {
    servlet = new TestableServlet();
    mockRpcServer = mock(JsonRpcServer.class);
    servlet.setRpcServer(mockRpcServer);
    savedMaxBatchSize = CommonParameter.getInstance().jsonRpcMaxBatchSize;
    savedMaxResponseSize = CommonParameter.getInstance().jsonRpcMaxResponseSize;
  }

  @After
  public void tearDown() {
    CommonParameter.getInstance().jsonRpcMaxBatchSize = savedMaxBatchSize;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = savedMaxResponseSize;
  }

  // --- parse error paths ---

  @Test
  public void invalidJson_returnsParseError() throws Exception {
    MockHttpServletResponse resp = doPost("not {{ valid json");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertFalse(body.isArray());
    assertEquals(-32700, body.get("error").get("code").asInt());
    // A non-constraint JsonProcessingException keeps the generic message (else branch).
    assertEquals("JSON parse error", body.get("error").get("message").asText());
    assertEquals("2.0", body.get("jsonrpc").asText());
    assertTrue(body.get("id").isNull());
  }

  @Test
  public void emptyBody_returnsParseError() throws Exception {
    MockHttpServletResponse resp = doPost("");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertEquals(-32700, body.get("error").get("code").asInt());
  }

  // --- batch size limit ---

  @Test
  public void batchExceedsLimit_returnsExceedLimitAsArray() throws Exception {
    CommonParameter.getInstance().jsonRpcMaxBatchSize = 2;
    MockHttpServletResponse resp = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("batch error response must be a JSON array", body.isArray());
    assertEquals(1, body.size());
    assertEquals(-32005, body.get(0).get("error").get("code").asInt());
  }

  @Test
  public void batchWithinLimit_proceedsToRpcServer() throws Exception {
    CommonParameter.getInstance().jsonRpcMaxBatchSize = 5;
    byte[] singleResp = "{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":1}"
        .getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(singleResp);
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("[{\"id\":1},{\"id\":2}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue("batch response must be a JSON array", body.isArray());
    assertEquals("each sub-request must produce a response", 2, body.size());
    assertEquals("ok", body.get(0).get("result").asText());
  }

  @Test
  public void emptyBatch_returnsInvalidRequest() throws Exception {
    MockHttpServletResponse resp = doPost("[]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertFalse("empty-batch error response must be a single object, not an array", body.isArray());
    assertEquals(-32600, body.get("error").get("code").asInt());
    assertEquals("2.0", body.get("jsonrpc").asText());
    assertTrue(body.get("id").isNull());
  }

  @Test
  public void invalidRequestIdTypes_returnInvalidRequestWithoutDispatch() throws Exception {
    String[] invalidIds = {"true", "{}", "[]"};

    for (String id : invalidIds) {
      MockHttpServletResponse resp = doPost(
          "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\","
              + "\"params\":[],\"id\":" + id + "}");
      assertEquals(200, resp.getStatus());
      assertEquals("application/json-rpc", resp.getContentType());
      assertInvalidRequestWithNullId(MAPPER.readTree(resp.getContentAsByteArray()));
    }

    verifyNoInteractions(mockRpcServer);
  }

  @Test
  public void nullRequestId_isNotRejectedByServletValidation() throws Exception {
    int[] callCount = {0};
    doAnswer(inv -> {
      callCount[0]++;
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    doPost("{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\","
        + "\"params\":[],\"id\":null}");

    assertEquals("a null id is valid JSON-RPC input and must reach dispatch", 1, callCount[0]);
  }

  @Test
  public void structuredAndAbsentParams_reachRpcServer() throws Exception {
    List<JsonNode> dispatchedRequests = new ArrayList<>();
    doAnswer(inv -> {
      dispatchedRequests.add(MAPPER.readTree((InputStream) inv.getArgument(0)));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    String[] requests = {
        "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"id\":1}",
        "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\","
            + "\"params\":null,\"id\":2}",
        "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\","
            + "\"params\":[],\"id\":3}",
        "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\","
            + "\"params\":{},\"id\":4}"
    };
    for (String request : requests) {
      doPost(request);
    }

    assertEquals("all supported params shapes must reach jsonrpc4j",
        requests.length, dispatchedRequests.size());
    for (int i = 0; i < requests.length; i++) {
      assertEquals("forwarded request must be unchanged at index " + i,
          MAPPER.readTree(requests[i]), dispatchedRequests.get(i));
    }
  }

  @Test
  public void singleScalarParams_isRejectedBeforeDispatch() throws Exception {
    MockHttpServletResponse resp = doPost(
        "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\","
            + "\"params\":5,\"id\":42}");

    assertEquals(200, resp.getStatus());
    assertEquals("application/json-rpc", resp.getContentType());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertEquals(MAPPER.getNodeFactory().textNode("2.0"), body.get("jsonrpc"));
    assertEquals(MAPPER.getNodeFactory().numberNode(-32600),
        body.get("error").get("code"));
    assertEquals(MAPPER.getNodeFactory().textNode("Invalid Request"),
        body.get("error").get("message"));
    assertFalse(body.get("error").has("data"));
    assertEquals(MAPPER.getNodeFactory().numberNode(42), body.get("id"));
    verifyNoInteractions(mockRpcServer);
  }

  @Test
  public void batchInvalidRequestId_isIsolatedFromValidSiblings() throws Exception {
    int[] callCount = {0};
    doAnswer(inv -> {
      JsonNode request = MAPPER.readTree((InputStream) inv.getArgument(0));
      OutputStream out = inv.getArgument(1);
      out.write(("{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":"
          + request.get("id") + "}").getBytes(StandardCharsets.UTF_8));
      callCount[0]++;
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("["
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":[],\"id\":1},"
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":[],\"id\":true},"
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":[],\"id\":\"two\"}"
        + "]");

    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue(body.isArray());
    assertEquals(3, body.size());
    assertEquals("ok", body.get(0).get("result").asText());
    assertEquals(1, body.get(0).get("id").asInt());
    assertInvalidRequestWithNullId(body.get(1));
    assertEquals("ok", body.get(2).get("result").asText());
    assertEquals("two", body.get(2).get("id").asText());
    assertEquals("only valid requests should reach jsonrpc4j", 2, callCount[0]);
  }

  @Test
  public void batchScalarParams_isIsolatedFromValidSibling() throws Exception {
    int[] callCount = {0};
    JsonNode[] dispatchedRequest = {null};
    doAnswer(inv -> {
      dispatchedRequest[0] = MAPPER.readTree((InputStream) inv.getArgument(0));
      OutputStream out = inv.getArgument(1);
      out.write(("{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":"
          + dispatchedRequest[0].get("id") + "}").getBytes(StandardCharsets.UTF_8));
      callCount[0]++;
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("["
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":5,\"id\":1},"
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":[],\"id\":2}"
        + "]");

    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue(body.isArray());
    assertEquals(2, body.size());
    assertEquals(MAPPER.getNodeFactory().numberNode(-32600),
        body.get(0).get("error").get("code"));
    assertEquals(MAPPER.getNodeFactory().textNode("Invalid Request"),
        body.get(0).get("error").get("message"));
    assertFalse(body.get(0).get("error").has("data"));
    assertEquals(MAPPER.getNodeFactory().numberNode(1), body.get(0).get("id"));
    assertEquals(MAPPER.getNodeFactory().textNode("ok"), body.get(1).get("result"));
    assertEquals(MAPPER.getNodeFactory().numberNode(2), body.get(1).get("id"));
    assertEquals("only the valid sibling should reach jsonrpc4j", 1, callCount[0]);
    assertEquals(MAPPER.getNodeFactory().numberNode(2), dispatchedRequest[0].get("id"));
    assertTrue(dispatchedRequest[0].get("params").isArray());
  }

  @Test
  public void batchScalarParamsWithoutId_returnsErrorAndDispatchesValidNotification()
      throws Exception {
    List<JsonNode> dispatchedRequests = new ArrayList<>();
    doAnswer(inv -> {
      dispatchedRequests.add(MAPPER.readTree((InputStream) inv.getArgument(0)));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("["
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":5},"
        + "{\"jsonrpc\":\"2.0\",\"method\":\"web3_clientVersion\",\"params\":[]}"
        + "]");

    assertEquals(200, resp.getStatus());
    assertEquals("application/json-rpc", resp.getContentType());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue(body.isArray());
    assertEquals(1, body.size());
    assertInvalidRequestWithNullId(body.get(0));
    assertEquals("only the valid notification should reach jsonrpc4j",
        1, dispatchedRequests.size());
    assertFalse(dispatchedRequests.get(0).has("id"));
    assertTrue(dispatchedRequests.get(0).get("params").isArray());
  }

  @Test
  public void batchLimitDisabled_largeBatchAllowed() throws Exception {
    CommonParameter.getInstance().jsonRpcMaxBatchSize = 0;
    // write nothing — simulates notifications (no response expected)
    doAnswer(inv -> 0).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < 500; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append("{}");
    }
    sb.append("]");
    MockHttpServletResponse resp = doPost(sb.toString());
    assertEquals(200, resp.getStatus());
    assertEquals("all-notification batch must return empty body per JSON-RPC 2.0 §6",
        0, resp.getContentLength());
    assertEquals("", resp.getContentAsString());
  }

  // --- rpcServer.handleRequest exceptions ---

  @Test
  public void rpcServerThrowsRuntimeException_returnsInternalError() throws Exception {
    doThrow(new RuntimeException("server exploded")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));
    MockHttpServletResponse resp = doPost("{\"method\":\"eth_blockNumber\",\"id\":42}");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertFalse(body.isArray());
    assertEquals(-32603, body.get("error").get("code").asInt());
    assertEquals("Internal error", body.get("error").get("message").asText());
    assertEquals(42, body.get("id").asInt());
  }

  @Test
  public void rpcServerThrowsIOException_returnsInternalError() throws Exception {
    doThrow(new IOException("server exploded")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("{\"method\":\"eth_blockNumber\",\"id\":42}");

    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertEquals(-32603, body.get("error").get("code").asInt());
    assertEquals("Internal error", body.get("error").get("message").asText());
    assertEquals(42, body.get("id").asInt());
  }

  @Test
  public void notificationIOException_returnsEmptyResponse() throws Exception {
    doThrow(new IOException("server exploded")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("{\"method\":\"eth_blockNumber\"}");

    assertEquals(200, resp.getStatus());
    assertEquals("application/json-rpc", resp.getContentType());
    assertEquals(0, resp.getContentAsByteArray().length);
  }

  @Test
  public void singleAssertionError_discardsPartialOutputAndReturnsInternalError() throws Exception {
    doAnswer(inv -> {
      OutputStream output = inv.getArgument(1);
      output.write("{\"result\":\"partial-sensitive-marker".getBytes(StandardCharsets.UTF_8));
      throw new AssertionError("assertion-sensitive-marker");
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    assertSingleInternalError(doPost("{\"id\":42}"), "42");
    assertSingleInternalError(doPost("{\"id\":\"request-1\"}"), "\"request-1\"");
  }

  @Test
  public void singleAssertionErrorWithoutId_keepsEmptyResponse() throws Exception {
    doThrow(new AssertionError("assertion-sensitive-marker")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse response = doPost("{\"method\":\"eth_blockNumber\"}");

    assertEquals(200, response.getStatus());
    assertEquals("application/json-rpc", response.getContentType());
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  public void singleAssertionErrorWithNullId_keepsErrorResponse() throws Exception {
    doThrow(new AssertionError("assertion-sensitive-marker")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    assertSingleInternalError(doPost("{\"method\":\"eth_blockNumber\",\"id\":null}"), "null");
  }

  @Test
  public void singleWrappedAssertionError_propagatesFatalCauseWithoutLogging() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    try (BatchLogCapture logs = new BatchLogCapture()) {
      assertWrappedFatalPropagates(new AssertionError("wrapper-marker", fatal), fatal, false);
      assertTrue(logs.executionEvents().isEmpty());
    }
  }

  @Test
  public void batchWrappedAssertionError_stopsBeforeLaterDispatchWithoutLogging() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    try (BatchLogCapture logs = new BatchLogCapture()) {
      assertWrappedFatalPropagates(new AssertionError("wrapper-marker", fatal), fatal, true);
      assertTrue(logs.executionEvents().isEmpty());
    }
  }

  @Test
  public void batchRpcServerThrows_internalErrorIsArray() throws Exception {
    doThrow(new RuntimeException("boom")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));
    MockHttpServletResponse resp = doPost(
        "[{\"method\":\"eth_blockNumber\",\"id\":\"request-1\"}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("batch internal error must be an array", body.isArray());
    assertEquals(-32603, body.get(0).get("error").get("code").asInt());
    assertEquals("Internal error", body.get(0).get("error").get("message").asText());
    assertEquals("request-1", body.get(0).get("id").asText());
  }

  @Test
  public void batchRpcServerThrowsIOException_internalErrorIsArray() throws Exception {
    doThrow(new IOException("boom")).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost(
        "[{\"method\":\"eth_blockNumber\",\"id\":\"request-1\"}]");

    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue(body.isArray());
    assertEquals(-32603, body.get(0).get("error").get("code").asInt());
    assertEquals("Internal error", body.get(0).get("error").get("message").asText());
    assertEquals("request-1", body.get(0).get("id").asText());
  }

  @Test
  public void fatalError_commitsBare500AndRethrowsOriginal() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    doThrow(fatal).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));
    MockHttpServletResponse response = new MockHttpServletResponse();

    StackOverflowError thrown = assertThrows(StackOverflowError.class,
        () -> doPost("{\"method\":\"eth_blockNumber\",\"id\":1}", response));

    assertSame(fatal, thrown);
    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
    assertTrue(response.isCommitted());
  }

  @Test
  public void singleWrappedRuntimeException_propagatesFatalCause() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    assertWrappedFatalPropagates(new RuntimeException("wrapper", fatal), fatal, false);
  }

  @Test
  public void singleWrappedIOException_propagatesFatalCause() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    assertWrappedFatalPropagates(new IOException("wrapper", fatal), fatal, false);
  }

  @Test
  public void batchWrappedRuntimeException_stopsBeforeLaterDispatch() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    assertWrappedFatalPropagates(new RuntimeException("wrapper", fatal), fatal, true);
  }

  @Test
  public void batchWrappedIOException_stopsBeforeLaterDispatch() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    assertWrappedFatalPropagates(new IOException("wrapper", fatal), fatal, true);
  }

  @Test
  public void cleanupIOException_doesNotReplaceOriginalFatalError() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    doThrow(new IOException("cleanup-io-marker")).when(response).flushBuffer();
    assertCleanupFailureDoesNotReplaceFatal(response);
    verify(response).flushBuffer();
  }

  @Test
  public void cleanupError_doesNotReplaceOriginalFatalError() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    doThrow(new OutOfMemoryError("cleanup-error-marker")).when(response).flushBuffer();
    assertCleanupFailureDoesNotReplaceFatal(response);
    verify(response).flushBuffer();
  }

  @Test
  public void fatalError_unwrapsNestedResponsesBeforeCommitting() throws Exception {
    MockHttpServletResponse actual = new MockHttpServletResponse();
    actual.getOutputStream().write("partial-response-marker".getBytes(StandardCharsets.UTF_8));
    HttpServletResponse wrapped = new HttpServletResponseWrapper(
        new CharResponseWrapper(new CharResponseWrapper(actual)));

    assertCleanupFailureDoesNotReplaceFatal(wrapped);

    assertEquals(500, actual.getStatus());
    assertEquals(0, actual.getContentAsByteArray().length);
    assertTrue(actual.isCommitted());
  }

  @Test
  public void fatalError_atWrapperDepthLimitStillCommits() throws Exception {
    MockHttpServletResponse actual = new MockHttpServletResponse();
    HttpServletResponse wrapped = actual;
    for (int i = 0; i < 16; i++) {
      wrapped = new CharResponseWrapper(wrapped);
    }

    assertCleanupFailureDoesNotReplaceFatal(wrapped);

    assertEquals(500, actual.getStatus());
    assertEquals(0, actual.getContentAsByteArray().length);
    assertTrue(actual.isCommitted());
  }

  @Test(timeout = 5000)
  public void fatalError_selfReferencingWrapperAbandonsCleanup() throws Exception {
    TrackingResponseWrapper wrapper = new TrackingResponseWrapper(new MockHttpServletResponse());
    wrapper.setResponse(wrapper);

    assertCleanupFailureDoesNotReplaceFatal(wrapper);

    assertEquals("cleanup must not delegate into a self-reference", 0, wrapper.cleanupCalls);
  }

  @Test(timeout = 5000)
  public void fatalError_cyclicWrappersAbandonCleanup() throws Exception {
    TrackingResponseWrapper first = new TrackingResponseWrapper(new MockHttpServletResponse());
    TrackingResponseWrapper second = new TrackingResponseWrapper(first);
    first.setResponse(second);

    assertCleanupFailureDoesNotReplaceFatal(first);

    assertEquals("cleanup must not delegate into a cycle", 0, first.cleanupCalls);
    assertEquals("cleanup must not delegate into a cycle", 0, second.cleanupCalls);
  }

  @Test(timeout = 5000)
  public void fatalError_excessiveWrapperDepthAbandonsCleanup() throws Exception {
    MockHttpServletResponse actual = new MockHttpServletResponse();
    List<TrackingResponseWrapper> wrappers = new ArrayList<>();
    HttpServletResponse wrapped = actual;
    for (int i = 0; i < 17; i++) {
      TrackingResponseWrapper wrapper = new TrackingResponseWrapper(wrapped);
      wrappers.add(wrapper);
      wrapped = wrapper;
    }

    assertCleanupFailureDoesNotReplaceFatal(wrapped);

    for (TrackingResponseWrapper wrapper : wrappers) {
      assertEquals("cleanup must not delegate beyond the depth limit", 0, wrapper.cleanupCalls);
    }
    assertFalse(actual.isCommitted());
  }

  @Test
  public void unwrappingFailure_doesNotReplaceOriginalFatalError() throws Exception {
    boolean[] attempted = {false};
    HttpServletResponseWrapper response = new HttpServletResponseWrapper(
        new MockHttpServletResponse()) {
      @Override
      public ServletResponse getResponse() {
        attempted[0] = true;
        throw new IllegalStateException("unwrap-failure-marker");
      }
    };

    assertCleanupFailureDoesNotReplaceFatal(response);

    assertTrue("cleanup must attempt to unwrap the response", attempted[0]);
  }

  @Test
  public void fatalError_doesNotResetCommittedUnderlyingResponse() throws Exception {
    MockHttpServletResponse actual = new MockHttpServletResponse();
    byte[] body = "already-committed".getBytes(StandardCharsets.UTF_8);
    actual.getOutputStream().write(body);
    actual.flushBuffer();

    assertCleanupFailureDoesNotReplaceFatal(new CharResponseWrapper(actual));

    assertEquals(200, actual.getStatus());
    assertArrayEquals(body, actual.getContentAsByteArray());
    assertTrue(actual.isCommitted());
  }

  @Test
  public void batchMalformedRpcServerResponse_preservesRequestId() throws Exception {
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write("not-json".getBytes(StandardCharsets.UTF_8));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost(
        "[{\"method\":\"eth_blockNumber\",\"id\":42}]");

    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue(body.isArray());
    assertEquals(-32603, body.get(0).get("error").get("code").asInt());
    assertEquals("Internal error", body.get(0).get("error").get("message").asText());
    assertEquals(42, body.get(0).get("id").asInt());
  }

  @Test
  public void batchRuntimeException_preservesSiblingsAndContinues() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(new RuntimeException("failure-marker"));

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredBatch(response, "2");
    assertEquals(MAPPER.readTree("[{\"id\":1},{\"id\":2},{\"id\":3}]"),
        MAPPER.valueToTree(dispatched));
  }

  @Test
  public void batchIOException_preservesSiblingsAndContinues() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(new IOException("failure-marker"));

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredBatch(response, "2");
    assertEquals(MAPPER.readTree("[{\"id\":1},{\"id\":2},{\"id\":3}]"),
        MAPPER.valueToTree(dispatched));
  }

  @Test
  public void batchAssertionError_discardsPartialOutputAndPreservesSiblings() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(
        new AssertionError("assertion-sensitive-marker"), true);
    String request = "[{\"id\":1},{\"id\":2},{\"id\":3}]";

    MockHttpServletResponse response = doPost(request);

    assertFalse(response.getContentAsString().contains("partial-sensitive-marker"));
    assertFalse(response.getContentAsString().contains("assertion-sensitive-marker"));
    assertRecoveredBatch(response, "2");
    assertEquals(recoveredBatch("2"), MAPPER.reader()
        .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .readTree(response.getContentAsByteArray()));
    assertEquals(MAPPER.readTree(request), MAPPER.valueToTree(dispatched));
  }

  @Test
  public void batchAssertionErrorWithoutId_keepsErrorNodeAndContinues() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(
        new AssertionError("assertion-sensitive-marker"));
    String request = "[{\"id\":1},{\"method\":\"eth_blockNumber\"},{\"id\":3}]";

    MockHttpServletResponse response = doPost(request);

    assertRecoveredBatch(response, "null");
    assertEquals(MAPPER.readTree(request), MAPPER.valueToTree(dispatched));
  }

  @Test
  public void batchAssertionErrors_logOnlyFirstStackPerBatch() throws Exception {
    List<Integer> dispatched = stubBatchWithTwoFailures(
        new AssertionError("first-sensitive-marker"),
        new AssertionError("repeat-sensitive-marker"));

    try (BatchLogCapture logs = new BatchLogCapture()) {
      for (int i = 0; i < 2; i++) {
        assertBatchWithTwoRecoveredFailures(
            doPost("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4}]"));
      }

      assertEquals(Arrays.asList(1, 2, 3, 4, 1, 2, 3, 4), dispatched);
      List<ILoggingEvent> events = logs.events();
      assertEquals(4, events.size());
      assertBoundedBatchLogs(events.subList(0, 2), AssertionError.class, AssertionError.class);
      assertBoundedBatchLogs(events.subList(2, 4), AssertionError.class, AssertionError.class);
    }
  }

  @Test
  public void batchMultipleFailures_logsOnlyFirstStackAndPreservesResponses() throws Exception {
    List<Integer> dispatched = stubBatchWithTwoFailures(
        new IOException("first-sensitive-marker"),
        new IllegalStateException("repeat-sensitive-marker"));

    try (BatchLogCapture logs = new BatchLogCapture()) {
      MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4}]");

      assertBatchWithTwoRecoveredFailures(response);
      assertEquals(Arrays.asList(1, 2, 3, 4), dispatched);
      assertBoundedBatchLogs(logs.events());
    }
  }

  @Test
  public void consecutiveBatches_eachLogTheirFirstFailure() throws Exception {
    List<Integer> dispatched = stubBatchWithTwoFailures(
        new IOException("first-sensitive-marker"),
        new IllegalStateException("repeat-sensitive-marker"));

    try (BatchLogCapture logs = new BatchLogCapture()) {
      for (int i = 0; i < 2; i++) {
        assertBatchWithTwoRecoveredFailures(
            doPost("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4}]"));
      }

      assertEquals(Arrays.asList(1, 2, 3, 4, 1, 2, 3, 4), dispatched);
      List<ILoggingEvent> events = logs.events();
      assertEquals(4, events.size());
      assertBoundedBatchLogs(events.subList(0, 2));
      assertBoundedBatchLogs(events.subList(2, 4));
    }
  }

  @Test
  public void batchFatalAfterOrdinaryFailure_doesNotLogFatalOrContinue() throws Exception {
    StackOverflowError fatal = new StackOverflowError("fatal-sensitive-marker");
    List<Integer> dispatched = stubBatchWithTwoFailures(
        new IOException("first-sensitive-marker"), new IOException("wrapped-fatal", fatal));
    MockHttpServletResponse response = new MockHttpServletResponse();

    try (BatchLogCapture logs = new BatchLogCapture()) {
      assertSame(fatal, assertThrows(StackOverflowError.class,
          () -> doPost("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4}]", response)));

      assertEquals(Arrays.asList(1, 2, 3), dispatched);
      assertEquals(500, response.getStatus());
      assertEquals(0, response.getContentAsByteArray().length);
      assertTrue(response.isCommitted());
      List<ILoggingEvent> events = logs.events();
      assertEquals(1, events.size());
      assertFirstBatchFailureLog(events.get(0));
    }
  }

  @Test
  public void batchMalformedResponse_preservesSiblingsAndContinues() throws Exception {
    List<Integer> dispatched = stubBatchWithMalformedMiddleResponse();

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredBatch(response, "2");
    assertEquals(Arrays.asList(1, 2, 3), dispatched);
  }

  @Test
  public void batchSerializationFailure_preservesSiblingsAndContinues() throws Exception {
    int[] serializationAttempts = {0};
    ObjectNode failingRequest = new ObjectNode(MAPPER.getNodeFactory()) {
      @Override
      public void serialize(JsonGenerator generator, SerializerProvider provider)
          throws IOException {
        serializationAttempts[0]++;
        throw new JsonProcessingException("serialization-marker") {};
      }
    };
    failingRequest.put("id", 2);
    ArrayNode requests = MAPPER.createArrayNode();
    requests.addObject().put("id", 1);
    requests.add(failingRequest);
    requests.addObject().put("id", 3);
    List<Integer> dispatched = stubBatchWithMalformedMiddleResponse();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Method handleBatch = JsonRpcServlet.class.getDeclaredMethod("handleBatch",
        HttpServletResponse.class, JsonNode.class, int.class);
    handleBatch.setAccessible(true);

    handleBatch.invoke(servlet, response, requests, 0);

    assertRecoveredBatch(response, "2");
    assertEquals(Arrays.asList(1, 3), dispatched);
    assertEquals(1, serializationAttempts[0]);
  }

  @Test
  public void batchFailureWithoutId_keepsErrorNodeAndContinues() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(new IOException("failure-marker"));
    String request = "[{\"id\":1},{\"method\":\"eth_blockNumber\"},{\"id\":3}]";

    MockHttpServletResponse response = doPost(request);

    assertRecoveredBatch(response, "null");
    assertEquals(MAPPER.readTree(request), MAPPER.valueToTree(dispatched));
  }

  @Test
  public void batchRecoveredError_atExactLimitDoesNotOverflow() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(new IOException("failure-marker"));
    int limit = MAPPER.writeValueAsBytes(recoveredBatch("2")).length;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredBatch(response, "2");
    assertEquals(limit, response.getContentAsByteArray().length);
    assertEquals(3, dispatched.size());
  }

  @Test
  public void batchMalformedResponse_countsOnlyReplacementBytes() throws Exception {
    List<Integer> dispatched = stubBatchWithMalformedMiddleResponse();
    int limit = MAPPER.writeValueAsBytes(recoveredBatch("2")).length;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredBatch(response, "2");
    assertEquals(limit, response.getContentAsByteArray().length);
    assertEquals(Arrays.asList(1, 2, 3), dispatched);
  }

  @Test
  public void batchRecoveredError_consumesBudgetForLaterResponse() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(new IOException("failure-marker"));
    CommonParameter.getInstance().jsonRpcMaxResponseSize =
        MAPPER.writeValueAsBytes(recoveredBatch("2")).length - 1;

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredErrorConsumesBudget(response);
    assertEquals(MAPPER.readTree("[{\"id\":1},{\"id\":2},{\"id\":3}]"),
        MAPPER.valueToTree(dispatched));
  }

  @Test
  public void batchMalformedResponse_replacementConsumesBudgetForLaterResponse() throws Exception {
    List<Integer> dispatched = stubBatchWithMalformedMiddleResponse();
    CommonParameter.getInstance().jsonRpcMaxResponseSize =
        MAPPER.writeValueAsBytes(recoveredBatch("2")).length - 1;

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertRecoveredErrorConsumesBudget(response);
    assertEquals(Arrays.asList(1, 2, 3), dispatched);
  }

  @Test
  public void batchRecoveredError_overflowStopsLaterDispatch() throws Exception {
    List<JsonNode> dispatched = stubBatchWithMiddleFailure(new IOException("failure-marker"));
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limitBeforeSecondErrorFits();

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertBatchErrorOverflow(response);
    assertEquals(2, dispatched.size());
  }

  @Test
  public void batchMalformedResponse_replacementCanTriggerOverflow() throws Exception {
    List<Integer> dispatched = stubBatchWithMalformedMiddleResponse();
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limitBeforeSecondErrorFits();

    MockHttpServletResponse response = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");

    assertBatchErrorOverflow(response);
    assertEquals(Arrays.asList(1, 2), dispatched);
  }

  // --- response size limit ---

  @Test
  public void responseTooLarge_returnsSingleErrorObject() throws Exception {
    int limit = 50;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(new byte[limit + 1]);
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("{\"method\":\"eth_getLogs\",\"id\":1}");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertFalse(body.isArray());
    assertEquals(-32003, body.get("error").get("code").asInt());
  }

  @Test
  public void batchResponseTooLarge_returnsErrorArray() throws Exception {
    int limit = 50;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(new byte[limit + 1]);
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("[{\"method\":\"eth_getLogs\"}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("batch response-too-large must be an array", body.isArray());
    assertEquals(-32003, body.get(0).get("error").get("code").asInt());
  }

  @Test
  public void batchShortCircuitsOnOverflow() throws Exception {
    int limit = 50;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;
    int[] callCount = {0};
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      callCount[0]++;
      if (callCount[0] == 1) {
        out.write("{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
      } else {
        out.write(new byte[limit]); // triggers overflow when added to accumulated size
      }
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("[{\"id\":1},{\"id\":2},{\"id\":3}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("overflow response must be an array", body.isArray());
    // Geth-compatible: previous successes are preserved; overflow item and remaining
    // unexecuted items each get a -32003 error with their original id.
    assertEquals(3, body.size());
    assertEquals("ok", body.get(0).get("result").asText());
    assertEquals(-32003, body.get(1).get("error").get("code").asInt());
    assertEquals(2, body.get(1).get("id").asInt());
    assertEquals(-32003, body.get(2).get("error").get("code").asInt());
    assertEquals(3, body.get(2).get("id").asInt());
    assertEquals("third sub-request must not be executed after overflow", 2, callCount[0]);
  }

  @Test
  public void batchInvalidRequestId_afterOverflowStillReturnsInvalidRequest() throws Exception {
    int limit = 50;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;
    int[] callCount = {0};
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(new byte[limit + 1]);
      callCount[0]++;
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("["
        + "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getLogs\",\"id\":1},"
        + "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getLogs\",\"id\":{}}"
        + "]");

    JsonNode body = MAPPER.readTree(resp.getContentAsByteArray());
    assertTrue(body.isArray());
    assertEquals(2, body.size());
    assertEquals(-32003, body.get(0).get("error").get("code").asInt());
    assertEquals(1, body.get(0).get("id").asInt());
    assertInvalidRequestWithNullId(body.get(1));
    assertEquals("invalid requests must not be dispatched after overflow", 1, callCount[0]);
  }

  // --- normal path ---

  @Test
  public void normalRequest_commitsRpcServerResponse() throws Exception {
    byte[] rpcResp = "{\"result\":\"0x1\"}".getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(rpcResp);
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("{\"method\":\"eth_blockNumber\",\"id\":1}");
    assertEquals(200, resp.getStatus());
    assertArrayEquals(rpcResp, resp.getContentAsByteArray());
  }

  // --- Content-Type header: must be application/json-rpc (no charset suffix) ---

  @Test
  public void errorResponse_contentTypeIsApplicationJsonRpc() throws Exception {
    MockHttpServletResponse resp = doPost("not valid json");
    assertEquals("application/json-rpc", resp.getContentType());
  }

  @Test
  public void batchResponse_contentTypeIsApplicationJsonRpc() throws Exception {
    byte[] singleResp = "{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":1}"
        .getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(singleResp);
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("[{\"id\":1}]");
    assertEquals("application/json-rpc", resp.getContentType());
  }

  @Test
  public void allNotificationBatch_contentTypeIsApplicationJsonRpc() throws Exception {
    // notification: rpcServer returns 0 bytes → empty batchResult → early return path
    doAnswer(inv -> 0).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("[{\"method\":\"eth_blockNumber\"}]");
    assertEquals(200, resp.getStatus());
    assertEquals(0, resp.getContentLength());
    assertEquals("application/json-rpc", resp.getContentType());
  }

  // --- Primitive root node → Invalid Request (-32600), id must be JSON null ---

  @Test
  public void primitiveRootNull_returnsInvalidRequestWithJsonNullId() throws Exception {
    MockHttpServletResponse resp = doPost("null");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertFalse(body.isArray());
    assertEquals("2.0", body.get("jsonrpc").asText());
    assertEquals(-32600, body.get("error").get("code").asInt());
    assertTrue("id must be JSON null, not the string \"null\"", body.get("id").isNull());
    assertFalse("id must not be a string", body.get("id").isTextual());
  }

  @Test
  public void primitiveRootBoolean_returnsInvalidRequest() throws Exception {
    MockHttpServletResponse resp = doPost("true");
    assertEquals(200, resp.getStatus());
    assertEquals(-32600,
        MAPPER.readTree(resp.getContentAsString()).get("error").get("code").asInt());
  }

  @Test
  public void primitiveRootNumber_returnsInvalidRequest() throws Exception {
    MockHttpServletResponse resp = doPost("123");
    assertEquals(200, resp.getStatus());
    assertEquals(-32600,
        MAPPER.readTree(resp.getContentAsString()).get("error").get("code").asInt());
  }

  @Test
  public void primitiveRootString_returnsInvalidRequest() throws Exception {
    MockHttpServletResponse resp = doPost("\"hello\"");
    assertEquals(200, resp.getStatus());
    assertEquals(-32600,
        MAPPER.readTree(resp.getContentAsString()).get("error").get("code").asInt());
  }

  // --- Non-object element inside a batch → Invalid Request per element ---

  @Test
  public void batchWithNestedArray_returnsInvalidRequestArray() throws Exception {
    MockHttpServletResponse resp = doPost("[[]]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("response must be a JSON array", body.isArray());
    assertEquals(1, body.size());
    assertEquals(-32600, body.get(0).get("error").get("code").asInt());
    assertTrue("id in batch error must be JSON null", body.get(0).get("id").isNull());
  }

  @Test
  public void batchWithMixedObjectAndArray_objectProcessedArrayRejected() throws Exception {
    byte[] singleResp = "{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":1}"
        .getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      OutputStream out = inv.getArgument(1);
      out.write(singleResp);
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));

    MockHttpServletResponse resp = doPost("[{\"id\":1}, []]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("response must be a JSON array", body.isArray());
    assertEquals(2, body.size());
    assertEquals("ok", body.get(0).get("result").asText());
    assertEquals(-32600, body.get(1).get("error").get("code").asInt());
  }

  @Test
  public void batchWithNumericAndStringElements_allGetInvalidRequest() throws Exception {
    MockHttpServletResponse resp = doPost("[42, \"foo\", true]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("response must be a JSON array", body.isArray());
    assertEquals(3, body.size());
    for (int i = 0; i < 3; i++) {
      assertEquals(-32600, body.get(i).get("error").get("code").asInt());
    }
  }

  // --- StreamReadConstraints: maxNestingDepth and maxTokenCount must be enforced ---

  @Test
  public void excessivelyNestedRequest_returnsParseError() throws Exception {
    int limit = Constant.MAX_NESTING_DEPTH;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i <= limit; i++) {
      sb.append('[');
    }
    sb.append('0');
    for (int i = 0; i <= limit; i++) {
      sb.append(']');
    }

    MockHttpServletResponse resp = doPost(sb.toString());
    assertEquals(200, resp.getStatus());
    JsonNode error = MAPPER.readTree(resp.getContentAsString()).get("error");
    assertEquals(-32700, error.get("code").asInt());
    // StreamConstraintsException message must be surfaced verbatim, not the generic text,
    // so callers can tell which constraint (nesting depth) was hit.
    String message = error.get("message").asText();
    assertNotEquals("JSON parse error", message);
    assertTrue("expected a nesting-depth constraint message, got: " + message,
        message.contains("nesting depth") && message.contains("exceeds the maximum allowed"));
  }

  @Test
  public void tooManyTokens_returnsParseError() throws Exception {
    int limit = Constant.MAX_TOKEN_COUNT;
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < limit; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append('0');
    }
    sb.append(']');

    MockHttpServletResponse resp = doPost(sb.toString());
    assertEquals(200, resp.getStatus());
    JsonNode error = MAPPER.readTree(resp.getContentAsString()).get("error");
    assertEquals(-32700, error.get("code").asInt());
    // StreamConstraintsException message must be surfaced verbatim, not the generic text,
    // so callers can tell which constraint (token count) was hit.
    String message = error.get("message").asText();
    assertNotEquals("JSON parse error", message);
    assertTrue("expected a token-count constraint message, got: " + message,
        message.contains("Token count") && message.contains("exceeds the maximum allowed"));
  }

  // --- helpers ---

  private List<Integer> stubBatchWithTwoFailures(Throwable first, Throwable second)
      throws Exception {
    List<Integer> dispatched = new ArrayList<>();
    doAnswer(inv -> {
      int id = MAPPER.readTree((InputStream) inv.getArgument(0)).get("id").asInt();
      dispatched.add(id);
      if (id == 2) {
        throw first;
      }
      if (id == 3) {
        throw second;
      }
      OutputStream out = inv.getArgument(1);
      out.write(successResponse(id));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));
    return dispatched;
  }

  private static void assertBatchWithTwoRecoveredFailures(MockHttpServletResponse response)
      throws IOException {
    assertEquals(200, response.getStatus());
    assertEquals("application/json-rpc", response.getContentType());
    assertEquals(MAPPER.readTree("[{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":1},"
        + "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"},"
        + "\"id\":2},{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,"
        + "\"message\":\"Internal error\"},\"id\":3},"
        + "{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":4}]"),
        MAPPER.readTree(response.getContentAsByteArray()));
  }

  private static void assertBoundedBatchLogs(List<ILoggingEvent> events) {
    assertBoundedBatchLogs(events, IOException.class, IllegalStateException.class);
  }

  private static void assertBoundedBatchLogs(List<ILoggingEvent> events,
      Class<? extends Throwable> firstType, Class<? extends Throwable> repeatedType) {
    assertEquals(2, events.size());
    assertFirstBatchFailureLog(events.get(0), firstType);
    ILoggingEvent repeated = events.get(1);
    assertEquals(Level.DEBUG, repeated.getLevel());
    assertNull(repeated.getThrowableProxy());
    assertArrayEquals(new Object[]{2, repeatedType.getName()},
        repeated.getArgumentArray());
    assertFalse(repeated.getFormattedMessage().contains("repeat-sensitive-marker"));
  }

  private static void assertFirstBatchFailureLog(ILoggingEvent event) {
    assertFirstBatchFailureLog(event, IOException.class);
  }

  private static void assertFirstBatchFailureLog(ILoggingEvent event,
      Class<? extends Throwable> failureType) {
    assertEquals(Level.ERROR, event.getLevel());
    assertEquals("RPC execution failed for batch sub-request 1", event.getFormattedMessage());
    assertEquals(failureType.getName(), event.getThrowableProxy().getClassName());
    assertEquals("first-sensitive-marker", event.getThrowableProxy().getMessage());
    assertTrue(event.getThrowableProxy().getStackTraceElementProxyArray().length > 0);
  }

  private static class BatchLogCapture implements AutoCloseable {
    private final Logger apiLogger = (Logger) LoggerFactory.getLogger("API");
    private final Level originalLevel = apiLogger.getLevel();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private BatchLogCapture() {
      appender.start();
      apiLogger.addAppender(appender);
      apiLogger.setLevel(Level.DEBUG);
    }

    private List<ILoggingEvent> events() {
      return matchingEvents("RPC execution failed for batch sub-request ");
    }

    private List<ILoggingEvent> executionEvents() {
      return matchingEvents("RPC execution failed");
    }

    private List<ILoggingEvent> matchingEvents(String prefix) {
      List<ILoggingEvent> events = new ArrayList<>();
      for (ILoggingEvent event : appender.list) {
        if (event.getMessage().startsWith(prefix)) {
          events.add(event);
        }
      }
      return events;
    }

    @Override
    public void close() {
      apiLogger.setLevel(originalLevel);
      apiLogger.detachAppender(appender);
      appender.stop();
    }
  }

  private List<JsonNode> stubBatchWithMiddleFailure(Throwable failure) throws Exception {
    return stubBatchWithMiddleFailure(failure, false);
  }

  private List<JsonNode> stubBatchWithMiddleFailure(Throwable failure, boolean partialOutput)
      throws Exception {
    List<JsonNode> dispatched = new ArrayList<>();
    doAnswer(inv -> {
      JsonNode request = MAPPER.readTree((InputStream) inv.getArgument(0));
      dispatched.add(request);
      if (dispatched.size() == 2) {
        if (partialOutput) {
          OutputStream output = inv.getArgument(1);
          output.write("{\"result\":\"partial-sensitive-marker".getBytes(StandardCharsets.UTF_8));
        }
        throw failure;
      }
      OutputStream out = inv.getArgument(1);
      out.write(successResponse(request.get("id").asInt()));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));
    return dispatched;
  }

  private List<Integer> stubBatchWithMalformedMiddleResponse() throws Exception {
    List<Integer> dispatched = new ArrayList<>();
    doAnswer(inv -> {
      JsonNode request = MAPPER.readTree((InputStream) inv.getArgument(0));
      int id = request.get("id").asInt();
      dispatched.add(id);
      OutputStream out = inv.getArgument(1);
      out.write(id == 2 ? "not-json".getBytes(StandardCharsets.UTF_8) : successResponse(id));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));
    return dispatched;
  }

  private static byte[] successResponse(int id) {
    return ("{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":" + id + "}")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static JsonNode recoveredBatch(String middleId) throws IOException {
    return MAPPER.readTree("[{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":1},"
        + "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"},"
        + "\"id\":" + middleId + "},{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":3}]");
  }

  private static void assertRecoveredBatch(MockHttpServletResponse response, String middleId)
      throws IOException {
    assertEquals(200, response.getStatus());
    assertEquals("application/json-rpc", response.getContentType());
    assertEquals(recoveredBatch(middleId), MAPPER.readTree(response.getContentAsByteArray()));
  }

  private static int limitBeforeSecondErrorFits() throws IOException {
    ArrayNode firstTwo = MAPPER.createArrayNode();
    firstTwo.add(recoveredBatch("2").get(0));
    firstTwo.add(recoveredBatch("2").get(1));
    return MAPPER.writeValueAsBytes(firstTwo).length - 1;
  }

  private static void assertRecoveredErrorConsumesBudget(MockHttpServletResponse response)
      throws IOException {
    assertEquals(200, response.getStatus());
    assertEquals("application/json-rpc", response.getContentType());
    ArrayNode expected = (ArrayNode) recoveredBatch("2");
    expected.set(2, MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32003,"
        + "\"message\":\"Response exceeds the limit of "
        + CommonParameter.getInstance().getJsonRpcMaxResponseSize()
        + " bytes\"},\"id\":3}"));
    assertEquals(expected, MAPPER.readTree(response.getContentAsByteArray()));
  }

  private static void assertBatchErrorOverflow(MockHttpServletResponse response)
      throws IOException {
    assertEquals(200, response.getStatus());
    assertEquals("application/json-rpc", response.getContentType());
    JsonNode body = MAPPER.readTree(response.getContentAsByteArray());
    assertEquals(3, body.size());
    assertEquals(recoveredBatch("2").get(0), body.get(0));
    for (int i = 1; i < body.size(); i++) {
      assertEquals("2.0", body.get(i).get("jsonrpc").asText());
      assertEquals(-32003, body.get(i).get("error").get("code").asInt());
      assertEquals("Response exceeds the limit of "
              + CommonParameter.getInstance().getJsonRpcMaxResponseSize() + " bytes",
          body.get(i).get("error").get("message").asText());
      assertFalse(body.get(i).get("error").has("data"));
      assertEquals(i + 1, body.get(i).get("id").asInt());
    }
  }

  private static void assertInvalidRequestWithNullId(JsonNode response) {
    assertEquals(MAPPER.getNodeFactory().textNode("2.0"), response.get("jsonrpc"));
    assertEquals(MAPPER.getNodeFactory().numberNode(-32600),
        response.get("error").get("code"));
    assertEquals(MAPPER.getNodeFactory().textNode("Invalid Request"),
        response.get("error").get("message"));
    assertFalse(response.get("error").has("data"));
    assertTrue(response.get("id").isNull());
  }

  private MockHttpServletResponse doPost(String body) throws Exception {
    MockHttpServletResponse resp = new MockHttpServletResponse();
    doPost(body, resp);
    return resp;
  }

  private void doPost(String body, HttpServletResponse response) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jsonrpc");
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    servlet.callDoPost(request, response);
  }

  private void assertCleanupFailureDoesNotReplaceFatal(HttpServletResponse response)
      throws Exception {
    StackOverflowError fatal = new StackOverflowError("original-fatal-marker");
    doThrow(fatal).when(mockRpcServer)
        .handleRequest(any(InputStream.class), any(OutputStream.class));

    StackOverflowError thrown = assertThrows(StackOverflowError.class,
        () -> doPost("{\"method\":\"eth_blockNumber\",\"id\":1}", response));

    assertSame(fatal, thrown);
  }

  private void assertWrappedFatalPropagates(Throwable failure, Error fatal, boolean batch)
      throws Exception {
    List<Integer> dispatched = new ArrayList<>();
    doAnswer(inv -> {
      JsonNode request = MAPPER.readTree((InputStream) inv.getArgument(0));
      int id = request.get("id").asInt();
      dispatched.add(id);
      if (id == (batch ? 2 : 1)) {
        throw failure;
      }
      OutputStream out = inv.getArgument(1);
      out.write(("{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":" + id + "}")
          .getBytes(StandardCharsets.UTF_8));
      return 0;
    }).when(mockRpcServer).handleRequest(any(InputStream.class), any(OutputStream.class));
    MockHttpServletResponse response = new MockHttpServletResponse();
    String request = batch ? "[{\"id\":1},{\"id\":2},{\"id\":3}]" : "{\"id\":1}";

    Error thrown = assertThrows(Error.class, () -> doPost(request, response));

    assertSame(fatal, thrown);
    assertEquals(500, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
    assertTrue(response.isCommitted());
    assertEquals(batch ? Arrays.asList(1, 2) : Arrays.asList(1), dispatched);
  }

  private static void assertSingleInternalError(MockHttpServletResponse response, String id)
      throws IOException {
    assertEquals(200, response.getStatus());
    assertEquals("application/json-rpc", response.getContentType());
    assertFalse(response.getContentAsString().contains("partial-sensitive-marker"));
    assertFalse(response.getContentAsString().contains("assertion-sensitive-marker"));
    assertEquals(MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,"
        + "\"message\":\"Internal error\"},\"id\":" + id + "}"),
        MAPPER.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(response.getContentAsByteArray()));
  }

  private static class TrackingResponseWrapper extends HttpServletResponseWrapper {

    private int cleanupCalls;

    TrackingResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public boolean isCommitted() {
      cleanupCalls++;
      return true;
    }

    @Override
    public void resetBuffer() {
      cleanupCalls++;
    }

    @Override
    public void flushBuffer() {
      cleanupCalls++;
    }
  }

  private static class TestableServlet extends JsonRpcServlet {

    void callDoPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      doPost(req, resp);
    }
  }
}
