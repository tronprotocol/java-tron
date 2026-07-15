package org.tron.core.services.filter;

import static org.junit.Assert.assertThrows;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class HttpInterceptorTest {

  private final HttpInterceptor interceptor = new HttpInterceptor();

  @Test
  public void testOversizedBadMessagePropagates() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jsonrpc");
    request.setServletPath("/jsonrpc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, resp) -> {
      throw new BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413,
          "Request body is too large");
    };

    BadMessageException e = assertThrows(BadMessageException.class,
        () -> interceptor.doFilter(request, response, chain));

    org.junit.Assert.assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413, e.getCode());
  }

  @Test
  public void testNonOversizedExceptionIsStillSwallowed() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jsonrpc");
    request.setServletPath("/jsonrpc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, resp) -> {
      throw new ServletException("expected");
    };

    interceptor.doFilter(request, response, chain);
  }
}
