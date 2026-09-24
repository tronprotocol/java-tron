package org.tron.core.jsonrpc;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.logsfilter.capsule.BlockFilterCapsule;
import org.tron.common.logsfilter.capsule.LogsFilterCapsule;
import org.tron.common.logsfilter.queue.FilterCapsuleQueue;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.BlockFilterAndResult;
import org.tron.core.services.jsonrpc.filters.LogFilterAndResult;
import org.tron.protos.Protocol.TransactionInfo;

/**
 * End-to-end coverage of the decoupled filter pipeline: capsules offered to the
 * FilterCapsuleQueue bean are delivered to registered filters by the consumer
 * thread that TronJsonRpcImpl starts at context startup.
 */
public class FilterPipelineDeliveryTest extends BaseTest {

  static {
    Args.setParam(new String[] {"--output-directory", dbPath()}, TestConstants.TEST_CONF);
    // isJsonRpcFilterEnabled() must hold at context startup so the consumer thread starts
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(true);
  }

  @Resource
  private TronJsonRpcImpl tronJsonRpc;
  @Resource
  private FilterCapsuleQueue filterCapsuleQueue;

  private static TransactionInfo buildTxInfoWithLog() {
    LogInfo logInfo = new LogInfo(new byte[20],
        Collections.singletonList(new DataWord(new byte[32])), new byte[0]);
    return TransactionInfo.newBuilder().addLog(LogInfo.buildLog(logInfo)).build();
  }

  private static void await(BooleanSupplier condition, String message)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(50);
    }
    Assert.fail(message);
  }

  @Test
  public void consumerStartedWithContext() {
    ExecutorService filterEs = ReflectUtils.getFieldValue(tronJsonRpc, "filterEs");
    Assert.assertNotNull("consumer did not start", filterEs);
    Assert.assertFalse("consumer already shut down", filterEs.isShutdown());
  }

  @Test
  public void blockFilterDeliveredOnFullAndSolidityPaths() throws Exception {
    BlockFilterAndResult full = new BlockFilterAndResult();
    BlockFilterAndResult solidity = new BlockFilterAndResult();
    tronJsonRpc.getBlockFilter2ResultFull().put("pipeline-block-full", full);
    tronJsonRpc.getBlockFilter2ResultSolidity().put("pipeline-block-solidity", solidity);
    try {
      filterCapsuleQueue.offer(new BlockFilterCapsule("e2e-full-hash", false));
      filterCapsuleQueue.offer(new BlockFilterCapsule("e2e-solidity-hash", true));
      await(() -> full.getResult().size() == 1, "full-path block hash not delivered");
      await(() -> solidity.getResult().size() == 1, "solidity-path block hash not delivered");
    } finally {
      tronJsonRpc.getBlockFilter2ResultFull().remove("pipeline-block-full");
      tronJsonRpc.getBlockFilter2ResultSolidity().remove("pipeline-block-solidity");
    }
  }

  @Test
  public void reorgLogsDeliveredWithRemovedFlag() throws Exception {
    LogFilterAndResult filter = new LogFilterAndResult(new FilterRequest(), 100L, null);
    tronJsonRpc.getEventFilter2ResultFull().put("pipeline-log-removed", filter);
    try {
      filterCapsuleQueue.offer(new LogsFilterCapsule(150L, "0xreorg", null,
          Collections.singletonList(buildTxInfoWithLog()), false, true));
      await(() -> !filter.getResult().isEmpty(), "removed log not delivered");
      List<LogFilterElement> elements = filter.popAll();
      Assert.assertEquals(1, elements.size());
      Assert.assertTrue(elements.get(0).isRemoved());
    } finally {
      tronJsonRpc.getEventFilter2ResultFull().remove("pipeline-log-removed");
    }
  }
}
