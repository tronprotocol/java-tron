package org.tron.core.services.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

/**
 * Selects which state view (HEAD / SOLIDITY / PBFT) the http servlets on this port read from.
 *
 * <p>The read cursor is a per-thread setting ({@code ThreadLocal} in {@code Chainbase}): it only
 * decides from which snapshot the current thread starts its reads, and has no effect on any
 * other thread. This filter sets the cursor before the servlet runs, so the same stateless
 * servlet beans can serve {@code /wallet} (HEAD), {@code /walletsolidity} (SOLIDITY) and
 * {@code /walletpbft} (PBFT) without per-port subclasses.
 *
 * <p>The cursor is always reset to HEAD in a finally block: jetty pools its worker threads, and
 * a leftover cursor would leak into the next request served by the same thread. Only read-only
 * endpoints may be mounted behind this filter — write paths must run with the HEAD cursor.
 */
public abstract class WalletCursorFilter implements Filter {

  private final Manager dbManager;
  private final Chainbase.Cursor cursor;

  protected WalletCursorFilter(Manager dbManager, Chainbase.Cursor cursor) {
    this.dbManager = dbManager;
    this.cursor = cursor;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain filterChain) throws IOException, ServletException {
    try {
      dbManager.setCursor(cursor);
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      dbManager.resetCursor();
    }
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
