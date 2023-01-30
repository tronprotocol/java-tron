package org.tron.common.backup.socket;

public interface EventHandler {

  void channelActivated();

  void handleEvent(UdpEvent event);

}
