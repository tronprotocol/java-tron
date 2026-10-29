package org.tron.p2p.connection.business.handshake;

import static org.tron.p2p.connection.ChannelManager.getDisconnectReason;
import static org.tron.p2p.connection.ChannelManager.logDisconnectReason;

import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.MessageProcess;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.socket.OutboundHandshakeTimeoutHandler;
import org.tron.p2p.connection.socket.PendingInboundConnectionHandler;
import org.tron.p2p.protos.Connect.DisconnectReason;

@Slf4j(topic = "net")
public class HandshakeService implements MessageProcess {

  private final int networkId = Parameter.p2pConfig.getNetworkId();

  public void startHandshake(Channel channel) {
    sendHelloMsg(channel, DisconnectCode.NORMAL, channel.getStartTime());
  }

  @Override
  public void processMessage(Channel channel, Message message) {
    if (channel.isDisconnect()) {
      return;
    }
    HelloMessage msg = (HelloMessage) message;

    if (channel.isFinishHandshake()) {
      logger.warn("Close channel {}, handshake is finished", channel.getInetSocketAddress());
      channel.send(new P2pDisconnectMessage(DisconnectReason.DUP_HANDSHAKE));
      channel.close();
      return;
    }

    if (msg.getNetworkId() != networkId) {
      logger.info("Peer {} different network id, peer->{}, me->{}",
          channel.getInetSocketAddress(), msg.getNetworkId(), networkId);
      if (!channel.isOutbound()) {
        sendHelloMsg(channel, DisconnectCode.DIFFERENT_VERSION, msg.getTimestamp());
      }
      logDisconnectReason(channel, DisconnectReason.DIFFERENT_VERSION);
      channel.close();
      return;
    }

    if (channel.isOutbound() && msg.getCode() != DisconnectCode.NORMAL.getValue()) {
      DisconnectCode disconnectCode = DisconnectCode.forNumber(msg.getCode());
      logger.info("Handshake failed {}, code: {}, reason: {}, networkId: {}, version: {}",
          channel.getInetSocketAddress(), msg.getCode(), disconnectCode.name(),
          msg.getNetworkId(), msg.getVersion());
      logDisconnectReason(channel, getDisconnectReason(disconnectCode));
      channel.close();
      return;
    }

    channel.setHelloMessage(msg);
    DisconnectCode code = finishHandshake(channel, msg);
    if (code != DisconnectCode.NORMAL) {
      if (!channel.isOutbound() && code != DisconnectCode.UNKNOWN) {
        sendHelloMsg(channel, code, msg.getTimestamp());
      }
      logDisconnectReason(channel, getDisconnectReason(code));
      channel.close();
      return;
    }
    Parameter.handlerList.forEach(h -> h.onConnect(channel));
  }

  private DisconnectCode finishHandshake(Channel channel, HelloMessage msg) {
    // Keep admission checks, handshake completion and registration in one critical section.
    synchronized (ChannelManager.class) {
      DisconnectCode code = ChannelManager.checkPeer(channel);
      if (code != DisconnectCode.NORMAL) {
        return code;
      }
      if (!channel.isOutbound()) {
        // Hello must be sent before enabling the negotiated message compression.
        sendHelloMsg(channel, DisconnectCode.NORMAL, msg.getTimestamp());
      }
      if (channel.isDisconnect() || !channel.getCtx().channel().isOpen()) {
        return DisconnectCode.UNKNOWN;
      }
      if (!PendingInboundConnectionHandler.handshakeCompleted(channel.getCtx().channel())) {
        return DisconnectCode.UNKNOWN;
      }
      if (!OutboundHandshakeTimeoutHandler.handshakeCompleted(channel.getCtx().channel())) {
        return DisconnectCode.UNKNOWN;
      }
      channel.setFinishHandshake(true);
      channel.updateAvgLatency(System.currentTimeMillis() - channel.getStartTime());
      ChannelManager.addPeer(channel);
      return DisconnectCode.NORMAL;
    }
  }

  private void sendHelloMsg(Channel channel, DisconnectCode code, long time) {
    HelloMessage helloMessage = new HelloMessage(code, time);
    channel.send(helloMessage);
  }

}
