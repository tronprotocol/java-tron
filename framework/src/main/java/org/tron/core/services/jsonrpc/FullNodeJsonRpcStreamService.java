package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.StreamServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;

@Component
@Slf4j(topic = "API")
// public class FullNodeJsonRpcService extends HttpServlet {
public class FullNodeJsonRpcStreamService implements Service {

  // private int port = Args.getInstance().getFullNodeHttpPort();
  private int port = 8099;

  private JsonRpcServer jsonRpcServer;
  private TestService testService;
  private StreamServer streamServer;

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter args) {
  }

  // protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
  //   jsonRpcServer.handle(req, resp);
  // }

  @Override
  public void start() {
    testService = new TestServiceImpl();

    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        this.getClass().getClassLoader(),
        new Object[] { testService},
        new Class<?>[] { TestService.class},
        true);

    jsonRpcServer = new JsonRpcServer(compositeService);


    // create the stream server
    int maxThreads = 50;
    ServerSocket serverSocket;
    try {
      // serverSocket = new ServerSocket(port);
      InetAddress bindAddress = InetAddress.getByName("127.0.0.1");
      serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 0, bindAddress);
    } catch (Exception e) {
      return;
    }
    streamServer = new StreamServer(jsonRpcServer, maxThreads, serverSocket);

// start it, this method doesn't block
    streamServer.start();
  }

  @Override
  public void stop() {
    try {
      streamServer.stop();
    } catch (Exception e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}
