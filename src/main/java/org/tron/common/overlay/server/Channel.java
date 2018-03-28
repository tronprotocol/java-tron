/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.discover.NodeManager;
import org.tron.common.overlay.discover.NodeStatistics;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.MessageCodec;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.common.overlay.message.StaticMessages;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.net.peer.PeerConnectionDelegate;
import org.tron.core.net.peer.TronHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
public class Channel {

    private final static Logger logger = LoggerFactory.getLogger("Channel");

    @Autowired
    protected MessageQueue msgQueue;

    @Autowired
    private MessageCodec messageCodec;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private StaticMessages staticMessages;

    @Autowired
    private WireTrafficStats stats;

    @Autowired
    private HandshakeHandler handshakeHandler;

    @Autowired
    private P2pHandler p2pHandler;

    @Autowired
    private TronHandler tronHandler;

    private ChannelManager channelManager;

    private InetSocketAddress inetSocketAddress;

    private Node node;

    private PeerConnectionDelegate peerDel;

    public TronState getTronState() {
        return tronState;
    }

    public void setTronState(TronState tronState) {
        //logger.info("channel {} state [{}] change to [{}]", inetSocketAddress, this.tronState, tronState);
        this.tronState = tronState;
    }

    private TronState tronState = TronState.INIT;

    protected NodeStatistics nodeStatistics;
    private boolean discoveryMode;
    private boolean isActive;

    private String remoteId;

    private PeerStatistics peerStats = new PeerStatistics();

    public void init(ChannelPipeline pipeline, String remoteId, boolean discoveryMode,
        ChannelManager channelManager, PeerConnectionDelegate peerDel) {
        this.channelManager = channelManager;
        this.remoteId = remoteId;

        isActive = remoteId != null && !remoteId.isEmpty();

        //TODO: use config here
        pipeline.addLast("readTimeoutHandler",
            new ReadTimeoutHandler(60, TimeUnit.SECONDS));
        pipeline.addLast(stats.tcp);
        pipeline.addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast("lengthDecode", new ProtobufVarint32FrameDecoder());
        //handshake first
        pipeline.addLast("handshakeHandler", handshakeHandler);

        this.discoveryMode = discoveryMode;
        this.peerDel = peerDel;

        messageCodec.setChannel(this);
        msgQueue.setChannel(this);
        handshakeHandler.setChannel(this, remoteId);
        p2pHandler.setChannel(this);
        tronHandler.setChannel(this);

        p2pHandler.setMsgQueue(msgQueue);
        tronHandler.setMsgQueue(msgQueue);
        tronHandler.setPeerDel(peerDel);

        logger.info("Channel init finished");
    }

    public void publicHandshakeFinished(ChannelHandlerContext ctx, HelloMessage helloRemote) throws IOException, InterruptedException {
        ctx.pipeline().addLast("messageCodec", messageCodec);
        ctx.pipeline().addLast("p2p", p2pHandler);
        ctx.pipeline().addLast("data", tronHandler);
        setTronState(TronState.HANDSHAKE_FINISHED);
    }

    public void sendHelloMessage(ChannelHandlerContext ctx) throws IOException, InterruptedException {
        final HelloMessage helloMessage = staticMessages.createHelloMessage(nodeManager.getPublicHomeNode());
        ctx.writeAndFlush(helloMessage.getSendData()).sync();
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }

    public NodeStatistics getNodeStatistics() {
        return nodeStatistics;
    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    public void initWithNode(byte[] nodeId, int remotePort) {
        node = new Node(nodeId, inetSocketAddress.getHostString(), remotePort);
        nodeStatistics = nodeManager.getNodeStatistics(node);
    }

    public Node getNode() {
        return node;
    }

    public boolean isProtocolsInitialized() {
        return tronState.ordinal() > TronState.INIT.ordinal();
    }

    public String logSyncStats() {
        //TODO: return tron sync status here.
//    int waitResp = lastReqSentTime > 0 ? (int) (System.currentTimeMillis() - lastReqSentTime) / 1000 : 0;
//    long lifeTime = System.currentTimeMillis() - connectedTime;
        return "";
//        return String.format(
//            "Peer %s: [ %18s, ping %6s ms, last know block num %s ]: needSyncFromPeer:%b needSyncFromUs:%b",
//            this.getNode().getHost() + ":" + this.getNode().getPort(),
//            this.getPeerIdShort(),
//            (int)this.getPeerStats().getAvgLatency(),
//            headBlockWeBothHave.getNum(),
//            isNeedSyncFromPeer(),
//            isNeedSyncFromUs());
    }

    public String getPeerId() {
        return node == null ? "<null>" : node.getHexId();
    }

    public String getPeerIdShort() {
        return node == null ? (remoteId != null && remoteId.length() >= 8 ? remoteId.substring(0,8) :remoteId)
                : node.getHexIdShort();
    }

    public byte[] getNodeId() {
        return node == null ? null : node.getId();
    }

    /**
     * Indicates whether this connection was initiated by our peer
     */
    public boolean isActive() {
        return isActive;
    }

    public ByteArrayWrapper getNodeIdWrapper() {
        return node == null ? null : new ByteArrayWrapper(node.getId());
    }

    public void disconnect(ReasonCode reason) {
         msgQueue.disconnect(reason);
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public PeerStatistics getPeerStats() {
        return peerStats;
    }

    public enum TronState {
        INIT,
        HANDSHAKE_FINISHED,
        START_TO_SYNC,
        SYNCING,
        SYNC_COMPLETED,
        SYNC_FAILED
    }

    public boolean isIdle() {
        // TODO: use peer's status.
        return  true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;


        if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress) : channel.inetSocketAddress != null) return false;
        if (node != null ? !node.equals(channel.node) : channel.node != null) return false;
        return this == channel;
    }

    @Override
    public int hashCode() {
        int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
        result = 31 * result + (node != null ? node.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", getPeerId(), inetSocketAddress);
    }
}
