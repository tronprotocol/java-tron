package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.StreamServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tron.common.parameter.CommonParameter;

public class ClientDemo {

  //  private int port = Args.getInstance().getRpcOnSolidityPort() + 1;
  private int port = 8099;
  private JsonRpcServer jsonRpcServer;
  private StreamServer streamServer;


  public void client() throws Throwable {
    InetAddress bindAddress = InetAddress.getByName("127.0.0.1");
    Socket socket = new Socket(bindAddress, port);
    JsonRpcClient jsonRpcClient = new JsonRpcClient();

    // Map params = new HashMap<String, Integer>();
    // params.put("a", 3);
    // params.put("b", 4);
    List<Integer> params = new ArrayList<>();
    params.add(12);

    Integer res = jsonRpcClient.invokeAndReadResponse("getInt", params, Integer.class,
        socket.getOutputStream(), socket.getInputStream());
    System.out.println(res); //传参方法三：通过ProxyUtil 客户端调用

  }

  public static void main(String[] args) throws Throwable {
    ClientDemo demo = new ClientDemo();
    demo.client();
  }
}
