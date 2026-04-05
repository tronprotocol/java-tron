package org.tron.p2p;

import java.util.Set;
import lombok.Getter;
import org.tron.p2p.connection.Channel;

public abstract class P2pEventHandler {

  @Getter
  protected Set<Byte> messageTypes;

  public void onConnect(Channel channel) {
  }

  public void onDisconnect(Channel channel) {
  }

  public void onMessage(Channel channel, byte[] data) {
  }
}
