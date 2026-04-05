package org.tron.p2p.discover.protocol.kad;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.DiscoverService;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.kad.FindNodeMessage;
import org.tron.p2p.discover.message.kad.KadMessage;
import org.tron.p2p.discover.message.kad.NeighborsMessage;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.message.kad.PongMessage;
import org.tron.p2p.discover.protocol.kad.table.NodeTable;
import org.tron.p2p.discover.socket.UdpEvent;

@Slf4j(topic = "net")
public class KadService implements DiscoverService {

  private static final int MAX_NODES = 2000;
  private static final int NODES_TRIM_THRESHOLD = 3000;
  @Getter
  @Setter
  private static long pingTimeout = 15_000;

  private final List<Node> bootNodes = new ArrayList<>();

  private volatile boolean inited = false;

  private final Map<InetSocketAddress, NodeHandler> nodeHandlerMap = new ConcurrentHashMap<>();

  private Consumer<UdpEvent> messageSender;

  private NodeTable table;
  private Node homeNode;

  private ScheduledExecutorService pongTimer;
  private DiscoverTask discoverTask;

  public void init() {
    for (InetSocketAddress address : Parameter.p2pConfig.getSeedNodes()) {
      bootNodes.add(new Node(address));
    }
    for (InetSocketAddress address : Parameter.p2pConfig.getActiveNodes()) {
      bootNodes.add(new Node(address));
    }
    this.pongTimer = Executors.newSingleThreadScheduledExecutor(
        BasicThreadFactory.builder().namingPattern("pongTimer").build());
    this.homeNode = new Node(Parameter.p2pConfig.getNodeID(), Parameter.p2pConfig.getIp(),
        Parameter.p2pConfig.getIpv6(), Parameter.p2pConfig.getPort());
    this.table = new NodeTable(homeNode);

    if (Parameter.p2pConfig.isDiscoverEnable()) {
      discoverTask = new DiscoverTask(this);
      discoverTask.init();
    }
  }

  public void close() {
    try {
      if (pongTimer != null) {
        pongTimer.shutdownNow();
      }

      if (discoverTask != null) {
        discoverTask.close();
      }
    } catch (Exception e) {
      log.error("Close nodeManagerTasksTimer or pongTimer failed", e);
      throw e;
    }
  }

  public List<Node> getConnectableNodes() {
    return getAllNodes().stream()
        .filter(node -> node.isConnectible(Parameter.p2pConfig.getNetworkId()))
        .filter(node -> node.getPreferInetSocketAddress() != null)
        .collect(Collectors.toList());
  }

  public List<Node> getTableNodes() {
    return table.getTableNodes();
  }

  public List<Node> getAllNodes() {
    List<Node> nodeList = new ArrayList<>();
    for (NodeHandler nodeHandler : nodeHandlerMap.values()) {
      nodeList.add(nodeHandler.getNode());
    }
    return nodeList;
  }

  @Override
  public void setMessageSender(Consumer<UdpEvent> messageSender) {
    this.messageSender = messageSender;
  }

  @Override
  public void channelActivated() {
    if (!inited) {
      inited = true;

      for (Node node : bootNodes) {
        getNodeHandler(node);
      }
    }
  }

  @Override
  public void handleEvent(UdpEvent udpEvent) {
    KadMessage m = (KadMessage) udpEvent.getMessage();

    InetSocketAddress sender = udpEvent.getAddress();

    Node n;
    if (sender.getAddress() instanceof Inet4Address) {
      n = new Node(m.getFrom().getId(), sender.getHostString(), m.getFrom().getHostV6(),
          sender.getPort(), m.getFrom().getPort());
    } else {
      n = new Node(m.getFrom().getId(), m.getFrom().getHostV4(), sender.getHostString(),
          sender.getPort(), m.getFrom().getPort());
    }

    NodeHandler nodeHandler = getNodeHandler(n);
    nodeHandler.getNode().setId(n.getId());
    nodeHandler.getNode().touch();

    switch (m.getType()) {
      case KAD_PING:
        nodeHandler.handlePing((PingMessage) m);
        break;
      case KAD_PONG:
        nodeHandler.handlePong((PongMessage) m);
        break;
      case KAD_FIND_NODE:
        nodeHandler.handleFindNode((FindNodeMessage) m);
        break;
      case KAD_NEIGHBORS:
        nodeHandler.handleNeighbours((NeighborsMessage) m, sender);
        break;
      default:
        break;
    }
  }

  public NodeHandler getNodeHandler(Node n) {
    NodeHandler ret = null;
    InetSocketAddress inet4 = n.getInetSocketAddressV4();
    InetSocketAddress inet6 = n.getInetSocketAddressV6();
    if (inet4 != null) {
      ret = nodeHandlerMap.get(inet4);
    }
    if (ret == null && inet6 != null) {
      ret = nodeHandlerMap.get(inet6);
    }

    if (ret == null) {
      trimTable();
      ret = new NodeHandler(n, this);
      if (n.getPreferInetSocketAddress() != null) {
        nodeHandlerMap.put(n.getPreferInetSocketAddress(), ret);
      }
    } else {
      ret.getNode().updateHostV4(n.getHostV4());
      ret.getNode().updateHostV6(n.getHostV6());
    }
    return ret;
  }

  public NodeTable getTable() {
    return table;
  }

  public Node getPublicHomeNode() {
    return homeNode;
  }

  public void sendOutbound(UdpEvent udpEvent) {
    if (Parameter.p2pConfig.isDiscoverEnable() && messageSender != null) {
      messageSender.accept(udpEvent);
    }
  }

  public ScheduledExecutorService getPongTimer() {
    return pongTimer;
  }

  private void trimTable() {
    if (nodeHandlerMap.size() > NODES_TRIM_THRESHOLD) {
      nodeHandlerMap.values().forEach(handler -> {
        if (!handler.getNode().isConnectible(Parameter.p2pConfig.getNetworkId())) {
          nodeHandlerMap.values().remove(handler);
        }
      });
    }
    if (nodeHandlerMap.size() > NODES_TRIM_THRESHOLD) {
      List<NodeHandler> sorted = new ArrayList<>(nodeHandlerMap.values());
      sorted.sort(Comparator.comparingLong(o -> o.getNode().getUpdateTime()));
      for (NodeHandler handler : sorted) {
        nodeHandlerMap.values().remove(handler);
        if (nodeHandlerMap.size() <= MAX_NODES) {
          break;
        }
      }
    }
  }
}
