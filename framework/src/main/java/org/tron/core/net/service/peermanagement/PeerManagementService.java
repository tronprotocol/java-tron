package org.tron.core.net.service.peermanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.CommonStore;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.peer.ActivePeerInfo;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
@Component
public class PeerManagementService {

  static final int MAX_BLOCKED_IPS = 10_000;
  private static final byte[] DB_KEY_BLOCKED_IPS =
      "blocked-ips".getBytes(StandardCharsets.UTF_8);
  private static final int MAX_IP_LENGTH = 45;
  private static final int MAX_ENDPOINT_LENGTH = 64;
  private static final int MAX_BLOCKED_IPS_VALUE_BYTES = 1024 * 1024;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String DYNAMIC_CONFIG_ERROR_MESSAGE =
      "Peer modification is unavailable while node.dynamicConfig.enable is true; "
          + "update node.active in the configuration file instead.";

  @Autowired
  private CommonStore commonStore;

  private final Object managementLock = new Object();
  private volatile Map<InetAddress, Long> blockedIps = Collections.emptyMap();
  private volatile P2pService p2pService;

  public void configure(P2pConfig p2pConfig) {
    this.p2pService = null;
    Objects.requireNonNull(p2pConfig, "p2pConfig must not be null");
    Map<InetAddress, Long> loadedBlockedIps = loadBlockedIps();
    blockedIps = immutableCopy(loadedBlockedIps);
    p2pConfig.setBlockedIps(new HashSet<>(loadedBlockedIps.keySet()));
  }

  public void init(P2pService p2pService) {
    this.p2pService = Objects.requireNonNull(p2pService, "p2pService must not be null");
  }

  public void close() {
    p2pService = null;
  }

  /**
   * Returns normalized IPs and their record creation times in Unix epoch milliseconds.
   */
  public List<BlockedIpInfo> listBlockedIps() {
    return toBlockedIpInfos(blockedIps);
  }

  /**
   * Returns current peer connections with the same core statistics used by peer logging.
   * Unlike {@code logPeerStats()}, this method excludes peers waiting for delayed disconnect
   * cleanup and derives all counts from the same filtered snapshot.
   */
  public ActivePeerListResult listActivePeers() {
    List<PeerConnection> peers = PeerManager.getPeers();
    List<ActivePeerInfo> peerInfos = new ArrayList<>(peers.size());
    int activeCount = 0;
    int effectiveCount = 0;
    for (PeerConnection peer : peers) {
      ActivePeerInfo peerInfo = peer.getActivePeerInfo();
      if (peer.getChannel().isActive()) {
        activeCount++;
      }
      if (!peerInfo.isNeedSyncFromPeer() && !peerInfo.isNeedSyncFromUs()) {
        effectiveCount++;
      }
      peerInfos.add(peerInfo);
    }
    return new ActivePeerListResult(peers.size(), activeCount, peers.size() - activeCount,
        effectiveCount, peerInfos);
  }

  /**
   * Adds an endpoint to libp2p's active-node set without adding it to the trusted-node set.
   */
  public PeerOperationResult addPeer(String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    if (Args.getInstance().isDynamicConfigEnable()) {
      return operationFailed(DYNAMIC_CONFIG_ERROR_MESSAGE);
    }
    InetSocketAddress address = parseEndpoint(endpoint);
    synchronized (managementLock) {
      P2pService p2pService = requireP2pService();
      if (blockedIps.containsKey(address.getAddress())) {
        return operationFailed(
            "Cannot add active node because IP " + address.getAddress().getHostAddress()
                + " is manually blocked");
      }
      try {
        boolean changed = p2pService.addActiveNode(address);
        logger.info("Admin add active node {}, changed {}", address, changed);
        return operationSucceeded(changed, 0);
      } catch (IllegalArgumentException e) {
        throw new JsonRpcInvalidParamsException("Invalid peer endpoint", e);
      } catch (RuntimeException e) {
        logger.error("Failed to add active node", e);
        throw new JsonRpcInternalException("Failed to add active node", e);
      }
    }
  }

