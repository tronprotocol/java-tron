package org.tron.core.services.jsonrpc;

import javax.servlet.http.HttpServletResponse;

/** HTTP response cleanup shared by the public and Admin JSON-RPC servlets. */
public final class JsonRpcHttpResponse {

  private JsonRpcHttpResponse() {
  }

  /**
   * Best-effort empty HTTP 500 before the caller rethrows a fatal error. Already committed
   * responses are left untouched, and cleanup failures never replace the original error.
   */
  public static void commitBareInternalServerError(HttpServletResponse resp) {
    try {
      if (resp.isCommitted()) {
        return;
      }
      // Commit before rethrowing so the container does not render its default error page,
      // which may expose Throwable details.
      resp.resetBuffer();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.setContentLength(0);
      resp.flushBuffer();
    } catch (Throwable ignored) {
      // Cleanup must never replace the original fatal Error.
    }
  }
}
