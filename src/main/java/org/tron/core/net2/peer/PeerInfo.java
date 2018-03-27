package org.tron.core.net2.peer;

import java.nio.channels.SocketChannel;

public class PeerInfo {

    public static byte STATUS_ACTIVE = 1;
    public static byte STATUS_WAIT_SYNC = 2;
    public static byte STATUS_WAIT_SYNC_ACK = 3;

    private SocketChannel channel;

    private byte status;

    private long lastActiveTime;

    private short clusterSize;

    private long packetNum;

    private long lastRcvDataMessageTime;

    public PeerInfo(SocketChannel channel){
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }
}
