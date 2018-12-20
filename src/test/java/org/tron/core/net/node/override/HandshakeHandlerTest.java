package org.tron.core.net.node.override;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.server.HandshakeHandler;

@Component
@Scope("prototype")
public class HandshakeHandlerTest extends HandshakeHandler {

  private static final Logger logger = LoggerFactory.getLogger("HandshakeHandler");

  private Node node;

  public HandshakeHandlerTest() {
  }

  public HandshakeHandlerTest setNode(Node node) {
    this.node = node;
    return this;
  }

  @Override
  protected void sendHelloMsg(ChannelHandlerContext ctx, long time) {
    HelloMessage message = new HelloMessage(node, time,
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    ctx.writeAndFlush(message.getSendData());
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
    logger.info("Handshake Send to {}, {} ", ctx.channel().remoteAddress(), message);
  }

  public void close() {
    manager.closeAllStore();
  }
}
