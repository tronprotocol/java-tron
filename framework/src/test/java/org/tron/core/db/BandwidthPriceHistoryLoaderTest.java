package org.tron.core.db;

import static org.tron.core.store.DynamicPropertiesStore.DEFAULT_BANDWIDTH_PRICE_HISTORY;
import static org.tron.core.utils.ProposalUtil.ProposalType.ALLOW_CREATION_OF_CONTRACTS;
import static org.tron.core.utils.ProposalUtil.ProposalType.ALLOW_TVM_FREEZE;
import static org.tron.core.utils.ProposalUtil.ProposalType.ALLOW_TVM_LONDON;
import static org.tron.core.utils.ProposalUtil.ProposalType.ALLOW_TVM_VOTE;
import static org.tron.core.utils.ProposalUtil.ProposalType.ASSET_ISSUE_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.CREATE_ACCOUNT_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT;
import static org.tron.core.utils.ProposalUtil.ProposalType.TRANSACTION_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.BandwidthPriceHistoryLoader;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.Proposal.State;


@Slf4j
public class BandwidthPriceHistoryLoaderTest {

  private static ChainBaseManager chainBaseManager;
  private static TronApplicationContext context;
  private static final String dbPath = "output-BandwidthPriceHistoryLoaderTest-test";
  private static long t1;
  private static long price1;
  private static long t2;
  private static long price2;
  private static long t5;
  private static long price5;

  // Note, here use @Before and @After instead of @BeforeClass and @AfterClass,
  // because it needs to initialize DB before the single test every time
  @Before
  public void init() {
    Args.setParam(new String[] {"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  public void initDB() {
    t1 = 1606240800000L;
    price1 = 40;
    initProposal(TRANSACTION_FEE.getCode(), t1, price1, State.APPROVED);

    t2 = 1613044800000L;
    price2 = 140;
    initProposal(TRANSACTION_FEE.getCode(), t2, price2, State.APPROVED);

    long t3 = 1626501600000L;
    long price3 = 1000;
    Map<Long, Long> parameters = new HashMap<>();
    parameters.put(TRANSACTION_FEE.getCode(), price3);
    parameters.put(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), 1000000L);
    initProposal(parameters, t3, State.CANCELED);

    long t4 = 1626501600000L;
    long price4 = 1000;
    parameters = new HashMap<>();
    parameters.put(TRANSACTION_FEE.getCode(), price4);
    parameters.put(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), 1000000L);
    initProposal(parameters, t4, State.DISAPPROVED);

    t5 = 1627279200000L;
    price5 = 1000L;
    parameters = new HashMap<>();
    parameters.put(TRANSACTION_FEE.getCode(), price4);
    parameters.put(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), 1000000L);
    initProposal(parameters, t5, State.APPROVED);

    long t6 = 1634299200000L;
    parameters = new HashMap<>();
    parameters.put(ALLOW_TVM_FREEZE.getCode(), 1L);
    parameters.put(ALLOW_TVM_VOTE.getCode(), 1L);
    initProposal(parameters, t6, State.DISAPPROVED);

    initProposal(ALLOW_TVM_LONDON.getCode(), 1647604800000L, 1, State.APPROVED);

    initProposal(ALLOW_CREATION_OF_CONTRACTS.getCode(), 1539259200000L, 1, State.APPROVED);
    initProposal(CREATE_ACCOUNT_FEE.getCode(), 1621468800000L, 1, State.DISAPPROVED);

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
  public void testLoaderWork() {

    initDB();

    String preBandwidthPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    String expectedRes = preBandwidthPriceHistory + "," + t1 + ":" + price1
        + "," + t2 + ":" + price2
        + "," + t5 + ":" + price5;

    BandwidthPriceHistoryLoader loader = new BandwidthPriceHistoryLoader(chainBaseManager);
    loader.getBandwidthProposals();
    String historyStr = loader.parseProposalsToStr();

    Assert.assertEquals(expectedRes, historyStr);
  }

  @Test
  public void testProposalEmpty() {
    String preBandwidthPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    Assert.assertEquals(DEFAULT_BANDWIDTH_PRICE_HISTORY, preBandwidthPriceHistory);

    chainBaseManager.getDynamicPropertiesStore().saveBandwidthPriceHistoryDone(0);

    // loader work
    BandwidthPriceHistoryLoader loader = new BandwidthPriceHistoryLoader(chainBaseManager);
    loader.doWork();

    // check result
    String afterBandwidthPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    Assert.assertEquals(DEFAULT_BANDWIDTH_PRICE_HISTORY, afterBandwidthPriceHistory);
    Assert.assertEquals(1L,
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistoryDone());
  }

  @Test
  public void testLoaderWithProposals() {
    String preBandwidthPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    Assert.assertEquals(DEFAULT_BANDWIDTH_PRICE_HISTORY, preBandwidthPriceHistory);

    chainBaseManager.getDynamicPropertiesStore().saveBandwidthPriceHistoryDone(0);

    // init proposals
    initDB();

    // loader work
    BandwidthPriceHistoryLoader loader = new BandwidthPriceHistoryLoader(chainBaseManager);
    loader.doWork();

    // check result
    String afterBandwidthPriceHistory =
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    String expectedRes = preBandwidthPriceHistory + "," + t1 + ":" + price1
        + "," + t2 + ":" + price2
        + "," + t5 + ":" + price5;

    Assert.assertEquals(expectedRes, afterBandwidthPriceHistory);
    Assert.assertEquals(1L,
        chainBaseManager.getDynamicPropertiesStore().getBandwidthPriceHistoryDone());
  }
}
