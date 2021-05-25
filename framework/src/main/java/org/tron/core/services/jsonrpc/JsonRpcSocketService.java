package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.StreamServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;

@Component
public class JsonRpcService implements Service {

  private int port = 50063;

  @Autowired
  private AddService addService;
  private JsonRpcServer jsonRpcServer;
  private StreamServer streamServer;

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter parameter) {
  }

  @Override
  public void start() {

    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        this.getClass().getClassLoader(),
        new Object[] {new AddService()},
        new Class<?>[] {AddInterface.class}, //interface类名
        true);
    jsonRpcServer = new JsonRpcServer(compositeService);
    jsonRpcServer.setContentType("application/json-rpc");

    // create the stream server
    int maxThreads = 50;

    ServerSocket serverSocket = null;
    try {
      InetAddress bindAddress = InetAddress.getByName("localhost");
      serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 0, bindAddress);
    } catch (IOException e) {
      e.printStackTrace();
    }
    streamServer = new StreamServer(jsonRpcServer, maxThreads, serverSocket);

    // start it, this method doesn't block
    streamServer.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down jsonrpc server on solidity since JVM is shutting down");
      //server.this.stop();
      System.err.println("*** jsonrpc server on solidity shut down");
    }));
  }

  @Override
  public void stop() {
    if (streamServer != null) {
      try {
        streamServer.stop();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
