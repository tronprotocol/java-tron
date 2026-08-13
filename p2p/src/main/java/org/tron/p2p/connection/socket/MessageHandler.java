package org.tron.p2p.connection.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.protos.Connect.DisconnectReason;
import org.tron.p2p.utils.ByteArray;

@Slf4j(topic = "net")
public class MessageHandler extends ByteToMessageDecoder {

  private final Channel channel;

  public MessageHandler(Channel channel) {
    this.channel = channel;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    log.debug("Channel active, {}", ctx.channel().remoteAddress());
    channel.setChannelHandlerContext(ctx);
    if (channel.isActive()) {
      if (channel.isDiscoveryMode()) {
        channel.send(new StatusMessage());
      } else {
        ChannelManager.getHandshakeService().startHandshake(channel);
      }
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
    byte[] data = new byte[buffer.readableBytes()];
    buffer.readBytes(data);
    try {
      if (channel.isFinishHandshake()) {
        data = UpgradeController.decodeReceiveData(channel.getVersion(), data);
      }
      ChannelManager.processMessage(channel, data);
    } catch (Exception e) {
      if (e instanceof P2pException) {
        P2pException pe = (P2pException) e;
        DisconnectReason disconnectReason;
        switch (pe.getType()) {
          case EMPTY_MESSAGE:
            disconnectReason = DisconnectReason.EMPTY_MESSAGE;
            break;
          case BAD_PROTOCOL:
            disconnectReason = DisconnectReason.BAD_PROTOCOL;
            break;
          case NO_SUCH_MESSAGE:
            disconnectReason = DisconnectReason.NO_SUCH_MESSAGE;
            break;
          case BAD_MESSAGE:
          case PARSE_MESSAGE_FAILED:
          case MESSAGE_WITH_WRONG_LENGTH:
          case TYPE_ALREADY_REGISTERED:
            disconnectReason = DisconnectReason.BAD_MESSAGE;
            break;
          default:
            disconnectReason = DisconnectReason.UNKNOWN;
        }
        channel.send(new P2pDisconnectMessage(disconnectReason));
      }
      channel.processException(e);
    } catch (Throwable t) {
      log.error("Decode message from {} failed, message:{}", channel.getInetSocketAddress(),
          ByteArray.toHexString(data));
      throw t;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    channel.processException(cause);
  }

}