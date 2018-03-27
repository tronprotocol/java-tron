package org.tron.core.net2.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net2.message.TMessage;
import org.tron.core.net2.message.TMessageHandle;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerClient {

    private static final Logger logger = LoggerFactory.getLogger("PeerClient");

    private static PeerClient peerClient;

    private TMessageHandle msgHandle;

    private BlockingQueue<TMessage> msgQueue = new LinkedBlockingQueue<>(10000);

    public void init(){
        msgHandle = TMessageHandle.getInstance();
        new Thread(()->{
            while (true){
                TMessage msg = null;
                try{
                    msg = msgQueue.take();
                }catch (Exception e){
                    logger.error("take msg failed, ", e);
                }
                if (msg.getChannel() == null){
                    for (PeerInfo peerInfo: PeerManager.getPeers()){
                        sendMsg(msg, peerInfo.getChannel());
                    }
                }else {
                    sendMsg(msg, msg.getChannel());
                }
            }
        }).start();
    }

    private void sendMsg(TMessage msg, SocketChannel client){
        try{
            logger.info("send msg {} to [{}].", msg, client.getRemoteAddress());
            ByteBuffer sendBuffer = TMessage.encode(msg);
            while (sendBuffer.hasRemaining()){
                client.write(sendBuffer);
            }
        }catch (Exception e){
            logger.error("send msg failed.", e);
        }
    }

    public void sendMsg(TMessage msg){
        if (!msgQueue.offer(msg)){
            logger.warn("add msg to queue failed.");
        }
    }

    public static PeerClient getInstance(){
        if (peerClient == null){
            peerClient = new PeerClient();
        }
        return  peerClient;
    }
}
