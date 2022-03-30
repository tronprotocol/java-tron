package org.tron.core.net.service;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Component
public class RelayService {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  private int maxFastForwardNum = Args.getInstance().getMaxFastForwardNum();

  private volatile long nextMaintenanceTime = -1;
  private volatile List<ByteString> witnesses;

  private List<ByteString> getSortedScheduleWitness() {
    long time = chainBaseManager.getDynamicPropertiesStore().getNextMaintenanceTime();
    if (time <= nextMaintenanceTime) {
      nextMaintenanceTime = time;
      return witnesses;
    }
    List<WitnessCapsule> l1 = new ArrayList<>();
    chainBaseManager.getWitnessScheduleStore().getActiveWitnesses().forEach(s -> {
      l1.add(chainBaseManager.getWitnessStore().get(s.toByteArray()));
    });
    l1.sort(Comparator.comparingLong(w -> -w.getVoteCount()));
    List<ByteString> l2 = new ArrayList<>();
    l1.forEach(w -> l2.add(w.getAddress()));
    nextMaintenanceTime = time;
    witnesses = l2;
    return witnesses;
  }

  private List<ByteString> getNextWitnesses(ByteString key, Integer count) {
    List<ByteString> l1 = getSortedScheduleWitness();
    int index = l1.indexOf(key);
    if (index < 0) {
      return l1;
    }
    List<ByteString> l2 = new ArrayList<>();
    for (; count > 0; count--) {
      l2.add(l1.get(++index % l1.size()));
    }
    return l2;
  }

  public void broadcast(BlockMessage msg) {
    List<ByteString> witnesses = getNextWitnesses(
            msg.getBlockCapsule().getWitnessAddress(), maxFastForwardNum);
    Item item = new Item(msg.getBlockId(), Protocol.Inventory.InventoryType.BLOCK);
    List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
            .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
            .filter(peer -> peer.getAdvInvReceive().getIfPresent(item) == null
                    && peer.getAdvInvSpread().getIfPresent(item) == null)
            .filter(peer -> peer.getAddress() != null && witnesses.contains(peer.getAddress()))
            .collect(Collectors.toList());

    peers.forEach(peer -> {
      peer.fastSend(msg);
      peer.getAdvInvSpread().put(item, System.currentTimeMillis());
      peer.setFastForwardBlock(msg.getBlockId());
    });
  }

}

