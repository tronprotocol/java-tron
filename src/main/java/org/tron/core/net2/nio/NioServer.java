package org.tron.core.net2.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

public class NioServer {

    private int port;

    public NioServer(int port)  {
       this.port = port;
    }

    public void start() throws IOException{

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.configureBlocking(false);

        ServerSocket serverSocket = serverSocketChannel.socket();

        serverSocket.bind(new InetSocketAddress(port));

        serverSocketChannel.register(EventHandle.getInstance().getSelector(), SelectionKey.OP_ACCEPT);
    }
}