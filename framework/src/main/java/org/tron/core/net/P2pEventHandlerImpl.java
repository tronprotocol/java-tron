package org.tron.core.net;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.PbftMessageFactory;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.TronMessageFactory;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.FetchInvDataMsgHandler;
import org.tron.core.net.messagehandler.InventoryMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.messagehandler.PbftMsgHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHandler;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.core.net.service.keepalive.KeepAliveService;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Component
public class P2pEventHandlerImpl extends P2pEventHandler {

  private static final String TAG = "~";
  private static final int DURATION_STEP = 50;
  @Getter
  private static AtomicInteger passivePeersCount = new AtomicInteger(0);
  @Getter
  private final AtomicInteger activePeersCount = new AtomicInteger(0);

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private SyncBlockChainMsgHandler syncBlockChainMsgHandler;

  @Autowired
  private ChainInventoryMsgHandler chainInventoryMsgHandler;

  @Autowired
  private InventoryMsgHandler inventoryMsgHandler;

  @Autowired
  private FetchInvDataMsgHandler fetchInvDataMsgHandler;

  @Autowired
  private BlockMsgHandler blockMsgHandler;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Autowired
  private PbftDataSyncHandler pbftDataSyncHandler;

  @Autowired
  private HandshakeService handshakeService;

  @Autowired
  private PbftMsgHandler pbftMsgHandler;

  @Autowired
  private KeepAliveService keepAliveService;

  private byte MESSAGE_MAX_TYPE = 127;

  public P2pEventHandlerImpl() {
    Set<Byte> set = new HashSet<>();
    for (byte i = 0; i < MESSAGE_MAX_TYPE; i++) {
      set.add(i);
    }
    messageTypes = set;
  }

  @Override
  public synchronized void onConnect(Channel channel) {
    PeerConnection peerConnection = PeerManager.add(ctx, channel);
    if (peerConnection != null) {
      handshakeService.startHandshake(peerConnection);
    }
  }

  @Override
  public synchronized void onDisconnect(Channel channel) {
    PeerConnection peerConnection = PeerManager.remove(channel);
    if (peerConnection != null) {
      peerConnection.onDisconnect();
    }
  }

  @Override
  public void onMessage(Channel c, byte[] data) {
    PeerConnection peerConnection = PeerManager.getPeerConnection(c);
    if (peerConnection == null) {
      logger.warn("Receive msg from unknown peer {}", c.getInetSocketAddress());
      return;
    }

    if (MessageTypes.PBFT_MSG.asByte() == data[0]) {
      PbftMessage message = null;
      try {
        message = (PbftMessage) PbftMessageFactory.create(data);
        pbftMsgHandler.processMessage(peerConnection, message);
      } catch (Exception e) {
        logger.warn("PBFT Message from {} process failed, {}",
                peerConnection.getInetSocketAddress(), message, e.getMessage());
        peerConnection.disconnect(Protocol.ReasonCode.BAD_PROTOCOL);
      }
      return;
    }

    processMessage(peerConnection, data);
  }

  private void processMessage(PeerConnection peer, byte[] data) {
    long startTime = System.currentTimeMillis();
    TronMessage msg = null;
    MessageTypes type = null;
    try {
      msg = TronMessageFactory.create(data);
      type = msg.getType();
      peer.getPeerStatistics().messageStatistics.addTcpInMessage(msg);
      if (PeerConnection.needToLog(msg)) {
        logger.info("Receive message from  peer: {}, {}", peer.getInetSocketAddress(), msg);
      }
      switch (type) {
        case P2P_PING:
        case P2P_PONG:
          keepAliveService.processMessage(peer, msg);
          break;
        case P2P_HELLO:
          handshakeService.processHelloMessage(peer, (HelloMessage) msg);
          break;
        case P2P_DISCONNECT:
          peer.getChannel().close();
          peer.getNodeStatistics()
                  .nodeDisconnectedRemote(((DisconnectMessage)msg).getReason());
          break;
        case SYNC_BLOCK_CHAIN:
          syncBlockChainMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK_CHAIN_INVENTORY:
          chainInventoryMsgHandler.processMessage(peer, msg);
          break;
        case INVENTORY:
          inventoryMsgHandler.processMessage(peer, msg);
          break;
        case FETCH_INV_DATA:
          fetchInvDataMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK:
          blockMsgHandler.processMessage(peer, msg);
          break;
        case TRXS:
          transactionsMsgHandler.processMessage(peer, msg);
          break;
        case PBFT_COMMIT_MSG:
          pbftDataSyncHandler.processMessage(peer, msg);
          break;
        default:
          throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, msg.getType().toString());
      }
    } catch (Exception e) {
      processException(peer, msg, e);
    } finally {
      long costs = System.currentTimeMillis() - startTime;
      if (costs > 50) {
        logger.info("Message processing costs {} ms, peer: {}, type: {}, time tag: {}",
                costs, peer.getInetSocketAddress(), type, getTimeTag(costs));
        if (type != null) {
          Metrics.histogramObserve(MetricKeys.Histogram.MESSAGE_PROCESS_LATENCY,
                  costs / Metrics.MILLISECONDS_PER_SECOND, type.name());
        }
      }
    }
  }

  private void processException(PeerConnection peer, TronMessage msg, Exception ex) {
    Protocol.ReasonCode code;

    if (ex instanceof P2pException) {
      P2pException.TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_TRX:
          code = Protocol.ReasonCode.BAD_TX;
          break;
        case BAD_BLOCK:
          code = Protocol.ReasonCode.BAD_BLOCK;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = Protocol.ReasonCode.BAD_PROTOCOL;
          break;
        case SYNC_FAILED:
          code = Protocol.ReasonCode.SYNC_FAIL;
          break;
        case UNLINK_BLOCK:
          code = Protocol.ReasonCode.UNLINKABLE;
          break;
        case DB_ITEM_NOT_FOUND:
          code = Protocol.ReasonCode.FETCH_FAIL;
          break;
        default:
          code = Protocol.ReasonCode.UNKNOWN;
          break;
      }
      logger.warn("Message from {} process failed, {} \n type: {}, detail: {}",
              peer.getInetSocketAddress(), msg, type, ex.getMessage());
    } else {
      code = Protocol.ReasonCode.UNKNOWN;
      logger.warn("Message from {} process failed, {}",
              peer.getInetSocketAddress(), msg, ex);
    }

    peer.disconnect(code);
  }

  private String getTimeTag(long duration) {
    StringBuilder tag = new StringBuilder(TAG);
    long tagCount = duration / DURATION_STEP;
    for (; tagCount > 0; tagCount--) {
      tag.append(TAG);
    }
    return tag.toString();
  }

}
