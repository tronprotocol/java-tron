package org.tron.core.metrics;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.program.Version;
import org.tron.protos.Protocol;

@Slf4j
public class MetricsApiServiceTest extends BaseMethodTest {

  private static String dbDirectory = "metrics-database";
  private static String indexDirectory = "metrics-index";
  private static int port = 10001;
  private MetricsApiService metricsApiService;
  private RpcApiService rpcApiService;

  @Override
  protected String[] extraArgs() {
    return new String[]{
        "--storage-db-directory", dbDirectory,
        "--storage-index-directory", indexDirectory,
        "--debug"
    };
  }

  @Override
  protected void afterInit() {
    CommonParameter parameter = Args.getInstance();
    parameter.setNodeListenPort(port);
    parameter.getSeedNode().getAddressList().clear();
    parameter.setNodeExternalIp("127.0.0.1");
    metricsApiService = context.getBean(MetricsApiService.class);
    appT.startup();
  }

  @Test
  public void testProcessMessage() {

    MetricsInfo m1 = metricsApiService.getMetricsInfo();

    Protocol.MetricsInfo m2 = metricsApiService.getMetricProtoInfo();

    Assert.assertEquals(m1.getNode().getBackupStatus(), m2.getNode().getBackupStatus());
    Assert.assertEquals(m1.getNode().getIp(), m2.getNode().getIp());
    Assert.assertEquals(m1.getNode().getNodeType(), m2.getNode().getNodeType());
    Assert.assertEquals(m1.getNode().getVersion(), m2.getNode().getVersion());
    Assert.assertEquals(m1.getNode().getVersion(), Version.getVersion());

    Assert.assertEquals(m1.getBlockchain().getBlockProcessTime().getCount(),
        m2.getBlockchain().getBlockProcessTime().getCount());
    Assert
        .assertEquals(m1.getBlockchain().getFailForkCount(), m2.getBlockchain().getFailForkCount());
    Assert.assertEquals(m1.getBlockchain().getFailProcessBlockNum(),
        m2.getBlockchain().getFailProcessBlockNum());
    Assert.assertEquals(m1.getBlockchain().getForkCount(), m2.getBlockchain().getForkCount());
    Assert.assertEquals(m1.getBlockchain().getFailProcessBlockReason(),
        m2.getBlockchain().getFailProcessBlockReason());
    Assert
        .assertEquals(m1.getBlockchain().getHeadBlockHash(), m2.getBlockchain().getHeadBlockHash());
    Assert.assertEquals(m1.getBlockchain().getHeadBlockNum(), m2.getBlockchain().getHeadBlockNum());
    Assert.assertEquals(m1.getBlockchain().getHeadBlockTimestamp(),
        m2.getBlockchain().getHeadBlockTimestamp());
    Assert.assertEquals(m1.getBlockchain().getMissedTransaction().getCount(),
        m2.getBlockchain().getMissedTransaction().getCount());
    Assert.assertEquals(m1.getBlockchain().getTps().getCount(),
        m2.getBlockchain().getTps().getCount());

    Assert.assertEquals(m1.getNet().getApi().getQps().getCount(),
        m2.getNet().getApi().getQps().getCount());
    Assert.assertEquals(m1.getNet().getApi().getFailQps().getCount(),
        m2.getNet().getApi().getFailQps().getCount());
    Assert.assertEquals(m1.getNet().getApi().getOutTraffic().getCount(),
        m2.getNet().getApi().getOutTraffic().getCount());
    Assert.assertEquals(m1.getNet().getConnectionCount(), m2.getNet().getConnectionCount());
    Assert.assertEquals(m1.getNet().getDisconnectionCount(), m2.getNet().getDisconnectionCount());
    Assert.assertEquals(m1.getNet().getErrorProtoCount(), m2.getNet().getErrorProtoCount());
    Assert
        .assertEquals(m1.getNet().getValidConnectionCount(), m2.getNet().getValidConnectionCount());
  }

}
