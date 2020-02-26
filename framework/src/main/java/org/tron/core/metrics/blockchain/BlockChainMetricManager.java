package org.tron.core.metrics.blockchain;

    import org.tron.core.metrics.MetricsInfo.BlockchainInfo;
    import org.tron.protos.Protocol.Block;

public class BlockChainMetricManager {

  public BlockchainInfo getBlockchainInfo() {
    return new BlockchainInfo();
  }

  public void applyBlcok(Block block) {
    // witness version, lantency,
  }
}
