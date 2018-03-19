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
package org.tron.common.overlay.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net.rlpx.NettyByteToMessageCodec;

import java.io.IOException;
import java.util.List;

/**
 * The Netty handler responsible for decrypting/encrypting RLPx frames
 * with the FrameCodec crated during HandshakeHandler initial work
 *
 * Created by Anton Nashatyrev on 15.10.2015.
 */
public class FrameCodecHandler extends NettyByteToMessageCodec<FrameCodec.Frame> {
    private static final Logger loggerWire = LoggerFactory.getLogger("wire");
    private static final Logger loggerNet = LoggerFactory.getLogger("net");

    public FrameCodec frameCodec;
    public Channel channel;

    public FrameCodecHandler(FrameCodec frameCodec, Channel channel) {
        this.frameCodec = frameCodec;
        this.channel = channel;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        if (in.readableBytes() == 0) {
            loggerWire.trace("in.readableBytes() == 0");
            return;
        }

        loggerWire.trace("Decoding frame (" + in.readableBytes() + " bytes)");
        List<FrameCodec.Frame> frames = frameCodec.readFrames(in);


        // Check if a full frame was available.  If not, we'll try later when more bytes come in.
        if (frames == null || frames.isEmpty()) return;

        for (int i = 0; i < frames.size(); i++) {
            FrameCodec.Frame frame = frames.get(i);

            channel.getNodeStatistics().rlpxInMessages.add();
        }

        out.addAll(frames);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FrameCodec.Frame frame, ByteBuf out) throws Exception {

        frameCodec.writeFrame(frame, out);

        channel.getNodeStatistics().rlpxOutMessages.add();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (channel.isDiscoveryMode()) {
            loggerNet.trace("FrameCodec failed: " + cause);
        } else {
            if (cause instanceof IOException) {
                loggerNet.debug("FrameCodec failed: " + ctx.channel().remoteAddress() + ": " + cause);
            } else {
                loggerNet.warn("FrameCodec failed: ", cause);
            }
        }
        ctx.close();
    }
}
