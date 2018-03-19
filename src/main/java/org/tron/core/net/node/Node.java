package org.tron.core.net.node;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.tmsg.Message;

public interface Node {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listen();

  void connectToP2PNetWork();

  void syncFrom(Sha256Hash myHeadBlockHash);

  void close() throws InterruptedException;
}
