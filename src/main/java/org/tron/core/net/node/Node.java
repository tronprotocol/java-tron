package org.tron.core.net.node;

import org.tron.core.net.message.Message;

public interface Node {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listenOn(String endPoint);

  void connectToP2PNetWork();

  void syncFrom(byte[] myHeadBlockHash);
}
