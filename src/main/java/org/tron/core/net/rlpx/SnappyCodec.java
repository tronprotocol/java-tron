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
package org.tron.core.net.rlpx;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.List;

/**
 * Snappy compression codec. <br>
 *
 * Check <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-706.md">EIP-706</a> for details
 *
 * @author Mikhail Kalinin
 * @since 31.10.2017
 */
public class SnappyCodec extends MessageToMessageCodec<FrameCodec.Frame, FrameCodec.Frame> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private final static int SNAPPY_P2P_VERSION = 5;
    private final static int MAX_SIZE = 16 * 1024 * 1024; // 16 mb

    Channel channel;

    public SnappyCodec(Channel channel) {
        this.channel = channel;
    }

    public static boolean isSupported(int p2pVersion) {
        return p2pVersion >= SNAPPY_P2P_VERSION;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FrameCodec.Frame msg, List<Object> out) throws Exception {

        // stay consistent with decoding party
        if (msg.size > MAX_SIZE) {
            logger.info("{}: outgoing frame size exceeds the limit ({} bytes), disconnect", channel, msg.size);
            channel.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.USELESS_PEER);
            channel.disconnect(ReasonCode.USELESS_PEER);
            return;
        }

        byte[] in = new byte[msg.size];
        msg.payload.read(in);

        byte[] compressed = Snappy.rawCompress(in, in.length);

        out.add(new FrameCodec.Frame((int) msg.type, compressed));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FrameCodec.Frame msg, List<Object> out) throws Exception {

        byte[] in = new byte[msg.size];
        msg.payload.read(in);

        long uncompressedLength = Snappy.uncompressedLength(in) & 0xFFFFFFFFL;
        if (uncompressedLength > MAX_SIZE) {
            logger.info("{}: uncompressed frame size exceeds the limit ({} bytes), drop the peer", channel, uncompressedLength);
            channel.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.BAD_PROTOCOL);
            channel.disconnect(ReasonCode.BAD_PROTOCOL);
            return;
        }

        byte[] uncompressed = new byte[(int) uncompressedLength];
        try {
            Snappy.rawUncompress(in, 0, in.length, uncompressed, 0);
        } catch (IOException e) {
            String detailMessage = e.getMessage();
            // 5 - error code for framed snappy
            if (detailMessage.startsWith("FAILED_TO_UNCOMPRESS") && detailMessage.contains("5")) {
                logger.info("{}: Snappy frames are not allowed in DEVp2p protocol, drop the peer", channel);
                channel.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.BAD_PROTOCOL);
                channel.disconnect(ReasonCode.BAD_PROTOCOL);
                return;
            } else {
                throw e;
            }
        }

        out.add(new FrameCodec.Frame((int) msg.type, uncompressed));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (channel.isDiscoveryMode()) {
            logger.trace("SnappyCodec failed: " + cause);
        } else {
            if (cause instanceof IOException) {
                logger.debug("SnappyCodec failed: " + ctx.channel().remoteAddress() + ": " + cause);
            } else {
                logger.warn("SnappyCodec failed: ", cause);
            }
        }
        ctx.close();
    }
}
