package org.tron.common.overlay.discover;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.overlay.discover.message.Message;

import java.util.List;

public class PacketDecoder extends MessageToMessageDecoder<DatagramPacket> {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    @Override
    public void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        ByteBuf buf = packet.content();
        byte[] encoded = new byte[buf.readableBytes()];
        try {
            DiscoveryEvent event = new DiscoveryEvent(Message.parse(encoded), packet.sender());
            out.add(event);
        } catch (Exception e) {
            throw new RuntimeException("Exception processing inbound message from " + ctx.channel().remoteAddress() + ": " + Hex.toHexString(encoded), e);
        }
    }
}
