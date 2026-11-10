package org.tron.p2p.connection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.keepalive.KeepAliveService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.connection.socket.PeerServer;
import org.tron.p2p.discover.Node;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;
import org.tron.p2p.protos.Connect.DisconnectReason;
import org.tron.p2p.utils.ByteArray;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
public class ChannelManager {

  @Getter
  private static NodeDetectService nodeDetectService;

  private static PeerServer peerServer;

  @Getter
  private static PeerClient peerClient;

  @Getter
  private static ConnPoolService connPoolService;

  private static KeepAliveService keepAliveService;

  @Getter
  private static HandshakeService handshakeService;

  // Channel.equals() compares remote addresses, which can be shared by distinct connections.
  // Register and remove by identity; snapshot iteration permits concurrent close callbacks.
  private static final CopyOnWriteArrayList<Channel> channels = new CopyOnWriteArrayList<>();

  @Getter
  private static final Cache<InetAddress, Long> bannedNodes = CacheBuilder
      .newBuilder().maximumSize(2000).build(); //ban timestamp

  private static boolean isInit = false;
  public static volatile boolean isShutdown = false;

  public static void init() {
    isInit = true;
    peerServer = new PeerServer();
    peerClient = new PeerClient();
    keepAliveService = new KeepAliveService();
    connPoolService = new ConnPoolService();
    handshakeService = new HandshakeService();
    nodeDetectService = new NodeDetectService();
    peerServer.init();
    peerClient.init();
    keepAliveService.init();
    connPoolService.init(peerClient);
    nodeDetectService.init(peerClient);
  }

  public static void connect(InetSocketAddress address) {
    NetUtil.validateInetSocketAddress(address);
    if (ConnectionPolicy.isBlocked(address)) {
      return;
    }
    peerClient.connect(address.getAddress().getHostAddress(), address.getPort(),
        ByteArray.toHexString(NetUtil.getNodeId()));
  }

  public static ChannelFuture connect(Node node, ChannelFutureListener future) {
    return peerClient.connect(node, future);
  }

  /**
   * Returns a detached snapshot of all registered channel instances, including address collisions.
   */
  public static List<Channel> getAllChannels() {
    return new ArrayList<>(channels);
  }

  public static int getChannelCount() {
    return channels.size();
  }

  /**
   * Returns a detached address-indexed snapshot. When addresses collide, only the most recently
   * registered instance is included. Changes to this map do not affect the registry.
   *
   * @deprecated Use {@link #getAllChannels()} or {@link #getChannelCount()} to include every
   *     instance.
   */
  @Deprecated
  public static Map<InetSocketAddress, Channel> getChannels() {
    Map<InetSocketAddress, Channel> result = new HashMap<>();
    channels.forEach(channel -> result.put(channel.getInetSocketAddress(), channel));
    return result;
  }

  public static void notifyDisconnect(Channel channel) {
    if (channel.getInetSocketAddress() == null) {
      logger.warn("Notify Disconnect peer has no address.");
      return;
    }
    channel.markDisconnected();
    if (!channel.markDisconnectNotified()) {
      return;
    }
    // A synchronous write failure can enter here while a caller holds the admission lock.
    // Cleanup must not acquire that lock under an application handler's monitor.
    channels.removeIf(registered -> registered == channel);
    Parameter.handlerList.forEach(h -> h.onDisconnect(channel));
    InetAddress inetAddress = channel.getInetAddress();
    if (inetAddress != null) {
      banNodeOnDisconnect(inetAddress);
    }
  }

  public static int getConnectionNum(InetAddress inetAddress) {
    int cnt = 0;
    for (Channel channel : channels) {
      if (channel.getInetAddress().equals(inetAddress)) {
        cnt++;
      }
    }
    return cnt;
  }

  /**
   * @deprecated Peer registration is managed by the libp2p HELLO handshake. This method only
   *     performs admission checks and registration; it does not complete the handshake or cancel
   *     its timeout. Application code should let the handshake register peers instead.
   */
  @Deprecated
  public static synchronized DisconnectCode processPeer(Channel channel) {
    DisconnectCode code = checkPeer(channel);
    if (code == DisconnectCode.NORMAL) {
      addPeer(channel);
    }
    return channel.isDisconnect() ? DisconnectCode.UNKNOWN : code;
  }

