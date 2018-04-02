package org.tron.core.net2.peer;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net2.message.TMessage;
import org.tron.core.net2.message.TMessageType;
import org.tron.core.net2.nio.Address;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

@Slf4j(topic = "core.net2")
public class PeerMsgProccess {

    public static void processMsg(TMessage msg){
        switch (msg.getMsgType()) {
            case TMessageType.ADD_PEER:
                PeerManager.addPeer(msg.getChannel());
                break;
            case TMessageType.REMOVE_PEER:
                PeerManager.removePeer(msg.getChannel());
                break;
            case TMessageType.GET_PEERS:
                proGetPeersMsg(msg.getChannel());
                break;
            case TMessageType.RCV_GET_PEERS:
                break;
            default:
                break;
        }
    }

    public static void proGetPeersMsg(SocketChannel channel){
        ArrayList<String> list = Lists.newArrayList();
        for (PeerInfo peerInfo: PeerManager.getPeers()) {
            try {
                list.add(Address.create(peerInfo.getChannel()).toString());
            } catch (Exception e) {
                logger.info("add peer address failed.", e);
            }
        }

        TMessage msg = new TMessage((byte) 1, TMessageType.RCV_GET_PEERS, list.toString().getBytes(), channel);
        PeerClient.getInstance().sendMsg(msg);
    }
    public static void proRcvGetPeersMsg(TMessage msg){
        logger.info(msg.getVersion() + "");
        logger.info(msg.getMsgType() + "");
        logger.info(msg.getChannel() + "");
    }
}