  /**
   * Removes an endpoint from the active-node set and disconnects its current connection.
   */
  public PeerOperationResult removePeer(String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    if (Args.getInstance().isDynamicConfigEnable()) {
      return operationFailed(DYNAMIC_CONFIG_ERROR_MESSAGE);
    }
    InetSocketAddress address = parseEndpoint(endpoint);
    synchronized (managementLock) {
      P2pService p2pService = requireP2pService();
      try {
        boolean changed = p2pService.removeActiveNode(address);
        int disconnectedCount = p2pService.disconnect(address);
        logger.info("Admin remove active node {}, changed {}, disconnected channels {}",
            address, changed, disconnectedCount);
        return operationSucceeded(changed, disconnectedCount);
      } catch (IllegalArgumentException e) {
        throw new JsonRpcInvalidParamsException("Invalid peer endpoint", e);
      } catch (RuntimeException e) {
        logger.error("Failed to remove active node", e);
        throw new JsonRpcInternalException("Failed to remove active node", e);
      }
    }
  }

  /**
   * Disconnects the exact endpoint without changing the configured active-node set.
   */
  public PeerOperationResult disconnectPeer(String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    InetSocketAddress address = parseEndpoint(endpoint);
    P2pService p2pService = requireP2pService();
    try {
      int disconnectedCount = p2pService.disconnect(address);
      logger.info("Admin disconnect peer {}, disconnected channels {}",
          address, disconnectedCount);
      return operationSucceeded(false, disconnectedCount);
    } catch (IllegalArgumentException e) {
      throw new JsonRpcInvalidParamsException("Invalid peer endpoint", e);
    } catch (RuntimeException e) {
      logger.error("Failed to disconnect peer", e);
      throw new JsonRpcInternalException("Failed to disconnect peer", e);
    }
  }

  /**
   * Persists an IP and its creation time, and disconnects its existing TCP connections.
   * Repeated blocking preserves the original timestamp until the IP is unblocked.
   */
  public PeerOperationResult blockIp(String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    InetAddress address = parseIp(ip);
    synchronized (managementLock) {
      P2pService p2pService = requireP2pService();
      if (blockedIps.containsKey(address)) {
        return operationSucceeded(false, 0);
      }
      if (blockedIps.size() >= MAX_BLOCKED_IPS) {
        return operationFailed(
            "Blocked IP limit of " + MAX_BLOCKED_IPS + " has been reached");
      }
      Map<InetAddress, Long> nextBlockedIps = new HashMap<>(blockedIps);
      nextBlockedIps.put(address, System.currentTimeMillis());
      return replaceBlockedIps(nextBlockedIps, address, true, p2pService);
    }
  }

  /**
   * Removes an IP from the persistent manual blocklist and applies the updated snapshot.
   */
  public PeerOperationResult unblockIp(String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    InetAddress address = parseIp(ip);
    synchronized (managementLock) {
      P2pService p2pService = requireP2pService();
      if (!blockedIps.containsKey(address)) {
        return operationSucceeded(false, 0);
      }
      Map<InetAddress, Long> nextBlockedIps = new HashMap<>(blockedIps);
      nextBlockedIps.remove(address);
      return replaceBlockedIps(nextBlockedIps, address, false, p2pService);
    }
  }

  private Map<InetAddress, Long> loadBlockedIps() {
    byte[] storedValue = commonStore.get(DB_KEY_BLOCKED_IPS).getData();
    if (storedValue == null) {
      return Collections.emptyMap();
    }
    try {
      return deserializeBlockedIps(storedValue);
    } catch (IOException e) {
      logger.warn("Invalid blocked IP data in CommonStore key blocked-ips; starting with an empty "
          + "manual blocklist and retaining the stored value until the next update", e);
      return Collections.emptyMap();
    }
  }

  private PeerOperationResult replaceBlockedIps(Map<InetAddress, Long> nextBlockedIps,
      InetAddress changedAddress, boolean blocked, P2pService p2pService)
      throws JsonRpcInternalException {
    Map<InetAddress, Long> snapshot = immutableCopy(nextBlockedIps);
    Set<InetAddress> addresses = new HashSet<>(snapshot.keySet());
    saveBlockedIps(snapshot);
    int disconnectedCount;
    try {
      disconnectedCount = p2pService.replaceBlockedIps(addresses);
    } catch (RuntimeException e) {
      logger.error("Failed to apply blocked IP snapshot", e);
      throw new JsonRpcInternalException("Failed to apply blocked IP snapshot", e);
    }
    blockedIps = snapshot;
    logger.info("Admin {} IP {}, disconnected channels {}",
        blocked ? "blocked" : "unblocked", changedAddress.getHostAddress(), disconnectedCount);
    return operationSucceeded(true, disconnectedCount);
  }

