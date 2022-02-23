package org.tron.core.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Constant;
import org.tron.core.metrics.blockchain.BlockChainInfo;
import org.tron.core.metrics.blockchain.BlockChainMetricManager;
import org.tron.core.metrics.net.NetInfo;
import org.tron.core.metrics.net.NetMetricManager;
import org.tron.core.metrics.node.NodeInfo;
import org.tron.core.metrics.node.NodeMetricManager;
import org.tron.protos.Protocol;

@Slf4j(topic = "metrics")
@Component
public class MetricsApiService {

  private static final long time = System.currentTimeMillis();

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

    metricsInfo.setInterval((System.currentTimeMillis() - time) / Constant.ONE_THOUSAND);

    NodeInfo nodeInfo = nodeMetricManager.getNodeInfo();
    metricsInfo.setNode(nodeInfo);

    BlockChainInfo blockChainInfo = blockChainMetricManager.getBlockChainInfo();
    metricsInfo.setBlockchain(blockChainInfo);

    NetInfo netInfo = netMetricManager.getNetInfo();
    metricsInfo.setNet(netInfo);

    return metricsInfo;
  }

  public Protocol.MetricsInfo getMetricProtoInfo() {

    Protocol.MetricsInfo.Builder builder = Protocol.MetricsInfo.newBuilder();
    builder.setInterval((System.currentTimeMillis() - time) / Constant.ONE_THOUSAND);

    Protocol.MetricsInfo.NodeInfo nodeInfo = nodeMetricManager.getNodeProtoInfo();
    builder.setNode(nodeInfo);

    Protocol.MetricsInfo.BlockChainInfo blockChainInfo =
        blockChainMetricManager.getBlockChainProtoInfo();
    builder.setBlockchain(blockChainInfo);

    Protocol.MetricsInfo.NetInfo netInfo = netMetricManager.getNetProtoInfo();
    builder.setNet(netInfo);

    return builder.build();
  }
}
