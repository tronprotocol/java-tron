package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.services.interfaceJsonRpcOnPBFT.JsonRpcOnPBFTServlet;
import org.tron.core.services.interfaceJsonRpcOnSolidity.JsonRpcOnSolidityServlet;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;
import org.tron.core.services.jsonrpc.JsonRpcServlet;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;

@RunWith(Parameterized.class)
public class JsonRpcRateLimiterServletTest {

  private final Class<? extends JsonRpcServlet> servletClass;
  private RateLimiterServlet servlet;
  private IRateLimiter perEndpoint;
  private Object dispatcher;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  public JsonRpcRateLimiterServletTest(Class<? extends JsonRpcServlet> servletClass) {
    this.servletClass = servletClass;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> servlets() {
    return Arrays.asList(new Object[][] {
        {JsonRpcServlet.class},
        {JsonRpcOnSolidityServlet.class},
        {JsonRpcOnPBFTServlet.class}
    });
  }

  @Before
  public void setUp() throws Exception {
    // Initialize Args before GlobalRateLimiter's static QPS limiters are loaded.
    Args.setParam(new String[0], TestConstants.TEST_CONF);
    servlet = servletClass.getDeclaredConstructor().newInstance();
    RateLimiterContainer container = new RateLimiterContainer();
    perEndpoint = mock(IRateLimiter.class);
    container.add("http_", servletClass.getSimpleName(), perEndpoint);
    ReflectionTestUtils.setField(servlet, "container", container);

    if (servlet instanceof JsonRpcOnSolidityServlet) {
      dispatcher = mock(WalletOnSolidity.class);
      ReflectionTestUtils.setField(servlet, "walletOnSolidity", dispatcher);
    } else if (servlet instanceof JsonRpcOnPBFTServlet) {
      dispatcher = mock(WalletOnPBFT.class);
      ReflectionTestUtils.setField(servlet, "walletOnPBFT", dispatcher);
    } else {
      dispatcher = mock(JsonRpcServer.class);
      ReflectionTestUtils.setField(servlet, "rpcServer", dispatcher);
    }

    request = new MockHttpServletRequest("POST", "/jsonrpc");
    request.setServletPath("/jsonrpc");
    request.setRemoteAddr("10.0.0.1");
    request.setContentType("application/json");
    request.setContent("{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"id\":1}"
        .getBytes(StandardCharsets.UTF_8));
    response = new MockHttpServletResponse();
  }

  @After
  public void tearDown() {
    Args.clearParam();
  }

  @Test
  public void testPerEndpointRejectionReturnsSanitizedHttpError() throws Exception {
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(false);

    try (MockedStatic<GlobalRateLimiter> global = mockStatic(GlobalRateLimiter.class)) {
      servlet.service(request, response);

      global.verify(() -> GlobalRateLimiter.acquirePermit(any()), never());
      assertRateLimitResponse();
    }
  }

  @Test
  public void testGlobalRejectionReturnsSanitizedHttpError() throws Exception {
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);

    try (MockedStatic<GlobalRateLimiter> global = mockStatic(GlobalRateLimiter.class)) {
      global.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(false);

      servlet.service(request, response);

      global.verify(() -> GlobalRateLimiter.acquirePermit(any()));
      assertRateLimitResponse();
    }
  }

  private void assertRateLimitResponse() throws Exception {
    assertEquals(200, response.getStatus());
    assertEquals("application/json; charset=utf-8", response.getContentType());
    assertEquals("{\"Error\":\"lack of computing resources\"}",
        response.getContentAsString().trim());
    verify(perEndpoint).acquirePermit(any(RuntimeData.class));
    verifyNoInteractions(dispatcher);
  }
}
