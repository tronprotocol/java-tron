package org.tron.core.metrics.prometheus;

import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.witness.Witness;
import org.tron.protos.Protocol;
import org.tron.test.Env;

@Slf4j(topic = "metric")
public class PrometheusApiServiceTest extends Witness {


  static ChainBaseManager chainManager;
  static LocalDateTime localDateTime = LocalDateTime.now();
  final int blocks = 512;

  private final long time = ZonedDateTime.of(localDateTime,
      ZoneId.systemDefault()).toInstant().toEpochMilli();
  protected String  dbPath = "output-prometheus-metric";
  protected Manager dbManager;
  private TronNetDelegate tronNetDelegate;
  private AnnotationConfigApplicationContext context;



  @Before
  public void init() throws Exception {

    Args.getInstance().setMetricsPrometheusEnable(true);
    Metrics.init();

    context = Env.init(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF, dbPath);
    dbManager = context.getBean(Manager.class);
    dposSlot = context.getBean(DposSlot.class);
    ConsensusService consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    chainManager = dbManager.getChainBaseManager();
    tronNetDelegate = context.getBean(TronNetDelegate.class);

    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.getWitnessScheduleStore().saveActiveWitnesses(new ArrayList<>());
    chainManager.addWitness(ByteString.copyFrom(address));

    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), time, privateKey);

    tronNetDelegate.processBlock(new BlockCapsule(block), false);
  }

  @After
  public void destroy() {
    Env.destroy(context, dbPath);
  }

  @Test
  public void testMetric() throws Exception {

    Map<ByteString, String> witnessAndAccount = addWitnessAndAccount();
    witnessAndAccount.put(ByteString.copyFrom(address), key);
    for (int i = 0; i < blocks; i++) {
      tronNetDelegate.processBlock(getSignedBlock(witnessAndAccount), false);
    }
    Double memoryBytes = CollectorRegistry.defaultRegistry.getSampleValue(
        "system_total_physical_memory_bytes");
    Assert.assertNotNull(memoryBytes);
    Assert.assertTrue(memoryBytes.intValue() > 0);

    Double cpus = CollectorRegistry.defaultRegistry.getSampleValue("system_available_cpus");
    Assert.assertNotNull(cpus);
    Assert.assertEquals(cpus.intValue(), Runtime.getRuntime().availableProcessors());

    Double pushBlock = CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:block_process_latency_seconds_count",
        new String[] {"sync"}, new String[] {"false"});
    Assert.assertNotNull(pushBlock);
    Assert.assertEquals(pushBlock.intValue(), blocks + 1);
    Double errorLogs = CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:error_info_total", new String[] {"net"}, new String[] {MetricLabels.UNDEFINED});
    Assert.assertNull(errorLogs);
  }

}