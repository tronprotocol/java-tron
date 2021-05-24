package org.tron.core.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.MessageQueue;
import org.tron.common.overlay.server.SyncPool;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.base.Param;
import org.tron.consensus.pbft.PbftManager;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.exception.P2pException;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.PBFTMessage.DataType;

@Component
@Scope("prototype")
public class PbftHandler extends SimpleChannelInboundHandler<PbftMessage> {

  public static final int ENABLE_BLOCK_NUM = 10;
  protected PeerConnection peer;

  private MessageQueue msgQueue;

  private static final Striped<Lock> striped = Striped.lazyWeakLock(1024);

  private static final Cache<String, Boolean> msgCache = CacheBuilder.newBuilder()
      .initialCapacity(3000).maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Autowired
  private PbftManager pbftManager;

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, PbftMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    if (Param.getInstance().getPbftInterface().isSyncing()) {
      return;
    }
    msg.analyzeSignature();
    String key = buildKey(msg);
    Lock lock = striped.get(key);
    try {
      lock.lock();
      if (msgCache.getIfPresent(key) != null || !verifyMsgEnable(msg)) {
        return;
      }
      if (!nextTimesVerifyMsgSign(msg) && !pbftManager.verifyMsgSign(msg)) {
        throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, msg.toString());
      }
      msgCache.put(key, true);
      forwardMessage(msg);
      pbftManager.doAction(msg);
    } finally {
      lock.unlock();
    }

  }

  private boolean verifyMsgEnable(PbftMessage msg) {
    long viewN = msg.getNumber();
    if (msg.getDataType() == DataType.BLOCK
        && viewN >= consensusDelegate.getLatestBlockHeaderNumber() + ENABLE_BLOCK_NUM) {
      return false;
    }
    if (msg.getDataType() == DataType.BLOCK
        && viewN < consensusDelegate.getLatestBlockHeaderNumber() - ENABLE_BLOCK_NUM) {
      return false;
    }
    if (msg.getDataType() == DataType.SRL && viewN > consensusDelegate
        .getNextMaintenanceTime() + chainBaseManager.getDynamicPropertiesStore()
        .getMaintenanceTimeInterval()) {
      return false;
    }
    if (msg.getDataType() == DataType.SRL && viewN < consensusDelegate
        .getNextMaintenanceTime() - chainBaseManager.getDynamicPropertiesStore()
        .getMaintenanceTimeInterval()) {
      return false;
    }
    return true;
  }

  private boolean nextTimesVerifyMsgSign(PbftMessage msg) {
    long viewN = msg.getNumber();
    if (msg.getDataType() == DataType.BLOCK
        && viewN > consensusDelegate.getLatestBlockHeaderNumber()) {
      return true;
    }
    if (msg.getDataType() == DataType.SRL && viewN > consensusDelegate
        .getNextMaintenanceTime()) {
      return true;
    }
    return false;
  }

  public void forwardMessage(PbftBaseMessage message) {
    if (syncPool == null) {
      return;
    }
    syncPool.getActivePeers().stream().filter(peerConnection -> !peerConnection.equals(peer))
        .forEach(peerConnection -> {
          peerConnection.sendMessage(message);
        });
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

  private String buildKey(PbftBaseMessage msg) {
    return msg.getKey() + msg.getPbftMessage().getRawData().getMsgType().toString();
  }

}