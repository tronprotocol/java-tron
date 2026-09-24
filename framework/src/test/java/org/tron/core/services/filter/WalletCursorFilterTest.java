package org.tron.core.services.filter;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;
import org.mockito.InOrder;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

/**
 * Behaviour of the cursor filters that replaced the per-servlet OnSolidity / OnPBFT wrappers.
 * The route tests only prove which paths are mounted; these prove the cursor is actually switched
 * for the request and always reset, so a solidity/pbft read cannot silently serve HEAD state and a
 * cursor cannot leak onto a pooled jetty thread.
 */
public class WalletCursorFilterTest {

  @Test
  public void testSolidityFilterSetsCursorBeforeChainAndResetsAfter() throws Exception {
    Manager manager = mock(Manager.class);
    FilterChain chain = mock(FilterChain.class);
    ServletRequest req = mock(ServletRequest.class);
    ServletResponse resp = mock(ServletResponse.class);

    new SolidityCursorFilter(manager).doFilter(req, resp, chain);

    // cursor is set to SOLIDITY before the servlet runs and reset only after it returns
    InOrder order = inOrder(manager, chain);
    order.verify(manager).setCursor(Chainbase.Cursor.SOLIDITY);
    order.verify(chain).doFilter(req, resp);
    order.verify(manager).resetCursor();
  }

  @Test
  public void testPbftFilterSetsCursorBeforeChainAndResetsAfter() throws Exception {
    Manager manager = mock(Manager.class);
    FilterChain chain = mock(FilterChain.class);
    ServletRequest req = mock(ServletRequest.class);
    ServletResponse resp = mock(ServletResponse.class);

    new PbftCursorFilter(manager).doFilter(req, resp, chain);

    InOrder order = inOrder(manager, chain);
    order.verify(manager).setCursor(Chainbase.Cursor.PBFT);
    order.verify(chain).doFilter(req, resp);
    order.verify(manager).resetCursor();
  }

  @Test
  public void testCursorIsResetWhenChainThrows() throws Exception {
    Manager manager = mock(Manager.class);
    FilterChain chain = mock(FilterChain.class);
    ServletRequest req = mock(ServletRequest.class);
    ServletResponse resp = mock(ServletResponse.class);
    doThrow(new ServletException("boom")).when(chain).doFilter(req, resp);

    try {
      new SolidityCursorFilter(manager).doFilter(req, resp, chain);
      fail("expected the chain exception to propagate");
    } catch (ServletException expected) {
      // expected
    }
    // the finally block must still restore HEAD, or the next request on this thread reads SOLIDITY
    verify(manager).resetCursor();
  }
}
