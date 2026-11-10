package org.tron.p2p;

import java.util.Set;
import lombok.Getter;
import org.tron.p2p.connection.Channel;

public abstract class P2pEventHandler {

  @Getter
  protected Set<Byte> messageTypes;

  public void onConnect(Channel channel) {
  }

  /**
   * Called when a connection is disconnected.
   *
   * <p>This callback may run synchronously during connection closure, including while the
   * {@link org.tron.p2p.connection.ChannelManager} class monitor is held.
   *
   * @param channel the disconnected connection
   */
  public void onDisconnect(Channel channel) {
  }

  public void onMessage(Channel channel, byte[] data) {
  }
}
