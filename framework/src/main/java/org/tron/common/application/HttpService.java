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

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.tron.core.config.args.Args;

@Slf4j(topic = "rpc")
public abstract class HttpService extends AbstractService {

  protected Server apiServer;

  protected String contextPath;

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
  }

  protected ServletContextHandler initContextHandler() {
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath(this.contextPath);
    this.apiServer.setHandler(context);
    return context;
  }

  protected abstract void addServlet(ServletContextHandler context);

  protected void addFilter(ServletContextHandler context) {

  }
}
