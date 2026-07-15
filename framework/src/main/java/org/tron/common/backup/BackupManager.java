package org.tron.common.backup;

import static org.tron.common.backup.BackupManager.BackupStatusEnum.INIT;
import static org.tron.common.backup.BackupManager.BackupStatusEnum.MASTER;
import static org.tron.common.backup.BackupManager.BackupStatusEnum.SLAVER;
import static org.tron.common.backup.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;
import static org.tron.core.config.args.InetUtil.resolveInetAddress;

import io.netty.util.internal.ConcurrentSet;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.backup.message.KeepAliveMessage;
import org.tron.common.backup.message.Message;
import org.tron.common.backup.socket.EventHandler;
import org.tron.common.backup.socket.MessageHandler;
import org.tron.common.backup.socket.UdpEvent;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "backup")
@Component
public class BackupManager implements EventHandler {

  private final CommonParameter parameter = CommonParameter.getInstance();

  private final int priority = parameter.getBackupPriority();

  private final int port = parameter.getBackupPort();

  private final int keepAliveInterval = parameter.getKeepAliveInterval();

  private final int keepAliveTimeout = keepAliveInterval * 6;

  private String localIp = "";

  private final Set<String> members = new ConcurrentSet<>();

  private final Map<String, String> domainIpCache = new ConcurrentHashMap<>();

  private final String esName = "backup-manager";
  private final ScheduledExecutorService executorService =
      ExecutorServiceManager.newSingleThreadScheduledExecutor(esName);

  private final String dnsEsName = "backup-dns-refresh";
  private ScheduledExecutorService dnsExecutorService;

  @Setter
  private MessageHandler messageHandler;

  @Getter
  private BackupStatusEnum status = MASTER;

  private volatile long lastKeepAliveTime;

  private volatile boolean isInit = false;

  public void setStatus(BackupStatusEnum status) {
    logger.info("Change backup status to {}", status);
    this.status = status;
  }

  public void init() {

    if (isInit) {
      return;
    }
    isInit = true;

    try {
      localIp = InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      logger.warn("Failed to get local ip");
    }

    for (String ipOrDomain : parameter.getBackupMembers()) {
      InetAddress inetAddress = resolveInetAddress(ipOrDomain);
      if (inetAddress == null) {
        logger.warn("Failed to resolve backup member domain: {}", ipOrDomain);
        continue;
      }
      String ip = inetAddress.getHostAddress();
      if (localIp.equals(ip)) {
        continue;
      }
      if (!NetUtil.validIpV4(ipOrDomain) && !NetUtil.validIpV6(ipOrDomain)) {
        domainIpCache.put(ipOrDomain, ip);
      }
      members.add(ip);
    }

    logger.info("Backup localIp:{}, members: size= {}, {}", localIp, members.size(), members);

    setStatus(INIT);

    lastKeepAliveTime = System.currentTimeMillis();

    executorService.scheduleWithFixedDelay(() -> {
      try {
        if (!status.equals(MASTER)
            && System.currentTimeMillis() - lastKeepAliveTime > keepAliveTimeout) {
          if (status.equals(SLAVER)) {
            setStatus(INIT);
            lastKeepAliveTime = System.currentTimeMillis();
          } else {
            setStatus(MASTER);
          }
        }
        if (status.equals(SLAVER)) {
          return;
        }
        members.forEach(member -> messageHandler
            .accept(new UdpEvent(new KeepAliveMessage(status.equals(MASTER), priority),
                new InetSocketAddress(member, port))));
      } catch (Throwable t) {
        logger.error("Exception in send keep alive", t);
      }
    }, 1000, keepAliveInterval, TimeUnit.MILLISECONDS);

    if (!domainIpCache.isEmpty()) {
      dnsExecutorService = ExecutorServiceManager.newSingleThreadScheduledExecutor(dnsEsName);
      dnsExecutorService.scheduleWithFixedDelay(() -> {
        try {
          refreshMemberIps();
        } catch (Throwable t) {
          logger.error("Exception in backup DNS refresh", t);
        }
      }, 60_000L, 60_000L, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void handleEvent(UdpEvent udpEvent) {
    InetSocketAddress sender = udpEvent.getAddress();
    Message msg = udpEvent.getMessage();
    if (!msg.getType().equals(BACKUP_KEEP_ALIVE)) {
      logger.warn("Receive not keep alive message from {}, type {}", sender.getHostString(),
          msg.getType());
      return;
    }
    if (!members.contains(sender.getHostString())) {
      logger.warn("Receive keep alive message from {} is not my member", sender.getHostString());
      return;
    }
    logger.info("Receive keep alive message from {}", sender);
    lastKeepAliveTime = System.currentTimeMillis();

    KeepAliveMessage keepAliveMessage = (KeepAliveMessage) msg;
    int peerPriority = keepAliveMessage.getPriority();
    String peerIp = sender.getAddress().getHostAddress();

    if (status.equals(INIT) && (keepAliveMessage.getFlag() || peerPriority > priority)) {
      setStatus(SLAVER);
      return;
    }

    if (status.equals(MASTER) && keepAliveMessage.getFlag()) {
      if (peerPriority > priority) {
        setStatus(SLAVER);
      } else if (peerPriority == priority && localIp.compareTo(peerIp) < 0) {
        setStatus(SLAVER);
      }
    }
  }

  public void stop() {
    ExecutorServiceManager.shutdownAndAwaitTermination(executorService, esName);
    if (dnsExecutorService != null) {
      ExecutorServiceManager.shutdownAndAwaitTermination(dnsExecutorService, dnsEsName);
    }
  }

  @Override
  public void channelActivated() {
    init();
  }

  public enum BackupStatusEnum {
    INIT,
    SLAVER,
    MASTER
  }

  /**
   * Re-resolves all tracked domain entries. If an IP has changed, the old IP is
   * removed from {@link #members} and the new IP is added.
   */
  private void refreshMemberIps() {
    for (Map.Entry<String, String> entry : domainIpCache.entrySet()) {
      String domain = entry.getKey();
      String oldIp = entry.getValue();
      InetAddress inetAddress = resolveInetAddress(domain);
      if (inetAddress == null) {
        logger.warn("DNS refresh: failed to re-resolve backup member domain {}, keep it", domain);
        continue;
      }
      String newIp = inetAddress.getHostAddress();
      if (!newIp.equals(oldIp)) {
        logger.info("DNS refresh: backup member {} IP changed {} -> {}", domain, oldIp, newIp);
        members.remove(oldIp);
        members.add(newIp);
        domainIpCache.put(domain, newIp);
      }
    }
  }
}
