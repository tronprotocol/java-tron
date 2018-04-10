package org.tron.core.net2.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerManager {

    private static final Logger logger = LoggerFactory.getLogger("PeerManager");

    private static short activeNum;

    private static short totalNum;

    private static Map<SocketChannel, PeerInfo> peers = new ConcurrentHashMap<>();

    public static boolean addPeer(SocketChannel channel){
        peers.put(channel, new PeerInfo(channel));
        logger.info("add a peer, peer-size: {}", peers.size());
        return  true;
    }

    public static void removePeer(SocketChannel channel){
        peers.remove(channel);
        logger.info("remove a peer, peer-size:  {}.", peers.size());
    }

    public static List<PeerInfo> getPeers(){
        List<PeerInfo> list = new ArrayList<>();
        peers.values().forEach(peer-> list.add(peer));
        return list;
    }
}
