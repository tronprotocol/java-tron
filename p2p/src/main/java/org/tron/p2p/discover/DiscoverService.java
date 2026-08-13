package org.tron.p2p.discover;

import org.tron.p2p.discover.socket.EventHandler;
import org.tron.p2p.discover.socket.UdpEvent;

import java.util.List;

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
