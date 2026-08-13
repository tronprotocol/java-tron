package org.tron.p2p.discover.socket;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "net")
public class MessageHandler extends SimpleChannelInboundHandler<UdpEvent>
    implements Consumer<UdpEvent> {

  private Channel channel;

  private EventHandler eventHandler;

  public MessageHandler(NioDatagramChannel channel, EventHandler eventHandler) {
    this.channel = channel;
    this.eventHandler = eventHandler;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    eventHandler.channelActivated();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, UdpEvent udpEvent) {
    log.debug("Rcv udp msg type {}, len {} from {} ",
        udpEvent.getMessage().getType(),
        udpEvent.getMessage().getSendData().length,
        udpEvent.getAddress());
    eventHandler.handleEvent(udpEvent);
  }

  @Override
  public void accept(UdpEvent udpEvent) {
    log.debug("Send udp msg type {}, len {} to {} ",
        udpEvent.getMessage().getType(),
        udpEvent.getMessage().getSendData().length,
        udpEvent.getAddress());
    InetSocketAddress address = udpEvent.getAddress();
    sendPacket(udpEvent.getMessage().getSendData(), address);
  }

  void sendPacket(byte[] wire, InetSocketAddress address) {
    DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(wire), address);
    channel.write(packet);
    channel.flush();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.warn("Exception caught in udp message handler, {} {}",
        ctx.channel().remoteAddress(), cause.getMessage());
    ctx.close();
  }
}
