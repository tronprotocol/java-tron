package org.tron.p2p.discover.socket;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.discover.message.Message;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.utils.ByteArray;

@Slf4j(topic = "net")
public class P2pPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

  private static final int MAXSIZE = 2048;

  @Override
  public void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out)
      throws Exception {
    ByteBuf buf = packet.content();
    int length = buf.readableBytes();
    if (length <= 1 || length >= MAXSIZE) {
      log.warn("UDP rcv bad packet, from {} length = {}", ctx.channel().remoteAddress(), length);
      return;
    }
    byte[] encoded = new byte[length];
    buf.readBytes(encoded);
    try {
      UdpEvent event = new UdpEvent(Message.parse(encoded), packet.sender());
      out.add(event);
    } catch (P2pException pe) {
      if (pe.getType().equals(P2pException.TypeEnum.BAD_MESSAGE)) {
        log.error("Message validation failed, type {}, len {}, address {}", encoded[0],
            encoded.length, packet.sender());
      } else {
        log.info("Parse msg failed, type {}, len {}, address {}", encoded[0], encoded.length,
            packet.sender());
      }
    } catch (InvalidProtocolBufferException e) {
      log.warn("An exception occurred while parsing the message, type {}, len {}, address {}, "
              + "data {}, cause: {}", encoded[0], encoded.length, packet.sender(),
          ByteArray.toHexString(encoded), e.getMessage());
    } catch (Exception e) {
      log.error("An exception occurred while parsing the message, type {}, len {}, address {}, "
              + "data {}", encoded[0], encoded.length, packet.sender(),
          ByteArray.toHexString(encoded), e);
    }
  }
}
