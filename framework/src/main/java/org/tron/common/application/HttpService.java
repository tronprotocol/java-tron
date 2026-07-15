/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.application;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.SizeLimitHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.BufferUtil;
import org.tron.core.config.args.Args;

@Slf4j(topic = "rpc")
public abstract class HttpService extends AbstractService {

  protected Server apiServer;

  protected String contextPath;

  protected long maxRequestSize = 4 * 1024 * 1024; // 4MB

  @VisibleForTesting
  public long getMaxRequestSize() {
    return this.maxRequestSize;
  }

  @VisibleForTesting
  public void setMaxRequestSize(long maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
  }

  @Override
  public void innerStart() throws Exception {
    if (this.apiServer != null) {
      this.apiServer.start();
    }
  }

  @Override
  public void innerStop() throws Exception {
    if (this.apiServer != null) {
      this.apiServer.stop();
    }
  }

  @Override
  public CompletableFuture<Boolean> start() {
    initServer();
    ServletContextHandler context = initContextHandler();
    addServlet(context);
    addFilter(context);
    return super.start();
  }

  protected void initServer() {
    this.apiServer = new Server(this.port);
    int maxHttpConnectNumber = Args.getInstance().getMaxHttpConnectNumber();
    if (maxHttpConnectNumber > 0) {
      this.apiServer.addBean(new ConnectionLimit(maxHttpConnectNumber, this.apiServer));
    }
    this.apiServer.setErrorHandler(new OversizedRequestErrorHandler());
  }

  protected ServletContextHandler initContextHandler() {
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath(this.contextPath);
    SizeLimitHandler sizeLimitHandler = new SizeLimitHandler(this.maxRequestSize, -1);
    sizeLimitHandler.setHandler(context);
    this.apiServer.setHandler(sizeLimitHandler);
    return context;
  }

  protected abstract void addServlet(ServletContextHandler context);

  protected void addFilter(ServletContextHandler context) {

  }

  /**
   * For oversized requests (the 413 thrown by SizeLimitHandler during dispatch) logs the
   * detail server-side and returns the short, uniform bad-message page, instead of the
   * default error page that leaks the exception stack and internal request sizes. All
   * other errors keep Jetty's default handling.
   */
  private static final class OversizedRequestErrorHandler extends ErrorHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      if (response.getStatus() == HttpStatus.PAYLOAD_TOO_LARGE_413) {
        Throwable cause = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        logger.info("Reject oversized request, uri: {}, detail: {}",
            request.getRequestURI(), cause == null ? "413" : cause.getMessage());
        baseRequest.setHandled(true);
        ByteBuffer body = badMessageError(HttpStatus.PAYLOAD_TOO_LARGE_413,
            HttpStatus.getMessage(HttpStatus.PAYLOAD_TOO_LARGE_413),
            baseRequest.getResponse().getHttpFields());
        response.getOutputStream().write(BufferUtil.toArray(body));
        return;
      }
      super.handle(target, baseRequest, request, response);
    }
  }
}
