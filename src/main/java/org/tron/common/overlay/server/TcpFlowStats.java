package org.tron.common.overlay.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TcpFlowStats {

  private static final long MIN_DATA_LENGTH = 2048;

  private Map<InetAddress, AtomicLong> peerInSizeMap = new ConcurrentHashMap<>();

  private ScheduledExecutorService executor;

  @PostConstruct
  public void init() {
    executor = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("TcpFlowStats-%d").build());
    executor.scheduleAtFixedRate(new LogTask(), 10, 10, TimeUnit.SECONDS);
  }

  private class LogTask implements Runnable {

    @Override
    public void run() {
      StringBuffer sb = new StringBuffer();
      for (Entry<InetAddress, AtomicLong> entry : peerInSizeMap.entrySet()) {
        sb.append(entry.getKey());
        sb.append(" : ");
        sb.append(entry.getValue().get());
        sb.append("\r\n");
      }
      logger.info("[TcpFlowStats] flow stats : {}", sb.toString());
    }
  }

  public void statsPeerFlow(ChannelHandlerContext ctx, int length) {
    InetAddress inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getAddress();
    if (!peerInSizeMap.containsKey(inetAddress)) {
      peerInSizeMap.put(inetAddress, new AtomicLong(0));
    }
    peerInSizeMap.get(inetAddress).addAndGet(length);
  }

  public boolean peerIsHaveDataTransfer(Channel channel) {
    if (peerInSizeMap.containsKey(channel.getInetAddress())) {
      return peerInSizeMap.get(channel.getInetAddress()).get() > MIN_DATA_LENGTH;
    }
    return false;
  }

  public void resetPeerFlow(Channel channel) {
    peerInSizeMap.remove(channel.getInetAddress());
  }
}
