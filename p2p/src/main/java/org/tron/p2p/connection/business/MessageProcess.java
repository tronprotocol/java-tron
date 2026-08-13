package org.tron.p2p.connection.business;

import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.message.Message;

public interface MessageProcess {
  void processMessage(Channel channel, Message message);
}
