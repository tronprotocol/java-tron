package org.tron.p2p.discover;

import java.util.List;
import org.tron.p2p.discover.socket.EventHandler;
import org.tron.p2p.discover.socket.UdpEvent;

public interface DiscoverService extends EventHandler {

  void init();

  void close();

  List<Node> getConnectableNodes();

  List<Node> getTableNodes();

  List<Node> getAllNodes();

  Node getPublicHomeNode();

  void channelActivated();

  void handleEvent(UdpEvent event);
}
