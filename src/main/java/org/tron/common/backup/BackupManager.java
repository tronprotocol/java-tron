package org.tron.common.backup;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.net.udp.handler.EventHandler;
import org.tron.common.net.udp.handler.UdpEvent;
import org.tron.common.net.udp.message.Message;
import org.tron.common.net.udp.message.backup.KeepAliveMessage;
import org.tron.common.net.udp.message.discover.FindNodeMessage;
import org.tron.common.net.udp.message.discover.NeighborsMessage;
import org.tron.common.net.udp.message.discover.PingMessage;
import org.tron.common.net.udp.message.discover.PongMessage;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.core.config.args.Args;

@Component
public class BackupManager implements EventHandler{

  static final org.slf4j.Logger logger = LoggerFactory.getLogger("BackupManager");

  private Args args = Args.getInstance();

  private int priority = args.getBackupPriority();

  private Map<String, KeepAliveMessage> members = new ConcurrentHashMap<>();

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  public BackupManager() {

    for (String member : args.getBackupIpList()) {
      members.put(member, new KeepAliveMessage(false, 0));
    }

    logger.info("members : size= {}, {}", members.size(), members);
  }

  @Override
  public void handleEvent(UdpEvent udpEvent) {
    Message m = udpEvent.getMessage();
    InetSocketAddress sender = udpEvent.getAddress();

    switch (m.getType()) {
      case BACKUP_KEEP_ALIVE:
        nodeHandler.handlePing((PingMessage) m);
        break;
      case DISCOVER_PONG:
        nodeHandler.handlePong((PongMessage) m);
        break;
      case DISCOVER_FIND_NODE:
        nodeHandler.handleFindNode((FindNodeMessage) m);
        break;
      case DISCOVER_NEIGHBORS:
        nodeHandler.handleNeighbours((NeighborsMessage) m);
        break;
    }
  }

  @Override
  public void channelActivated(){}

}
