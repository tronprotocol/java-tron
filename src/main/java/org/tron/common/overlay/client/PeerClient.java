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
package org.tron.common.overlay.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.TronChannelInitializer;
import org.tron.core.config.args.Args;
import org.tron.core.net.node.NodeImpl;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class creates the connection to an remote address using the Netty framework
 *
 * @see <a href="http://netty.io">http://netty.io</a>
 */
@Component
public class PeerClient {

    private static final Logger logger = LoggerFactory.getLogger("net");

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private NodeImpl node;

    private EventLoopGroup workerGroup;

    public PeerClient() {
        workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
            AtomicInteger cnt = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "TronJClientWorker-" + cnt.getAndIncrement());
            }
        });
    }

    public void connect(String host, int port, String remoteId) {
        connect(host, port, remoteId, false);
    }

    /**
     *  Connects to the node and returns only upon connection close
     */
    public void connect(String host, int port, String remoteId, boolean discoveryMode) {
        try {
            ChannelFuture f = connectAsync(host, port, remoteId, discoveryMode);

            f.sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();

            logger.debug("Connection is closed");

        } catch (Exception e) {
            if (discoveryMode) {
                logger.trace("Exception:", e);
            } else {
                if (e instanceof IOException) {
                    logger.info("PeerClient: Can't connect to " + host + ":" + port + " (" + e.getMessage() + ")");
                    logger.debug("PeerClient.connect(" + host + ":" + port + ") exception:", e);
                } else {
                    logger.error("Exception:", e);
                }
            }
        }
    }

    public ChannelFuture connectAsync(String host, int port, String remoteId, boolean discoveryMode) {

        logger.info("connect peer {} {} {}", host, port, remoteId);

        TronChannelInitializer tronChannelInitializer = ctx.getBean(TronChannelInitializer.class, remoteId);
        tronChannelInitializer.setPeerDiscoveryMode(discoveryMode);
        tronChannelInitializer.setNodeImpl(node);

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            Args.getInstance().getNodeConnectionTimeout());
        b.remoteAddress(host, port);

        b.handler(tronChannelInitializer);

        // Start the client.
        return b.connect();
    }

    public void close() {
        logger.info("Shutdown peerClient");
        workerGroup.shutdownGracefully();
        workerGroup.terminationFuture().syncUninterruptibly();
    }
}
