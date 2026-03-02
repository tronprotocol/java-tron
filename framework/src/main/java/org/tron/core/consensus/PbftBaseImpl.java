package org.tron.core.consensus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.PbftInterface;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;

@Component
public class PbftBaseImpl implements PbftInterface {

  @Autowired
  private Manager manager;

  @Override
  public boolean isSyncing() {
    List<PeerConnection> peers = PeerManager.getPeers();
    if (peers.isEmpty()) {
      return true;
    }
    AtomicBoolean result = new AtomicBoolean(false);
    peers.forEach(peerConnection -> {
      if (peerConnection.isNeedSyncFromPeer()) {
        result.set(true);
        return;
      }
    });
    return result.get();
  }

  @Override
  public void forwardMessage(PbftBaseMessage message) {
    List<PeerConnection> peers = PeerManager.getPeers();
    if (peers.isEmpty()) {
      return;
    }
    peers.forEach(peerConnection -> {
      peerConnection.sendMessage(message);
    });
  }

  @Override
  public BlockCapsule getBlock(long blockNum) throws BadItemException, ItemNotFoundException {
    return manager.getChainBaseManager().getBlockByNum(blockNum);
  }
}