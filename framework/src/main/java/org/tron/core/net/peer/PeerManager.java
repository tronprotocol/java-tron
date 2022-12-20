package org.tron.core.net.peer;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.p2p.connection.Channel;

@Slf4j(topic = "net")
public class PeerManager {

  private static List<PeerConnection> peers = Collections.synchronizedList(new ArrayList<>());
  @Getter
  private static AtomicInteger passivePeersCount = new AtomicInteger(0);
  @Getter
  private static AtomicInteger activePeersCount = new AtomicInteger(0);

  private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  private static long DISCONNECTION_TIME_OUT = 60_000;

  public static void init() {
    executor.scheduleWithFixedDelay(() -> {
      try {
        check();
        logPeerStats();
      } catch (Throwable t) {
        logger.error("Exception in peer manager", t);
      }
    }, 30, 10, TimeUnit.SECONDS);
  }

  public static void close() {
    try {
      for (PeerConnection p : new ArrayList<>(peers)) {
        if (!p.isDisconnect()) {
          p.getChannel().close();
        }
      }
      executor.shutdownNow();
    } catch (Exception e) {
      logger.error("Peer manager shutdown failed", e);
    }
  }

  public static synchronized PeerConnection add(ApplicationContext ctx, Channel channel) {
    PeerConnection peerConnection = getPeerConnection(channel);
    if (peerConnection != null) {
      return null;
    }
    peerConnection = ctx.getBean(PeerConnection.class);
    peerConnection.setChannel(channel);
    peers.add(peerConnection);
    if (channel.isActive()) {
      activePeersCount.incrementAndGet();
    } else {
      passivePeersCount.incrementAndGet();
    }
    return peerConnection;
  }

  public static synchronized PeerConnection remove(Channel channel) {
    PeerConnection peerConnection = getPeerConnection(channel);
    if (peerConnection == null) {
      return null;
    }
    remove(peerConnection);
    return peerConnection;
  }

  private static void remove(PeerConnection peerConnection) {
    peers.remove(peerConnection);
    if (peerConnection.getChannel().isActive()) {
      activePeersCount.decrementAndGet();
    } else {
      passivePeersCount.decrementAndGet();
    }
  }

  public static synchronized void sortPeers() {
    peers.sort(Comparator.comparingDouble(c -> c.getChannel().getLatency()));
  }

  public static PeerConnection getPeerConnection(Channel channel) {
    for (PeerConnection peer : new ArrayList<>(peers)) {
      if (peer.getChannel().equals(channel)) {
        return peer;
      }
    }
    return null;
  }

  public static List<PeerConnection> getPeers() {
    List<PeerConnection> peers = Lists.newArrayList();
    for (PeerConnection peer : new ArrayList<>(PeerManager.peers)) {
      if (!peer.isDisconnect()) {
        peers.add(peer);
      }
    }
    return peers;
  }

  private static void check() {
    long now = System.currentTimeMillis();
    for (PeerConnection peer : new ArrayList<>(peers)) {
      long disconnectTime = peer.getChannel().getDisconnectTime();
      if (disconnectTime != 0 && now - disconnectTime > DISCONNECTION_TIME_OUT) {
        logger.warn("Notify disconnect peer {}.", peer.getInetSocketAddress());
        peers.remove(peer);
        if (peer.getChannel().isActive()) {
          activePeersCount.decrementAndGet();
        } else {
          passivePeersCount.decrementAndGet();
        }
        peer.onDisconnect();
      }
    }
  }

  private static synchronized void logPeerStats() {
    String str = String.format("\n\n============ Peer stats: all %d, active %d, passive %d\n\n",
            peers.size(), activePeersCount.get(), passivePeersCount.get());
    metric(peers.size(), MetricLabels.Gauge.PEERS_ALL);
    metric(activePeersCount.get(), MetricLabels.Gauge.PEERS_ACTIVE);
    metric(passivePeersCount.get(), MetricLabels.Gauge.PEERS_PASSIVE);
    StringBuilder sb = new StringBuilder(str);
    int valid = 0;
    for (PeerConnection peer : new ArrayList<>(peers)) {
      sb.append(peer.log());
      sb.append("\n");
      if (!(peer.isNeedSyncFromUs() || peer.isNeedSyncFromPeer())) {
        valid++;
      }
    }
    metric(valid, MetricLabels.Gauge.PEERS_VALID);
    logger.info(sb.toString());
  }

  private static void metric(double amt, String peerType) {
    Metrics.gaugeSet(MetricKeys.Gauge.PEERS, amt, peerType);
  }

}
