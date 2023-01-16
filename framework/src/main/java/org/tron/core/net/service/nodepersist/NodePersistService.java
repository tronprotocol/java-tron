package org.tron.core.net.service.nodepersist;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetService;
import org.tron.p2p.discover.Node;

@Slf4j(topic = "net")
@Component
public class NodePersistService {
  private static final byte[] DB_KEY_PEERS = "peers".getBytes();
  private static final long DB_COMMIT_RATE = 1 * 60 * 1000L;
  private static final int MAX_NODES_WRITE_TO_DB = 30;

  private boolean isNodePersist = Args.getInstance().isNodeDiscoveryPersist();

  private ChainBaseManager chainBaseManager = ChainBaseManager.getInstance();

  private Timer nodePersistTaskTimer = new Timer("NodePersistTaskTimer");

  public void init() {
    if (isNodePersist) {
      nodePersistTaskTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          dbWrite();
        }
      }, DB_COMMIT_RATE, DB_COMMIT_RATE);
    }
  }

  public void close() {
    try {
      nodePersistTaskTimer.cancel();
    } catch (Exception e) {
      logger.error("Close nodePersistTaskTimer failed", e);
    }
  }

  public List<InetSocketAddress> dbRead() {
    List<InetSocketAddress> nodes = new ArrayList<>();
    try {
      byte[] nodeBytes = chainBaseManager.getCommonStore().get(DB_KEY_PEERS).getData();
      if (ByteArray.isEmpty(nodeBytes)) {
        return nodes;
      }
      DBNodes dbNodes = JsonUtil.json2Obj(new String(nodeBytes), DBNodes.class);
      logger.info("Read node from store: {} nodes", dbNodes.getNodes().size());
      dbNodes.getNodes().forEach(n -> nodes.add(new InetSocketAddress(n.getHost(), n.getPort())));
    } catch (Exception e) {
      logger.warn("DB read nodes failed, {}", e.getMessage());
    }
    return nodes;
  }

  private void dbWrite() {
    try {
      List<DBNode> batch = new ArrayList<>();
      List<Node> tableNodes = TronNetService.getP2pService().getTableNodes();
      tableNodes.sort(Comparator.comparingLong(value -> -value.getUpdateTime()));
      for (Node n : tableNodes) {
        batch.add(new DBNode(n.getHost(), n.getPort()));
      }

      if (batch.size() > MAX_NODES_WRITE_TO_DB) {
        batch = batch.subList(0, MAX_NODES_WRITE_TO_DB);
      }

      DBNodes dbNodes = new DBNodes();
      dbNodes.setNodes(batch);

      logger.info("Write nodes to store: {}/{} nodes", batch.size(), tableNodes.size());

      chainBaseManager.getCommonStore()
              .put(DB_KEY_PEERS, new BytesCapsule(JsonUtil.obj2Json(dbNodes).getBytes()));
    } catch (Exception e) {
      logger.warn("DB write nodes failed, {}", e.getMessage());
    }
  }
}