  /**
   * Checks peer admission. Callers must hold the ChannelManager class lock until addPeer
   * to prevent another connection from taking the checked capacity.
   */
  public static synchronized DisconnectCode checkPeer(Channel channel) {
    if (channel.isDisconnect()) {
      return DisconnectCode.UNKNOWN;
    }
    if (channels.stream().anyMatch(registered -> registered == channel)) {
      return DisconnectCode.NORMAL;
    }

    if (ConnectionPolicy.isBlocked(channel.getInetAddress())) {
      logger.info("Reject peer {} because its IP is manually blocked", channel);
      return DisconnectCode.UNKNOWN;
    }

    if (!channel.isOutbound() && !channel.isTrustPeer()) {
      InetAddress inetAddress = channel.getInetAddress();
      if (isBanned(inetAddress)) {
        logger.info("Peer {} recently disconnected", channel);
        return DisconnectCode.TIME_BANNED;
      }

      if (channels.size() >= Parameter.p2pConfig.getMaxConnections()) {
        logger.info("Too many peers, disconnected with {}", channel);
        return DisconnectCode.TOO_MANY_PEERS;
      }

      int num = getConnectionNum(channel.getInetAddress());
      if (num >= Parameter.p2pConfig.getMaxConnectionsWithSameIp()) {
        logger.info("Max connection with same ip {}", channel);
        return DisconnectCode.MAX_CONNECTION_WITH_SAME_IP;
      }
    }

    if (StringUtils.isNotEmpty(channel.getNodeId())) {
      if (channel.getNodeId().equals(Hex.toHexString(Parameter.p2pConfig.getNodeID()))) {
        logger.info("Channel {} is myself", channel.getInetSocketAddress());
        return DisconnectCode.DUPLICATE_PEER;
      }
      for (Channel c : channels) {
        if (channel.getNodeId().equals(c.getNodeId())) {
          if (c.getStartTime() > channel.getStartTime()) {
            disconnectDuplicatePeer(c);
          } else {
            logger.info("Duplicate peer {}, exist peer {}", channel, c);
            return DisconnectCode.DUPLICATE_PEER;
          }
        }
      }
    }

    return DisconnectCode.NORMAL;
  }

  // Called under the admission lock. Always queue I/O, even on the current EventLoop.
  private static void disconnectDuplicatePeer(Channel peer) {
    peer.getCtx().executor().execute(() -> {
      boolean sendReason;
      // A different EventLoop may start this task before the admission caller returns.
      synchronized (ChannelManager.class) {
        sendReason = !peer.isDisconnect();
      }
      if (sendReason) {
        peer.send(new P2pDisconnectMessage(DisconnectReason.DUPLICATE_PEER));
      }
      peer.close();
    });
    channels.removeIf(registered -> registered == peer);
  }

  public static synchronized void addPeer(Channel channel) {
    if (channel.isDisconnect()
        || channels.stream().anyMatch(registered -> registered == channel)) {
      return;
    }
    channel.markRegisteredPeer();
    channels.add(channel);
    // Cleanup may have completed between the precheck and insertion, without taking this lock.
    if (channel.isDisconnect()) {
      channels.removeIf(registered -> registered == channel);
      return;
    }

    logger.info("Add peer {}, total channels: {}", channel.getInetSocketAddress(), channels.size());
  }

  public static DisconnectReason getDisconnectReason(DisconnectCode code) {
    DisconnectReason disconnectReason;
    switch (code) {
      case DIFFERENT_VERSION:
        disconnectReason = DisconnectReason.DIFFERENT_VERSION;
        break;
      case TIME_BANNED:
        disconnectReason = DisconnectReason.RECENT_DISCONNECT;
        break;
      case DUPLICATE_PEER:
        disconnectReason = DisconnectReason.DUPLICATE_PEER;
        break;
      case TOO_MANY_PEERS:
        disconnectReason = DisconnectReason.TOO_MANY_PEERS;
        break;
      case MAX_CONNECTION_WITH_SAME_IP:
        disconnectReason = DisconnectReason.TOO_MANY_PEERS_WITH_SAME_IP;
        break;
      default: {
        disconnectReason = DisconnectReason.UNKNOWN;
      }
    }
    return disconnectReason;
  }

  public static void logDisconnectReason(Channel channel, DisconnectReason reason) {
    logger.info("Try to close channel: {}, reason: {}", channel.getInetSocketAddress(),
        reason.name());
  }

  public static void banNode(InetAddress inetAddress, Long banTime) {
    long expiry = System.currentTimeMillis() + banTime;
    Long currentExpiry = bannedNodes.getIfPresent(inetAddress);
    if (currentExpiry == null || currentExpiry < expiry) {
      bannedNodes.put(inetAddress, expiry);
    }
  }

  public static boolean isBanned(InetAddress inetAddress) {
    Long expiry = bannedNodes.getIfPresent(inetAddress);
    return expiry != null && expiry > System.currentTimeMillis();
  }

  static void banNodeOnDisconnect(InetAddress inetAddress) {
    long now = System.currentTimeMillis();
    Long expiry = bannedNodes.getIfPresent(inetAddress);
    if (expiry == null || expiry < now) {
      bannedNodes.put(inetAddress, now + Parameter.DEFAULT_BAN_TIME);
    }
  }

