package org.tron.core.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.metrics.blockchain.BlockChainInfo;
import org.tron.core.metrics.blockchain.BlockChainMetricManager;
import org.tron.core.metrics.net.NetInfo;
import org.tron.core.metrics.net.NetMetricManager;
import org.tron.core.metrics.node.NodeInfo;
import org.tron.core.metrics.node.NodeMetricManager;

@Slf4j(topic = "metrics")
@Component
public class MetricsApiService {

  public static final long startTime = System.currentTimeMillis();

  @Autowired
  private BlockChainMetricManager blockChainMetricManager;

  @Autowired
  private NetMetricManager netMetricManager;

  @Autowired
  private NodeMetricManager nodeMetricManager;

  /**
   * get metrics info.
   *
   * @return metricsInfo
   */
  public MetricsInfo getMetricsInfo() {

    MetricsInfo metricsInfo = new MetricsInfo();

    metricsInfo.setInterval(MetricsUtil.getInterval());

    NodeInfo nodeInfo = nodeMetricManager.getNodeInfo();
    metricsInfo.setNode(nodeInfo);

    BlockChainInfo blockChainInfo = blockChainMetricManager.getBlockChainInfo();
    metricsInfo.setBlockchain(blockChainInfo);

    NetInfo netInfo = netMetricManager.getNetInfo();
    metricsInfo.setNet(netInfo);

    return metricsInfo;
  }
}
