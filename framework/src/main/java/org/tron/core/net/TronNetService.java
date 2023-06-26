package org.tron.core.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.peer.PeerStatusCheck;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.nodepersist.NodePersistService;
import org.tron.core.net.service.relay.RelayService;
import org.tron.core.net.service.statistics.TronStatsManager;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
@Component
public class TronNetService {

  @Getter
  private static P2pConfig p2pConfig;

  @Getter
  private static P2pService p2pService = new P2pService();

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private PeerStatusCheck peerStatusCheck;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Autowired
  private FetchBlockService fetchBlockService;

  private CommonParameter parameter = Args.getInstance();

  @Autowired
  private P2pEventHandlerImpl p2pEventHandler;

  @Autowired
  private NodePersistService nodePersistService;

  @Autowired
  private TronStatsManager tronStatsManager;

  @Autowired
  private RelayService relayService;

  @Autowired
  private EffectiveCheckService effectiveCheckService;

  private volatile boolean init;

  private static void setP2pConfig(P2pConfig config) {
    TronNetService.p2pConfig = config;
  }

  public void start() {
    try {
      init = true;
      setP2pConfig(getConfig());
      p2pService.start(p2pConfig);
      p2pService.register(p2pEventHandler);
      advService.init();
      syncService.init();
      peerStatusCheck.init();
      transactionsMsgHandler.init();
      fetchBlockService.init();
      nodePersistService.init();
      tronStatsManager.init();
      PeerManager.init();
      relayService.init();
      effectiveCheckService.init();
      logger.info("Net service start successfully");
    } catch (Exception e) {
      logger.error("Net service start failed", e);
    }
  }

  public void close() {
    if (!init) {
      return;
    }
    PeerManager.close();
    tronStatsManager.close();
    nodePersistService.close();
    advService.close();
    syncService.close();
    peerStatusCheck.close();
    transactionsMsgHandler.close();
    fetchBlockService.close();
    effectiveCheckService.close();
    p2pService.close();
    relayService.close();
    logger.info("Net service closed successfully");
  }

  public static List<PeerConnection> getPeers() {
    return PeerManager.getPeers();
  }

  public void broadcast(Message msg) {
    advService.broadcast(msg);
  }

  public int fastBroadcastTransaction(TransactionMessage msg) {
    return advService.fastBroadcastTransaction(msg);
  }

  public static boolean hasIpv4Stack(Set<String> ipSet) {
    for (String ip : ipSet) {
      InetAddress inetAddress;
      try {
        inetAddress = InetAddress.getByName(ip);
      } catch (UnknownHostException e) {
        logger.warn("Get inet address failed, {}", e.getMessage());
        continue;
      }
      if (inetAddress instanceof Inet4Address) {
        return true;
      }
    }
    return false;
  }

  private P2pConfig getConfig() {
    P2pConfig config = new P2pConfig();
    return updateConfig(config);
  }

  private P2pConfig updateConfig(P2pConfig config) {
    List<InetSocketAddress> seeds = parameter.getSeedNode().getAddressList();
    seeds.addAll(nodePersistService.dbRead());
    logger.debug("Seed InetSocketAddress: {}", seeds);
    config.getSeedNodes().addAll(seeds);
    config.getActiveNodes().addAll(parameter.getActiveNodes());
    config.getTrustNodes().addAll(parameter.getPassiveNodes());
    config.getActiveNodes().forEach(n -> config.getTrustNodes().add(n.getAddress()));
    parameter.getFastForwardNodes().forEach(f -> config.getTrustNodes().add(f.getAddress()));
    int maxConnections = parameter.getMaxConnections();
    int minConnections = parameter.getMinConnections();
    int minActiveConnections = parameter.getMinActiveConnections();
    if (minConnections > maxConnections) {
      minConnections = maxConnections;
    }
    if (minActiveConnections > minConnections) {
      minActiveConnections = minConnections;
    }
    config.setMaxConnections(maxConnections);
    config.setMinConnections(minConnections);
    config.setMinActiveConnections(minActiveConnections);

    config.setMaxConnectionsWithSameIp(parameter.getMaxConnectionsWithSameIp());
    config.setPort(parameter.getNodeListenPort());
    config.setNetworkId(parameter.getNodeP2pVersion());
    config.setDisconnectionPolicyEnable(parameter.isOpenFullTcpDisconnect());
    config.setNodeDetectEnable(parameter.isNodeDetectEnable());
    config.setDiscoverEnable(parameter.isNodeDiscoveryEnable());
    if (StringUtils.isEmpty(config.getIp()) && hasIpv4Stack(NetUtil.getAllLocalAddress())) {
      config.setIp(parameter.getNodeExternalIp());
    }
    if (StringUtils.isNotEmpty(config.getIpv6())) {
      config.getActiveNodes().remove(new InetSocketAddress(config.getIpv6(), config.getPort()));
    }
    if (!parameter.nodeEnableIpv6) {
      config.setIpv6(null);
    }
    logger.info("Local ipv4: {}", config.getIp());
    logger.info("Local ipv6: {}", config.getIpv6());
    config.setTreeUrls(parameter.getDnsTreeUrls());
    if (Objects.nonNull(parameter.getDnsPublishConfig())) {
      config.setPublishConfig(parameter.getDnsPublishConfig());
    }
    return config;
  }
}