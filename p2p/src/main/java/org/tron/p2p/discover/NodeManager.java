package org.tron.p2p.discover;

import java.util.List;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.protocol.kad.KadService;
import org.tron.p2p.discover.socket.DiscoverServer;

public class NodeManager {

  private static DiscoverService discoverService;
  private static DiscoverServer discoverServer;

  public static void init() {
    discoverService = new KadService();
    discoverService.init();
    if (Parameter.p2pConfig.isDiscoverEnable()) {
      discoverServer = new DiscoverServer();
      discoverServer.init(discoverService);
    }
  }

  public static void close() {
    if (discoverService != null) {
      discoverService.close();
    }
    if (discoverServer != null) {
      discoverServer.close();
    }
  }

  public static List<Node> getConnectableNodes() {
    return discoverService.getConnectableNodes();
  }

  public static Node getHomeNode() {
    return discoverService.getPublicHomeNode();
  }

  public static List<Node> getTableNodes() {
    return discoverService.getTableNodes();
  }

  public static List<Node> getAllNodes() {
    return discoverService.getAllNodes();
  }

}
