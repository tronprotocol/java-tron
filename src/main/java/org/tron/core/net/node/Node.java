package org.tron.core.net.node;

import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Quitable;
import org.tron.common.utils.Sha256Hash;

public interface Node extends Quitable {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listen();

  void syncFrom(Sha256Hash myHeadBlockHash);

  void close() throws InterruptedException;
}