  public static void close() {
    if (!isInit || isShutdown) {
      return;
    }
    isShutdown = true;
    connPoolService.close();
    keepAliveService.close();
    peerServer.close();
    peerClient.close();
    nodeDetectService.close();
  }


  public static void processMessage(Channel channel, byte[] data) throws P2pException {
    if (data == null || data.length == 0) {
      throw new P2pException(TypeEnum.EMPTY_MESSAGE, "");
    }
    if (data[0] >= 0) {
      handMessage(channel, data);
      return;
    }

    Message message = Message.parse(data);

    if (message.needToLog()) {
      logger.info("Receive message from channel: {}, {}", channel.getInetSocketAddress(), message);
    } else {
      logger.debug("Receive message from channel {}, {}", channel.getInetSocketAddress(), message);
    }

    if (channel.isNodeDetectMode() && message.getType() != MessageType.STATUS) {
      logger.debug("Node detection channel {} received unexpected message {}, close it",
          channel.getInetSocketAddress(), message.getType());
      channel.close();
      return;
    }

    switch (message.getType()) {
      case KEEP_ALIVE_PING:
      case KEEP_ALIVE_PONG:
        keepAliveService.processMessage(channel, message);
        break;
      case HANDSHAKE_HELLO:
        handshakeService.processMessage(channel, message);
        break;
      case STATUS:
        nodeDetectService.processMessage(channel, message);
        break;
      case DISCONNECT:
        channel.close();
        break;
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type:" + data[0]);
    }
  }

  private static void handMessage(Channel channel, byte[] data) throws P2pException {
    P2pEventHandler handler = Parameter.handlerMap.get(data[0]);
    if (handler == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type:" + data[0]);
    }
    if (channel.isNodeDetectMode()) {
      channel.send(new P2pDisconnectMessage(DisconnectReason.DISCOVER_MODE));
      channel.getCtx().close();
      return;
    }

    if (!channel.isFinishHandshake()) {
      logger.warn("Close channel {}, application message received before handshake",
          channel.getInetSocketAddress());
      channel.send(new P2pDisconnectMessage(DisconnectReason.BAD_PROTOCOL));
      channel.close();
      return;
    }

    handler.onMessage(channel, data);
  }

  /**
   * @deprecated Node IDs are set during the libp2p handshake, and self-connection and
   *     duplicate-peer checks are handled by {@link #checkPeer(Channel)} before registration.
   *     Application code should validate node ID consistency instead of updating it.
   */
  @Deprecated
  public static synchronized void updateNodeId(Channel channel, String nodeId) {
    channel.setNodeId(nodeId);
    if (nodeId.equals(Hex.toHexString(Parameter.p2pConfig.getNodeID()))) {
      logger.warn("Channel {} is myself", channel.getInetSocketAddress());
      disconnectDuplicatePeer(channel);
      return;
    }

    List<Channel> list = new ArrayList<>();
    channels.forEach(c -> {
      if (nodeId.equals(c.getNodeId())) {
        list.add(c);
      }
    });
    if (list.size() <= 1) {
      return;
    }
    Channel c1 = list.get(0);
    Channel c2 = list.get(1);
    if (c1.getStartTime() > c2.getStartTime()) {
      logger.info("Close channel {}, other channel {} is earlier", c1, c2);
      disconnectDuplicatePeer(c1);
    } else {
      logger.info("Close channel {}, other channel {} is earlier", c2, c1);
      disconnectDuplicatePeer(c2);
    }
  }

  public static void triggerConnect(InetSocketAddress address) {
    connPoolService.triggerConnect(address);
  }

  public static int disconnect(InetSocketAddress address) {
    int disconnectedCount = 0;
    for (Channel channel : getAllChannels()) {
      if (address.equals(channel.getInetSocketAddress()) && !channel.isDisconnect()) {
        channel.send(new P2pDisconnectMessage(DisconnectReason.REQUESTED));
        channel.close();
        disconnectedCount++;
      }
    }
    return disconnectedCount;
  }

  public static int disconnectBlockedIps() {
    List<Channel> blockedChannels = new ArrayList<>();
    synchronized (ChannelManager.class) {
      for (Channel channel : getAllChannels()) {
        if (ConnectionPolicy.isBlocked(channel.getInetAddress()) && !channel.isDisconnect()) {
          blockedChannels.add(channel);
        }
      }
    }
    // Closing can synchronously invoke application callbacks, so release the admission lock first.
    // Do not send a DisconnectReason because it would reveal the manual blacklist policy.
    blockedChannels.forEach(Channel::close);
    return blockedChannels.size();
  }
}
