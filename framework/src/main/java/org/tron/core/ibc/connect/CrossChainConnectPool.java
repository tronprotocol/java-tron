package org.tron.core.ibc.connect;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.Channel;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

@Slf4j(topic = "net-cross")
@Component
public class CrossChainConnectPool {

  //key is chainId, value is connect list
  @Getter
  private Map<ByteString, List<PeerConnection>> crossChainConnectPool = new ConcurrentHashMap<>();

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  private PeerClient peerClient;
  @Autowired
  private NodeManager nodeManager;
  @Autowired
  private Manager manager;
  @Autowired
  private ChainBaseManager chainBaseManager;

  public void init() {
    Set<String> compare = new HashSet<>();
    Set<Node> dbCrossNode = new HashSet<>(Args.getInstance().getCrossChainConnect());
    dbCrossNode.forEach(n -> {
      if (!compare.contains(n.getHostPort())) {
        peerClient.connectAsync(nodeManager.getNodeHandler(n), false, true);
        compare.add(n.getHostPort());
      }
    });

    logExecutor.scheduleAtFixedRate(() -> {
      try {
        updateConnect();
        logActivePeers();
      } catch (Throwable t) {
        logger.error("CrossChainConnectPool Exception in sync worker", t);
      }
    }, 30, 10, TimeUnit.SECONDS);
  }

  public void onConnect(ByteString chainId, Channel channel) {
    synchronized (this) {
      PeerConnection peerConnection = (PeerConnection) channel;
      peerConnection.setChainId(chainId);
      if (!crossChainConnectPool.containsKey(chainId)) {
        crossChainConnectPool.put(chainId, new ArrayList<>());
      }
      if (!crossChainConnectPool.get(chainId).contains(peerConnection)) {
        crossChainConnectPool.get(chainId).add(peerConnection);
        //todo:sync the end block header

      }
    }
  }

  public void onDisconnect(Channel peer) {
    synchronized (this) {
      PeerConnection peerConnection = (PeerConnection) peer;
      for (ByteString key : crossChainConnectPool.keySet()) {
        if (crossChainConnectPool.get(key).contains(peerConnection)) {
          logger.info("disconnect the cross chain peer:{}", peer);
          crossChainConnectPool.get(key).remove(peerConnection);
          peerConnection.onDisconnect();
        }
      }
    }
  }

  public List<PeerConnection> getPeerConnect(ByteString chainId) {
    List<PeerConnection> peerConnectionList = crossChainConnectPool.get(chainId);
    return peerConnectionList == null ? Collections.emptyList() : peerConnectionList;
  }

  public void writeCrossNode() {
    synchronized (this) {
      Set<Node> nodeSet = new HashSet<>();
      crossChainConnectPool.values().forEach(peerConnections -> {
        peerConnections.forEach(peerConnection -> {
          nodeSet.add(peerConnection.getNode());
        });
      });
    }
  }

  private void updateConnect() {
    Set<Node> dbCrossNode = new HashSet<>(Args.getInstance().getCrossChainConnect());
    Set<String> connected = new HashSet<>();
    Set<String> disconnected = new HashSet<>();
    for (ByteString key : crossChainConnectPool.keySet()) {
      if (chainBaseManager.chainIsSelected(key)) {
        for (PeerConnection peer : crossChainConnectPool.get(key)) {
          connected.add(peer.getNode().getHostPort());
        }
      } else {
        for (PeerConnection peer : crossChainConnectPool.get(key)) {
          disconnected.add(peer.getNode().getHostPort());
          peer.disconnect(Protocol.ReasonCode.USER_REASON);
        }
      }
    }

    Set<String> compare = new HashSet<>();
    dbCrossNode.forEach(n -> {
      if (!compare.contains(n.getHostPort())
              && !connected.contains(n.getHostPort())
              && !disconnected.contains(n.getHostPort())) {
        peerClient.connectAsync(nodeManager.getNodeHandler(n), false, true);
        compare.add(n.getHostPort());
      }
    });
  }

  private void logActivePeers() {
    synchronized (this) {
      for (Entry<ByteString, List<PeerConnection>> entry : crossChainConnectPool.entrySet()) {
        String str = String
            .format("\n\n============ Cross Chain %s Peer stats: all %d\n\n",
                ByteArray.toHexString(entry.getKey().toByteArray()), entry.getValue().size());
        StringBuilder sb = new StringBuilder(str);
        for (PeerConnection peer : entry.getValue()) {
          sb.append(peer.log()).append('\n');
        }
        sb.append("===========================================================").append('\n');
        logger.info(sb.toString());
      }
    }
  }

}
