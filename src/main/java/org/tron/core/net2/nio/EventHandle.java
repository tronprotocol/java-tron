package org.tron.core.net2.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net2.message.TMessage;
import org.tron.core.net2.message.TMessageHandle;
import org.tron.core.net2.message.TMessageType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EventHandle {

    private static final Logger logger = LoggerFactory.getLogger("EventHandle");

    private static EventHandle eventHandle;

    private  Selector selector = null;

    private  int bufferSize = 10 * 1024 * 1024;

    private  ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);

    public void init() {
        try{
            selector = Selector.open();
            new Thread(()-> {
                while (true) {
                    try{
                        selector.select(5000);
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()) {

                            SelectionKey selectionKey = iterator.next();
                            logger.info(selectionKey.isAcceptable()+ "," + selectionKey.isReadable() + "," +selectionKey.isWritable());
                            iterator.remove();
                            handleKey(selectionKey);
                        }
                    }catch (Exception e){
                        logger.error("handleKey failed", e);
                        System.exit(0);
                    }
                }
            }).start();
        }catch (Exception e){
            logger.error("selector open failed.", e);
            System.exit(0);
        }
    }

    private void handleKey(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
            SocketChannel client = server.accept();
            logger.info("rcv accept from {}", client.getRemoteAddress());
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            TMessageHandle.getInstance().handleMsg(new TMessage(TMessageType.ADD_PEER, client));
        }else if (selectionKey.isConnectable()){
            SocketChannel client = (SocketChannel) selectionKey.channel();
            logger.info("rcv connect from {}", client.getRemoteAddress());
            if (!client.finishConnect()) {
                logger.error("connect failed {}", client.getRemoteAddress());
            }else {
                client.register(selector, SelectionKey.OP_READ);
                TMessageHandle.getInstance().handleMsg(new TMessage(TMessageType.ADD_PEER, client));
            }
        }else if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();
            StringBuilder msgBuild = new StringBuilder();
            int  size = client.read(readBuffer);
            while (size > 0) {
                msgBuild.append(new String(readBuffer.array(), 0, bufferSize - readBuffer.remaining()));
                readBuffer.clear();
                size = client.read(readBuffer);
            }
            if (size < 0){
                client.close();
                TMessageHandle.getInstance().handleMsg(new TMessage(TMessageType.REMOVE_PEER, client));
            }else {
                TMessage msg = TMessage.decode(msgBuild, client);
                TMessageHandle.getInstance().handleMsg(msg);
                logger.info("rsv msg {} from [{}]", msg, client.getRemoteAddress());
            }
        }
    }

    public Selector getSelector(){
        return  selector;
    }

    public static EventHandle getInstance(){
        if (eventHandle == null){
            eventHandle = new EventHandle();
        }
        return eventHandle;
    }

}
