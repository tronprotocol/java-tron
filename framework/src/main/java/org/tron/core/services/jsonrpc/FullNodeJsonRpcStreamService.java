package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.StreamServer;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Wallet;
import org.tron.core.services.NodeInfoService;

@Component
@Slf4j(topic = "API")
public class FullNodeJsonRpcStreamService implements Service {

  private int port = 8099;

  private JsonRpcServer jsonRpcServer;
  private TronJsonRpcImpl testServiceImpl;
  private StreamServer streamServer;

  @Autowired
  private NodeInfoService nodeInfoService;
  @Autowired
  private Wallet wallet;

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter args) {
  }

  @Override
  public void start() {
    testServiceImpl = new TronJsonRpcImpl(nodeInfoService, wallet);

    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        this.getClass().getClassLoader(),
        new Object[] {testServiceImpl},
        new Class<?>[] {TronJsonRpc.class},
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
