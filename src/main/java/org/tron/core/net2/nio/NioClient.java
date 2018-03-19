package org.tron.core.net2.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioClient {

    private InetSocketAddress address;

    public NioClient(InetSocketAddress address) {
        this.address = address;
    }

    public void connect() throws IOException{

        SocketChannel socketChannel = SocketChannel.open();

        socketChannel.configureBlocking(false);

        socketChannel.register(EventHandle.getInstance().getSelector(), SelectionKey.OP_CONNECT);

        socketChannel.connect(address);
    }

}
