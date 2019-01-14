package org.tron.common.backup;

import static org.tron.common.backup.BackupManager.BackupStatusEnum.INIT;
import static org.tron.common.backup.BackupManager.BackupStatusEnum.MASTER;
import static org.tron.common.backup.BackupManager.BackupStatusEnum.SLAVER;
import static org.tron.common.net.udp.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;

import io.netty.util.internal.ConcurrentSet;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tron.common.net.udp.handler.EventHandler;
import org.tron.common.net.udp.handler.MessageHandler;
import org.tron.common.net.udp.handler.UdpEvent;
import org.tron.common.net.udp.message.Message;
import org.tron.common.net.udp.message.backup.KeepAliveMessage;
import org.tron.core.config.args.Args;

@Component
public class BackupManager implements EventHandler{
  private static final Logger logger = LoggerFactory.getLogger("BackupManager");

  private Args args = Args.getInstance();

  private int priority = args.getBackupPriority();

  private int port = args.getBackupPort();

  private String localIp = "";

  private Set<String> members = new ConcurrentSet<>();

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private volatile MessageHandler messageHandler;

  private volatile BackupStatusEnum status = MASTER;

  private volatile long lastKeepAliveTime;

  private volatile long keepAliveTimeout = 10_000;

  private volatile boolean isInit = false;

  public void setMessageHandler(MessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  public enum BackupStatusEnum{
    INIT,
    SLAVER,
    MASTER
  }

  public void setStatus(BackupStatusEnum status) {
    logger.info("Change backup status to {}", status);
    this.status = status;
  }

  public BackupStatusEnum getStatus() {
    return status;
  }

  public void init() {

    if (isInit){
      return;
    }
    isInit = true;

    try{
      localIp = InetAddress.getLocalHost().getHostAddress();
    }catch (Exception e){
      logger.warn("Get local ip failed.");
    }

    for (String member : args.getBackupMembers()) {
      if (!localIp.equals(member)){
        members.add(member);
      }
    }

    logger.info("Backup localIp:{}, members: size= {}, {}", localIp, members.size(), members);

    setStatus(INIT);

    lastKeepAliveTime = System.currentTimeMillis();

    executorService.scheduleWithFixedDelay(() -> {
      try {
        if (!status.equals(MASTER) && System.currentTimeMillis() - lastKeepAliveTime > keepAliveTimeout){
          if (status.equals(SLAVER)){
            setStatus(INIT);
            lastKeepAliveTime = System.currentTimeMillis();
          }else {
            setStatus(MASTER);
          }
        }
        if (status.equals(SLAVER)){
          return;
        }
        members.forEach(member -> messageHandler.accept(new UdpEvent(new KeepAliveMessage(status.equals(MASTER), priority),
            new InetSocketAddress(member, port))));
      } catch (Throwable t) {
        logger.error("Exception in send keep alive message:{}" , t.getMessage());
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public void handleEvent(UdpEvent udpEvent) {
    InetSocketAddress sender = udpEvent.getAddress();
    Message msg = udpEvent.getMessage();
    if (!msg.getType().equals(BACKUP_KEEP_ALIVE)){
      logger.warn("Receive not keep alive message from {}, type {}", sender.getHostString(), msg.getType());
      return;
    }
    if (!members.contains(sender.getHostString())){
      logger.warn("Receive keep alive message from {} is not my member.", sender.getHostString());
      return;
    }

    lastKeepAliveTime = System.currentTimeMillis();

    KeepAliveMessage keepAliveMessage = (KeepAliveMessage) msg;
    int peerPriority = keepAliveMessage.getPriority();
    String peerIp = sender.getAddress().getHostAddress();

    if (status.equals(INIT) && (keepAliveMessage.getFlag() || peerPriority > priority)){
      setStatus(SLAVER);
      return;
    }

    if (status.equals(MASTER) && keepAliveMessage.getFlag()){
      if (peerPriority > priority){
        setStatus(SLAVER);
      }else if (peerPriority == priority && localIp.compareTo(peerIp) < 0){
        setStatus(SLAVER);
      }
    }
  }

  @Override
  public void channelActivated(){
    init();
  }

}
