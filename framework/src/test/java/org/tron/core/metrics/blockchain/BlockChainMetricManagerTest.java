package org.tron.core.metrics.blockchain;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;

public class BlockChainMetricManagerTest {

  @Test
  @SuppressWarnings("unchecked")
  public void missingDuplicateWitnessBlockNumberDefaultsToZero() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    boolean nodeMetricsEnabled = parameter.isNodeMetricsEnable();
    String witness = "missing-block-number-" + System.nanoTime();
    parameter.setNodeMetricsEnable(true);
    try {
      MetricsUtil.counterInc(MetricsKey.BLOCKCHAIN_DUP_WITNESS + witness);

      Method getDupWitness = BlockChainMetricManager.class.getDeclaredMethod("getDupWitness");
      getDupWitness.setAccessible(true);
      List<DupWitnessInfo> dupWitnesses = (List<DupWitnessInfo>) getDupWitness.invoke(
          new BlockChainMetricManager());

      DupWitnessInfo dupWitness = dupWitnesses.stream()
          .filter(info -> witness.equals(info.getAddress()))
          .findFirst()
          .orElse(null);
      Assert.assertNotNull(dupWitness);
      Assert.assertEquals(0L, dupWitness.getBlockNum());
    } finally {
      parameter.setNodeMetricsEnable(nodeMetricsEnabled);
    }
  }
}
