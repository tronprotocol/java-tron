package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.parameter.CommonParameter;

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
    Field f = JsonRpcServlet.class.getDeclaredField("rpcServer");
    f.setAccessible(true);
    f.set(servlet, mockRpcServer);
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
    byte[] rpcResp = "[{\"result\":\"ok\"}]".getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      HttpServletResponse r = inv.getArgument(1);
      r.getOutputStream().write(rpcResp);
      return null;
    }).when(mockRpcServer).handle(any(HttpServletRequest.class), any(HttpServletResponse.class));

    MockHttpServletResponse resp = doPost("[{\"id\":1},{\"id\":2}]");
    assertEquals(200, resp.getStatus());
    assertArrayEquals(rpcResp, resp.getContentAsByteArray());
  }

  @Test
  public void batchLimitDisabled_largeBatchAllowed() throws Exception {
    CommonParameter.getInstance().jsonRpcMaxBatchSize = 0;
    byte[] rpcResp = "[]".getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      HttpServletResponse r = inv.getArgument(1);
      r.getOutputStream().write(rpcResp);
      return null;
    }).when(mockRpcServer).handle(any(HttpServletRequest.class), any(HttpServletResponse.class));

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
    assertArrayEquals(rpcResp, resp.getContentAsByteArray());
  }

  // --- rpcServer.handle exceptions ---

  @Test
  public void rpcServerThrowsRuntimeException_returnsInternalError() throws Exception {
    doThrow(new RuntimeException("server exploded")).when(mockRpcServer)
        .handle(any(HttpServletRequest.class), any(HttpServletResponse.class));
    MockHttpServletResponse resp = doPost("{\"method\":\"eth_blockNumber\",\"id\":42}");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertFalse(body.isArray());
    assertEquals(-32603, body.get("error").get("code").asInt());
  }

  @Test
  public void batchRpcServerThrows_internalErrorIsArray() throws Exception {
    doThrow(new RuntimeException("boom")).when(mockRpcServer)
        .handle((HttpServletRequest) any(), any());
    MockHttpServletResponse resp = doPost("[{\"method\":\"eth_blockNumber\"}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("batch internal error must be an array", body.isArray());
    assertEquals(-32603, body.get(0).get("error").get("code").asInt());
  }

  // --- response size limit ---

  @Test
  public void responseTooLarge_returnsSingleErrorObject() throws Exception {
    int limit = 50;
    CommonParameter.getInstance().jsonRpcMaxResponseSize = limit;
    doAnswer(inv -> {
      HttpServletResponse r = inv.getArgument(1);
      r.getOutputStream().write(new byte[limit + 1]);
      return null;
    }).when(mockRpcServer).handle(any(HttpServletRequest.class), any(HttpServletResponse.class));

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
      HttpServletResponse r = inv.getArgument(1);
      r.getOutputStream().write(new byte[limit + 1]);
      return null;
    }).when(mockRpcServer).handle(any(HttpServletRequest.class), any(HttpServletResponse.class));

    MockHttpServletResponse resp = doPost("[{\"method\":\"eth_getLogs\"}]");
    assertEquals(200, resp.getStatus());
    JsonNode body = MAPPER.readTree(resp.getContentAsString());
    assertTrue("batch response-too-large must be an array", body.isArray());
    assertEquals(-32003, body.get(0).get("error").get("code").asInt());
  }

  // --- normal path ---

  @Test
  public void normalRequest_commitsRpcServerResponse() throws Exception {
    byte[] rpcResp = "{\"result\":\"0x1\"}".getBytes(StandardCharsets.UTF_8);
    doAnswer(inv -> {
      HttpServletResponse r = inv.getArgument(1);
      r.getOutputStream().write(rpcResp);
      return null;
    }).when(mockRpcServer).handle(any(HttpServletRequest.class), any(HttpServletResponse.class));

    MockHttpServletResponse resp = doPost("{\"method\":\"eth_blockNumber\",\"id\":1}");
    assertEquals(200, resp.getStatus());
    assertArrayEquals(rpcResp, resp.getContentAsByteArray());
  }

  // --- helpers ---

  private MockHttpServletResponse doPost(String body) throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/jsonrpc");
    req.setContent(body.getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse resp = new MockHttpServletResponse();
    servlet.callDoPost(req, resp);
    return resp;
  }

  private static class TestableServlet extends JsonRpcServlet {

    void callDoPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      doPost(req, resp);
    }
  }
}
