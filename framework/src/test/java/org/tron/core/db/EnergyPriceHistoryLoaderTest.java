package org.tron.core.db;

import static org.tron.core.store.DynamicPropertiesStore.DEFAULT_ENERGY_PRICE_HISTORY;
import static org.tron.core.utils.ProposalUtil.ProposalType.ALLOW_CREATION_OF_CONTRACTS;
import static org.tron.core.utils.ProposalUtil.ProposalType.ASSET_ISSUE_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.ENERGY_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.MAX_FEE_LIMIT;
import static org.tron.core.utils.ProposalUtil.ProposalType.TRANSACTION_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.EnergyPriceHistoryLoader;
import org.tron.core.store.ProposalStore;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.Proposal.State;


@Slf4j
public class EnergyPriceHistoryLoaderTest {

  private static ChainBaseManager chainBaseManager;
  private static TronApplicationContext context;
  private static String dbPath = "output-EnergyPriceHistoryLoaderTest-test";
  private static long t1 = 1542607200000L;
  private static long price1 = 20;
  private static long t3 = 1544724000000L;
  private static long price3 = 10;
  private static long t4 = 1606240800000L;
  private static long price4 = 40;
  private static long t5 = 1613044800000L;
  private static long price5 = 140L;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    chainBaseManager = context.getBean(ChainBaseManager.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  public void initDB() {
    t1 = 1542607200000L;
    price1 = 20;
    initProposal(ENERGY_FEE.getCode(), t1, price1, State.APPROVED);

    long t2 = 1543168800000L;
    long price2 = 11;
    initProposal(ENERGY_FEE.getCode(), t2, price2, State.DISAPPROVED);

    t3 = 1544724000000L;
    price3 = 10;
    initProposal(ENERGY_FEE.getCode(), t3, price3, State.APPROVED);

    t4 = 1606240800000L;
    price4 = 40;
    initProposal(ENERGY_FEE.getCode(), t4, price4, State.APPROVED);

    t5 = 1613044800000L;
    price5 = 140L;
    Map<Long, Long> parameters = new HashMap<>();
    parameters.put(TRANSACTION_FEE.getCode(), 140L);
    parameters.put(ENERGY_FEE.getCode(), price5);
    parameters.put(MAX_FEE_LIMIT.getCode(), 5000000000L);
    initProposal(parameters, t5, State.APPROVED);

    long t6 = 1629700950000L;
    long price6 = 420;
    initProposal(ENERGY_FEE.getCode(), t6, price6, State.DISAPPROVED);

    initProposal(ALLOW_CREATION_OF_CONTRACTS.getCode(), 1539259200000L, 1, State.APPROVED);

    parameters = new HashMap<>();
    parameters.put(ASSET_ISSUE_FEE.getCode(), 48000000L);
    parameters.put(WITNESS_127_PAY_PER_BLOCK.getCode(), 128000000L);
    initProposal(parameters, 1572609600000L, State.CANCELED);
  }

  private static void initProposal(long code, long timestamp, long price, State state) {
    long id = chainBaseManager.getDynamicPropertiesStore().getLatestProposalNum() + 1;

    Proposal proposal = Proposal.newBuilder().putParameters(code, price)
        .setExpirationTime(timestamp)
        .setState(state)
        .setProposalId(id)
        .build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);

    chainBaseManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    chainBaseManager.getDynamicPropertiesStore().saveLatestProposalNum(id);
  }

  private static void initProposal(Map<Long, Long> parameters, long timestamp, State state) {
    long id = chainBaseManager.getDynamicPropertiesStore().getLatestProposalNum() + 1;

    Proposal proposal = Proposal.newBuilder().putAllParameters(parameters)
        .setExpirationTime(timestamp)
        .setState(state)
        .setProposalId(id)
        .build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);

    chainBaseManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    chainBaseManager.getDynamicPropertiesStore().saveLatestProposalNum(id);
  }

  @Test
  public void testLoader() {
    if (chainBaseManager == null) {
      init();
    }
    EnergyPriceHistoryLoader loader = new EnergyPriceHistoryLoader(chainBaseManager);

    initDB();

    String preEnergyPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getEnergyPriceHistory();
    String expectedRes = preEnergyPriceHistory + "," + t1 + ":" + price1
        + "," + t3 + ":" + price3
        + "," + t4 + ":" + price4
        + "," + t5 + ":" + price5;

    loader.getEnergyProposals();
    String historyStr = loader.parseProposalsToStr();

    Assert.assertEquals(expectedRes, historyStr);
  }

  @Test
  public void testProposalEmpty() {
    if (chainBaseManager == null) {
      init();
    }

    // clean DB firstly
    ProposalStore proposalStore = chainBaseManager.getProposalStore();
    proposalStore.forEach(
        bytesCapsuleEntry -> proposalStore
            .delete(bytesCapsuleEntry.getKey()));
    chainBaseManager.getDynamicPropertiesStore().saveEnergyPriceHistoryDone(0);

    String preEnergyPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getEnergyPriceHistory();
    Assert.assertEquals(DEFAULT_ENERGY_PRICE_HISTORY, preEnergyPriceHistory);

    // loader work
    EnergyPriceHistoryLoader loader = new EnergyPriceHistoryLoader(chainBaseManager);
    loader.doWork();

    // check result
    String afterEnergyPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getEnergyPriceHistory();
    Assert.assertEquals(DEFAULT_ENERGY_PRICE_HISTORY, afterEnergyPriceHistory);
    Assert.assertEquals(1L,
        chainBaseManager.getDynamicPropertiesStore().getEnergyPriceHistoryDone());
  }
}
