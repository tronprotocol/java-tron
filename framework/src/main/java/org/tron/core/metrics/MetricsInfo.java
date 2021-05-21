package org.tron.core.metrics;

import lombok.Data;
import org.tron.core.metrics.blockchain.BlockChainInfo;
import org.tron.core.metrics.net.NetInfo;
import org.tron.core.metrics.node.NodeInfo;

@Data
public class MetricsInfo {
  private long interval;
  private NodeInfo node;
  private BlockChainInfo blockchain;
  private NetInfo net;
}
