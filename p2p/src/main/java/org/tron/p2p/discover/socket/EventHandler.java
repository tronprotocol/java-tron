package org.tron.p2p.discover.socket;

import java.util.function.Consumer;

public interface EventHandler {

  void channelActivated();

  void handleEvent(UdpEvent event);

  void setMessageSender(Consumer<UdpEvent> messageSender);
}
