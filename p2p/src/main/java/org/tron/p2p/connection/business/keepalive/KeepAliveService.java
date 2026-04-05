package org.tron.p2p.connection.business.keepalive;

import static org.tron.p2p.base.Parameter.KEEP_ALIVE_TIMEOUT;
import static org.tron.p2p.base.Parameter.PING_TIMEOUT;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.MessageProcess;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.protos.Connect.DisconnectReason;

@Slf4j(topic = "net")
public class KeepAliveService implements MessageProcess {

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
      BasicThreadFactory.builder().namingPattern("keepAlive").build());

  public void init() {
    executor.scheduleWithFixedDelay(() -> {
      try {
        long now = System.currentTimeMillis();
        ChannelManager.getChannels().values().stream()
            .filter(p -> !p.isDisconnect())
            .forEach(p -> {
              if (p.waitForPong) {
                if (now - p.pingSent > KEEP_ALIVE_TIMEOUT) {
                  p.send(new P2pDisconnectMessage(DisconnectReason.PING_TIMEOUT));
                  p.close();
                }
              } else {
                if (now - p.getLastSendTime() > PING_TIMEOUT && p.isFinishHandshake()) {
                  p.send(new PingMessage());
                  p.waitForPong = true;
                  p.pingSent = now;
                }
              }
            });
      } catch (Exception t) {
        log.error("Exception in keep alive task", t);
      }
    }, 2, 2, TimeUnit.SECONDS);
  }

  public void close() {
    executor.shutdown();
  }

  @Override
  public void processMessage(Channel channel, Message message) {
    switch (message.getType()) {
      case KEEP_ALIVE_PING:
        channel.send(new PongMessage());
        break;
      case KEEP_ALIVE_PONG:
        channel.updateAvgLatency(System.currentTimeMillis() - channel.pingSent);
        channel.waitForPong = false;
        break;
      default:
        break;
    }
  }
}
