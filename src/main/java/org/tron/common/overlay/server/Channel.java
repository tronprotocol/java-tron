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
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
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
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.TronHandler;


/**
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Component
@Scope("prototype")
public class Channel {

    private final static Logger logger = LoggerFactory.getLogger("net");

    @Autowired
    Args args;

    @Autowired
    private MessageQueue msgQueue;

    @Autowired
    private P2pHandler p2pHandler;

    @Autowired
    private MessageCodec messageCodec;

    @Autowired
    private HandshakeHandler handshakeHandler;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private StaticMessages staticMessages;

    @Autowired
    private WireTrafficStats stats;

    private TronHandler tronHandler;

    private ChannelManager channelManager;

    private InetSocketAddress inetSocketAddress;

    private PeerConnection node;
    private NodeStatistics nodeStatistics;

    private boolean discoveryMode;
    private boolean isActive;
    private boolean isDisconnected;

    private String remoteId;

    private PeerStatistics peerStats = new PeerStatistics();

    public void init(ChannelPipeline pipeline, String remoteId, boolean discoveryMode, ChannelManager channelManager) {
        this.channelManager = channelManager;
        this.remoteId = remoteId;

        isActive = remoteId != null && !remoteId.isEmpty();

        pipeline.addLast("readTimeoutHandler",
            new ReadTimeoutHandler(args.getNodeChannelReadTimeout(), TimeUnit.SECONDS));
        pipeline.addLast(stats.tcp);
        //handshake first
        pipeline.addLast("handshakeHandler", handshakeHandler);

        this.discoveryMode = discoveryMode;

        if (discoveryMode) {
            // temporary key/nodeId to not accidentally smear our reputation with
            // unexpected disconnect
//            handshakeHandler.generateTempKey();
        }

        handshakeHandler.setRemoteId(remoteId, this);

        messageCodec.setChannel(this);

        msgQueue.setChannel(this);

        p2pHandler.setMsgQueue(msgQueue);

    }

    public void publicRLPxHandshakeFinished(ChannelHandlerContext ctx, HelloMessage helloRemote) throws IOException, InterruptedException {

        logger.debug("publicRLPxHandshakeFinished with " + ctx.channel().remoteAddress());

//        FrameCodecHandler frameCodecHandler = new FrameCodecHandler(frameCodec, this);
//        ctx.pipeline().addLast("medianFrameCodec", frameCodecHandler);
        //TODO: use messageCodec handle bytes to message directly
        ctx.pipeline().addLast("messageCodec", messageCodec);
        ctx.pipeline().addLast("p2p", p2pHandler);

        p2pHandler.setChannel(this);
        p2pHandler.setHandshake(helloRemote, ctx);

        getNodeStatistics().rlpxHandshake.add();
    }

    public void sendHelloMessage(ChannelHandlerContext ctx,
        String nodeId) throws IOException, InterruptedException {

        final HelloMessage helloMessage = staticMessages.createHelloMessage(nodeId);
        //ByteBuf byteBufMsg = ctx.alloc().buffer();
        ctx.writeAndFlush(helloMessage).sync();

        if (logger.isDebugEnabled())
            logger.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), helloMessage);
        getNodeStatistics().rlpxOutHello.add();
    }

    public void activateTron(ChannelHandlerContext ctx) {
        //TODO: use tron handle here.

        tronHandler = new TronHandler();
        ctx.pipeline().addLast("data", tronHandler);
        tronHandler.setMsgQueue(msgQueue);
        tronHandler.setChannel(this);
        tronHandler.setPeerDiscoveryMode(discoveryMode);
        tronHandler.setPeerDel();
//        EthHandler handler = ethHandlerFactory.create(version);
//        MessageFactory messageFactory = createEthMessageFactory(version);
//        messageCodec.setEthVersion(version);
//        messageCodec.setEthMessageFactory(messageFactory);
//
//        ctx.pipeline().addLast("data", handler);
//
//        handler.setMsgQueue(msgQueue);
//        handler.setChannel(this);
//        handler.setPeerDiscoveryMode(discoveryMode);
//
//        handler.activate();
//
//        eth = handler;
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
        node = new PeerConnection(nodeId, inetSocketAddress.getHostString(), remotePort);
        node.setNodeStatistics(nodeStatistics);
        node.setMessageQueue(msgQueue);
        nodeStatistics = nodeManager.getNodeStatistics(node);
    }

    public void initWithNode(byte[] nodeId) {
        initWithNode(nodeId, inetSocketAddress.getPort());
    }

    public Node getNode() {
        return node;
    }

    public PeerConnection getPeer() {
        return node;
    }

    public void onDisconnect() {
        isDisconnected = true;
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }

    public void onSyncDone(boolean done) {
//
//        if (done) {
//            eth.enableTransactions();
//        } else {
//            eth.disableTransactions();
//        }
//
//        eth.onSyncDone(done);
    }

    public boolean isDiscoveryMode() {
        return discoveryMode;
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

    public ChannelManager getChannelManager() {
        return channelManager;
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

    public boolean isIdle() {
      // TODO: use peer's status.
        return  true;
    }

    public String logSyncStats() {
        //TODO: return tron sync status here.
        return "tron sync stats";
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
