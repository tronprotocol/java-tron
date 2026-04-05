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
import org.tron.p2p.protos.Connect.DisconnectReason;

@Slf4j(topic = "net")
public class HandshakeService implements MessageProcess {

  private final int networkId = Parameter.p2pConfig.getNetworkId();

  public void startHandshake(Channel channel) {
    sendHelloMsg(channel, DisconnectCode.NORMAL, channel.getStartTime());
  }

  @Override
  public void processMessage(Channel channel, Message message) {
    HelloMessage msg = (HelloMessage) message;

    if (channel.isFinishHandshake()) {
      log.warn("Close channel {}, handshake is finished", channel.getInetSocketAddress());
      channel.send(new P2pDisconnectMessage(DisconnectReason.DUP_HANDSHAKE));
      channel.close();
      return;
    }

    channel.setHelloMessage(msg);

    DisconnectCode code = ChannelManager.processPeer(channel);
    if (code != DisconnectCode.NORMAL) {
      if (!channel.isActive()) {
        sendHelloMsg(channel, code, msg.getTimestamp());
      }
      logDisconnectReason(channel, getDisconnectReason(code));
      channel.close();
      return;
    }

    ChannelManager.updateNodeId(channel, msg.getFrom().getHexId());
    if (channel.isDisconnect()) {
      return;
    }

    if (channel.isActive()) {
      if (msg.getCode() != DisconnectCode.NORMAL.getValue()
          || (msg.getNetworkId() != networkId && msg.getVersion() != networkId)) {
        DisconnectCode disconnectCode = DisconnectCode.forNumber(msg.getCode());
        //v0.1 have version, v0.2 both have version and networkId
        log.info("Handshake failed {}, code: {}, reason: {}, networkId: {}, version: {}",
            channel.getInetSocketAddress(),
            msg.getCode(),
            disconnectCode.name(),
            msg.getNetworkId(),
            msg.getVersion());
        logDisconnectReason(channel, getDisconnectReason(disconnectCode));
        channel.close();
        return;
      }
    } else {

      if (msg.getNetworkId() != networkId) {
        log.info("Peer {} different network id, peer->{}, me->{}",
            channel.getInetSocketAddress(), msg.getNetworkId(), networkId);
        sendHelloMsg(channel, DisconnectCode.DIFFERENT_VERSION, msg.getTimestamp());
        logDisconnectReason(channel, DisconnectReason.DIFFERENT_VERSION);
        channel.close();
        return;
      }
      sendHelloMsg(channel, DisconnectCode.NORMAL, msg.getTimestamp());
    }
    channel.setFinishHandshake(true);
    channel.updateAvgLatency(System.currentTimeMillis() - channel.getStartTime());
    Parameter.handlerList.forEach(h -> h.onConnect(channel));
  }

  private void sendHelloMsg(Channel channel, DisconnectCode code, long time) {
    HelloMessage helloMessage = new HelloMessage(code, time);
    channel.send(helloMessage);
  }

}
