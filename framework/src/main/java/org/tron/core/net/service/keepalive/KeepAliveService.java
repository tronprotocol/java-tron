package org.tron.core.net.service.keepalive;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.message.keepalive.PongMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Component
public class KeepAliveService {

  private long KEEP_ALIVE_TIMEOUT = 10_000;

  private long PING_TIMEOUT = 20_000;

  private long PING_PERIOD = 60_000;

  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "KeepAlive"));

  public void init() {
    executor.scheduleWithFixedDelay(() -> {
      try {
        long now = System.currentTimeMillis();
        TronNetService.getPeers().forEach(p -> {
          long pingSent = p.getChannel().pingSent;
          long lastSendTime = p.getChannel().getLastSendTime();
          if (p.getChannel().waitForPong) {
            if (now - pingSent > PING_TIMEOUT) {
              logger.warn("Peer {} receive pong timeout", p.getInetSocketAddress());
              p.disconnect(Protocol.ReasonCode.TIME_OUT);
            }
          } else {
            if (now - lastSendTime > KEEP_ALIVE_TIMEOUT || now - pingSent > PING_PERIOD) {
              p.sendMessage(new PingMessage());
              p.getChannel().waitForPong = true;
              p.getChannel().pingSent = now;
            }
          }
        });
      } catch (Throwable t) {
        logger.error("Exception in keep alive task.", t);
      }
    }, 2, 2, TimeUnit.SECONDS);
  }

  public void close() {
    executor.shutdown();
  }

  public void processMessage(PeerConnection peer, TronMessage message) {
    if (message.getType().equals(MessageTypes.P2P_PING)) {
      peer.sendMessage(new PongMessage());
    } else {
      peer.getChannel().updateLatency(System.currentTimeMillis() - peer.getChannel().pingSent);
      peer.getChannel().waitForPong = false;
    }
  }
}
