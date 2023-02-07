package org.tron.core.net.service.handshake;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.relay.RelayService;
import org.tron.p2p.discover.Node;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class HandshakeService {

  @Autowired
  private RelayService relayService;

  @Autowired
  private ChainBaseManager chainBaseManager;

  public void startHandshake(PeerConnection peer) {
    sendHelloMessage(peer, peer.getChannel().getStartTime());
  }

  public void processHelloMessage(PeerConnection peer, HelloMessage msg) {
    if (peer.getHelloMessageReceive() != null) {
      logger.warn("Peer {} receive dup hello message", peer.getInetSocketAddress());
      peer.disconnect(ReasonCode.BAD_PROTOCOL);
      return;
    }

    TronNetService.getP2pService().updateNodeId(peer.getChannel(), msg.getFrom().getHexId());
    if (peer.isDisconnect()) {
      logger.info("Duplicate Peer {}", peer.getInetSocketAddress());
      peer.disconnect(ReasonCode.DUPLICATE_PEER);
      return;
    }

    if (!msg.valid()) {
      logger.warn("Peer {} invalid hello message parameters, "
                      + "GenesisBlockId: {}, SolidBlockId: {}, HeadBlockId: {}",
              peer.getInetSocketAddress(),
              ByteArray.toHexString(msg.getInstance().getGenesisBlockId().getHash().toByteArray()),
              ByteArray.toHexString(msg.getInstance().getSolidBlockId().getHash().toByteArray()),
              ByteArray.toHexString(msg.getInstance().getHeadBlockId().getHash().toByteArray()));
      peer.disconnect(ReasonCode.UNEXPECTED_IDENTITY);
      return;
    }

    peer.setAddress(msg.getHelloMessage().getAddress());

    if (!relayService.checkHelloMessage(msg, peer.getChannel())) {
      peer.disconnect(ReasonCode.UNEXPECTED_IDENTITY);
      return;
    }

    long headBlockNum = chainBaseManager.getHeadBlockNum();
    long lowestBlockNum = msg.getLowestBlockNum();
    if (lowestBlockNum > headBlockNum) {
      logger.info("Peer {} miss block, lowestBlockNum:{}, headBlockNum:{}",
              peer.getInetSocketAddress(), lowestBlockNum, headBlockNum);
      peer.disconnect(ReasonCode.LIGHT_NODE_SYNC_FAIL);
      return;
    }

    if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
      logger.info("Peer {} different p2p version, peer->{}, me->{}",
              peer.getInetSocketAddress(), msg.getVersion(),
              Args.getInstance().getNodeP2pVersion());
      peer.disconnect(ReasonCode.INCOMPATIBLE_VERSION);
      return;
    }

    if (!Arrays.equals(chainBaseManager.getGenesisBlockId().getBytes(),
            msg.getGenesisBlockId().getBytes())) {
      logger.info("Peer {} different genesis block, peer->{}, me->{}",
              peer.getInetSocketAddress(),
              msg.getGenesisBlockId().getString(),
              chainBaseManager.getGenesisBlockId().getString());
      peer.disconnect(ReasonCode.INCOMPATIBLE_CHAIN);
      return;
    }

    if (chainBaseManager.getSolidBlockId().getNum() >= msg.getSolidBlockId().getNum()
            && !chainBaseManager.containBlockInMainChain(msg.getSolidBlockId())) {
      logger.info("Peer {} different solid block, peer->{}, me->{}",
              peer.getInetSocketAddress(),
              msg.getSolidBlockId().getString(),
              chainBaseManager.getSolidBlockId().getString());
      peer.disconnect(ReasonCode.FORKED);
      return;
    }

    peer.setHelloMessageReceive(msg);

    peer.getChannel().updateLatency(
            System.currentTimeMillis() - peer.getChannel().getStartTime());
    PeerManager.sortPeers();
    peer.onConnect();
  }

  private void sendHelloMessage(PeerConnection peer, long time) {
    Node node = new Node(TronNetService.getP2pConfig().getNodeID(),
            TronNetService.getP2pConfig().getIp(),
            TronNetService.getP2pConfig().getPort());
    HelloMessage message = new HelloMessage(node, time, ChainBaseManager.getChainBaseManager());
    relayService.fillHelloMessage(message, peer.getChannel());
    peer.sendMessage(message);
    peer.setHelloMessageSend(message);
  }

}
