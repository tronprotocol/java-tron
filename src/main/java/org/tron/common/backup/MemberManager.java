package org.tron.common.backup;

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
import org.tron.common.overlay.discover.DiscoveryEvent;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.core.config.args.Args;

@Component
public class MemberManager {
  static final org.slf4j.Logger logger = LoggerFactory.getLogger("MemberManager");

  private Args args = Args.getInstance();


  static final int MAX_NODES = 2000;
  static final int NODES_TRIM_THRESHOLD = 3000;

  Consumer<DiscoveryEvent> messageSender;

  private Map<String, NodeHandler> Members = new ConcurrentHashMap<>();
  private List<Node> bootNodes = new ArrayList<>();

  // option to handle inbounds only from known peers (i.e. which were discovered by ourselves)
  boolean inboundOnlyFromKnownNodes = false;

  private boolean discoveryEnabled;

  private Timer nodeManagerTasksTimer = new Timer("NodeManagerTasks");
  private ScheduledExecutorService pongTimer;

  @Autowired
  public MemberManager() {

    for (String boot : args.getSeedNode().getIpList()) {
      bootNodes.add(Node.instanceOf(boot));
    }

    logger.info("bootNodes : size= {}", bootNodes.size());

    this.pongTimer = Executors.newSingleThreadScheduledExecutor();
  }
}