  private void saveBlockedIps(Map<InetAddress, Long> snapshot) throws JsonRpcInternalException {
    try {
      byte[] serialized = OBJECT_MAPPER.writeValueAsBytes(toBlockedIpInfos(snapshot));
      commonStore.put(DB_KEY_BLOCKED_IPS, new BytesCapsule(serialized));
    } catch (RuntimeException | IOException e) {
      logger.error("Failed to save CommonStore key blocked-ips", e);
      throw new JsonRpcInternalException("Failed to persist blocked IP snapshot", e);
    }
  }

  private P2pService requireP2pService() throws JsonRpcInternalException {
    P2pService service = p2pService;
    if (service == null) {
      throw new JsonRpcInternalException("P2P service is not ready");
    }
    return service;
  }

  private InetAddress parseIp(String ip) throws JsonRpcInvalidParamsException {
    if (StringUtils.isEmpty(ip) || ip.length() > MAX_IP_LENGTH) {
      throw new JsonRpcInvalidParamsException("IP must be an IPv4 or IPv6 literal");
    }
    try {
      return NetUtil.parseIpAddress(ip.trim());
    } catch (IllegalArgumentException e) {
      throw new JsonRpcInvalidParamsException("IP must be an IPv4 or IPv6 literal", e);
    }
  }

  private InetSocketAddress parseEndpoint(String endpoint)
      throws JsonRpcInvalidParamsException {
    if (StringUtils.isEmpty(endpoint) || endpoint.length() > MAX_ENDPOINT_LENGTH) {
      throw new JsonRpcInvalidParamsException(
          "Endpoint must use IPv4:port or [IPv6]:port format");
    }
    try {
      return NetUtil.parseIpSocketAddress(endpoint);
    } catch (IllegalArgumentException e) {
      throw new JsonRpcInvalidParamsException("Invalid peer endpoint", e);
    }
  }

  private PeerOperationResult operationSucceeded(boolean changed,
      int disconnectedCount) {
    return new PeerOperationResult(true, changed, disconnectedCount, "");
  }

  private PeerOperationResult operationFailed(String errorMessage) {
    return new PeerOperationResult(false, false, 0, errorMessage);
  }

  private Map<InetAddress, Long> deserializeBlockedIps(byte[] value) throws IOException {
    if (value.length == 0 || value.length > MAX_BLOCKED_IPS_VALUE_BYTES) {
      throw new IOException("Invalid blocked IP value size");
    }
    JsonNode root = OBJECT_MAPPER.readTree(value);
    if (root == null || !root.isArray() || root.size() > MAX_BLOCKED_IPS) {
      throw new IOException("Invalid blocked IP value structure");
    }
    Map<InetAddress, Long> entries = new HashMap<>();
    for (JsonNode element : root) {
      JsonNode ip = element.path("ip");
      if (!ip.isTextual() || ip.textValue().length() > MAX_IP_LENGTH) {
        throw new IOException("Invalid blocked IP entry");
      }
      // Creation time is display metadata; an unavailable timestamp must not discard the block.
      JsonNode blockedAtMillis = element.path("blockedAtMillis");
      long timestamp = blockedAtMillis.isIntegralNumber() && blockedAtMillis.canConvertToLong()
          ? blockedAtMillis.longValue() : 0;
      try {
        entries.put(NetUtil.parseIpAddress(ip.textValue()), timestamp);
      } catch (IllegalArgumentException e) {
        throw new IOException("Invalid blocked IP entry", e);
      }
    }
    return entries;
  }

  private List<BlockedIpInfo> toBlockedIpInfos(Map<InetAddress, Long> snapshot) {
    return snapshot.entrySet().stream()
        .map(entry -> new BlockedIpInfo(entry.getKey().getHostAddress(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private Map<InetAddress, Long> immutableCopy(Map<InetAddress, Long> entries) {
    Map<InetAddress, Long> sorted = new TreeMap<>(
        Comparator.comparing(InetAddress::getHostAddress));
    sorted.putAll(entries);
    return Collections.unmodifiableMap(sorted);
  }
}
