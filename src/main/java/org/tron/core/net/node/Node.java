package org.tron.core.net.node;

import java.util.List;
import java.util.Map;
import org.tron.common.overlay.discover.NodeHandler;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;

public interface Node {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listen();

  void syncFrom(Sha256Hash myHeadBlockHash);

  void close() throws InterruptedException;

  Map<String, NodeHandler> getActiveNodes();
}
