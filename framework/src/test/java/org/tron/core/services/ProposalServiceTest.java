package org.tron.core.services;

import static org.tron.core.Constant.MAX_PROPOSAL_EXPIRE_TIME;
import static org.tron.core.utils.ProposalUtil.ProposalType.CONSENSUS_LOGIC_OPTIMIZATION;
import static org.tron.core.utils.ProposalUtil.ProposalType.ENERGY_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.PROPOSAL_EXPIRE_TIME;
import static org.tron.core.utils.ProposalUtil.ProposalType.TRANSACTION_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ProposalService;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.protos.Protocol.Proposal;

@Slf4j
public class ProposalServiceTest extends BaseTest {

  private static boolean init;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
    
  }

  @Before
  public void before() {
    if (init) {
      return;
    }
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
    init = true;
  }

  @Test
  public void test() {
    Set<Long> set = new HashSet<>();
    for (ProposalType proposalType : ProposalType.values()) {
      Assert.assertTrue(set.add(proposalType.getCode()));
    }

    Proposal proposal = Proposal.newBuilder().putParameters(1, 1).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    boolean result = ProposalService.process(dbManager, proposalCapsule);
    Assert.assertTrue(result);
    //
    proposal = Proposal.newBuilder().putParameters(1000, 1).build();
    proposalCapsule = new ProposalCapsule(proposal);
    result = ProposalService.process(dbManager, proposalCapsule);
    Assert.assertFalse(result);
    //
    for (ProposalType proposalType : ProposalType.values()) {
      if (proposalType == WITNESS_127_PAY_PER_BLOCK) {
        proposal = Proposal.newBuilder().putParameters(proposalType.getCode(), 16160).build();
      } else {
        proposal = Proposal.newBuilder().putParameters(proposalType.getCode(), 1).build();
      }
      proposalCapsule = new ProposalCapsule(proposal);
      result = ProposalService.process(dbManager, proposalCapsule);
      Assert.assertTrue(result);
    }
  }

  @Test
  public void testUpdateEnergyFee() {
    String preHistory = dbManager.getDynamicPropertiesStore().getEnergyPriceHistory();

    long newPrice = 500;
    Proposal proposal = Proposal.newBuilder().putParameters(ENERGY_FEE.getCode(), newPrice).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    boolean result = ProposalService.process(dbManager, proposalCapsule);
    Assert.assertTrue(result);

    long currentPrice = dbManager.getDynamicPropertiesStore().getEnergyFee();
    Assert.assertEquals(currentPrice, newPrice);

    String currentHistory = dbManager.getDynamicPropertiesStore().getEnergyPriceHistory();
    Assert.assertEquals(preHistory + "," + proposalCapsule.getExpirationTime() + ":" + newPrice,
        currentHistory);
  }

  @Test
  public void testUpdateTransactionFee() {
    String preHistory = dbManager.getDynamicPropertiesStore().getBandwidthPriceHistory();

    long newPrice = 1500;
    Proposal proposal =
        Proposal.newBuilder().putParameters(TRANSACTION_FEE.getCode(), newPrice).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    proposalCapsule.setExpirationTime(1627279200000L);
    boolean result = ProposalService.process(dbManager, proposalCapsule);
    Assert.assertTrue(result);

    long currentPrice = dbManager.getDynamicPropertiesStore().getTransactionFee();
    Assert.assertEquals(currentPrice, newPrice);

    String expResult = preHistory + "," + proposalCapsule.getExpirationTime() + ":" + newPrice;
    String currentHistory = dbManager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    Assert.assertEquals(expResult, currentHistory);
  }

  @Test
  public void testUpdateConsensusLogicOptimization() {
    long v = dbManager.getDynamicPropertiesStore().getConsensusLogicOptimization();
    Assert.assertEquals(v, 0);
    Assert.assertTrue(!dbManager.getDynamicPropertiesStore().allowConsensusLogicOptimization());
    Assert.assertFalse(dbManager.getDynamicPropertiesStore().allowWitnessSortOptimization());
    Assert.assertFalse(dbManager.getDynamicPropertiesStore().disableJavaLangMath());

    long value = 1;
    Proposal proposal =
        Proposal.newBuilder().putParameters(CONSENSUS_LOGIC_OPTIMIZATION.getCode(), value).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    proposalCapsule.setExpirationTime(1627279200000L);
    boolean result = ProposalService.process(dbManager, proposalCapsule);
    Assert.assertTrue(result);

    v = dbManager.getDynamicPropertiesStore().getConsensusLogicOptimization();
    Assert.assertEquals(v, value);

    Assert.assertTrue(dbManager.getDynamicPropertiesStore().allowConsensusLogicOptimization());
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().allowWitnessSortOptimization());
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().disableJavaLangMath());
  }

  @Test
  public void testProposalExpireTime() {
    long defaultWindow = dbManager.getDynamicPropertiesStore().getProposalExpireTime();
    long proposalExpireTime = CommonParameter.getInstance().getProposalExpireTime();
    Assert.assertEquals(proposalExpireTime, defaultWindow);

    Proposal proposal = Proposal.newBuilder().putParameters(PROPOSAL_EXPIRE_TIME.getCode(),
        31536000000L).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    proposalCapsule.setExpirationTime(1627279200000L);
    boolean result = ProposalService.process(dbManager, proposalCapsule);
    Assert.assertTrue(result);

    long window = dbManager.getDynamicPropertiesStore().getProposalExpireTime();
    Assert.assertEquals(MAX_PROPOSAL_EXPIRE_TIME - 3000, window);
  }

  // -------------------------------------------------------------------------------------------
  // CLOSE_EXCHANGE application-side regression tests
  // -------------------------------------------------------------------------------------------

  private static final long CLOSE_EXCHANGE_CODE =
      ProposalType.CLOSE_EXCHANGE.getCode();
  private static final long EXCHANGE_CREATE_FEE_CODE =
      ProposalType.EXCHANGE_CREATE_FEE.getCode();
  private static final long HARDEN_EXCHANGE_CODE =
      ProposalType.ALLOW_HARDEN_EXCHANGE_CALCULATION.getCode();
  private static final long CREATE_ACCOUNT_FEE_CODE =
      ProposalType.CREATE_ACCOUNT_FEE.getCode();
  private static final long WITNESS_PAY_PER_BLOCK_CODE =
      ProposalType.WITNESS_PAY_PER_BLOCK.getCode();

  private DynamicPropertiesStore dps() {
    return dbManager.getDynamicPropertiesStore();
  }

  private void apply(long code, long value) {
    Proposal proposal = Proposal.newBuilder().putParameters(code, value).build();
    Assert.assertTrue(ProposalService.process(dbManager, new ProposalCapsule(proposal)));
  }

  /**
   * The dynamic property starts at 0 (missing key) and only ever moves forward one level at
   * a time; duplicate, downgrade and jump proposals are silently ignored.
   */
  @Test
  public void testApplyCloseExchangeStepwiseAndIdempotent() {
    dps().saveCloseExchange(0);

    apply(CLOSE_EXCHANGE_CODE, 1);
    Assert.assertEquals(1, dps().getCloseExchange());

    // duplicate re-application of the same level in the same maintenance window: no-op
    apply(CLOSE_EXCHANGE_CODE, 1);
    Assert.assertEquals(1, dps().getCloseExchange());

    apply(CLOSE_EXCHANGE_CODE, 2);
    Assert.assertEquals(2, dps().getCloseExchange());

    // stale lower proposal: no-op
    apply(CLOSE_EXCHANGE_CODE, 1);
    Assert.assertEquals(2, dps().getCloseExchange());

    // jump beyond the highest level: no-op
    apply(CLOSE_EXCHANGE_CODE, 3);
    Assert.assertEquals(2, dps().getCloseExchange());

    // reset proposals: no-op
    apply(CLOSE_EXCHANGE_CODE, 0);
    Assert.assertEquals(2, dps().getCloseExchange());
  }

  /**
   * A CLOSE_EXCHANGE entry whose value does not match current + 1 must not block the other
   * parameters carried by the same proposal.
   */
  @Test
  public void testApplyCloseExchangeDoesNotBlockOtherParameters() {
    dps().saveCloseExchange(1);
    dps().saveCreateAccountFee(1000L);
    dps().saveWitnessPayPerBlock(100L);

    Proposal proposal = Proposal.newBuilder()
        .putParameters(CLOSE_EXCHANGE_CODE, 3L)      // jump: ignored
        .putParameters(CREATE_ACCOUNT_FEE_CODE, 2000L)
        .putParameters(WITNESS_PAY_PER_BLOCK_CODE, 300L)
        .build();
    Assert.assertTrue(ProposalService.process(dbManager, new ProposalCapsule(proposal)));

    Assert.assertEquals("invalid CLOSE_EXCHANGE entry must be a no-op",
        1, dps().getCloseExchange());
    Assert.assertEquals(2000L, dps().getCreateAccountFee());
    Assert.assertEquals(300L, dps().getWitnessPayPerBlock());
  }

  /**
   * While the close level is 0, EXCHANGE_CREATE_FEE(12) and
   * ALLOW_HARDEN_EXCHANGE_CALCULATION(98) proposals apply as before; once the level
   * reaches 1 their application becomes a no-op while unrelated parameters of the same
   * proposal still apply. Historical level-0 applications are preserved (the stored
   * value is never reverted).
   */
  @Test
  public void testApplyLegacyExchangeParametersAroundCloseLevel() {
    // ensure level 0: prior tests in this class may have left the level raised
    dps().saveCloseExchange(0);
    // close level 0: both legacy parameters still apply
    dps().saveExchangeCreateFee(1024_000_000L);
    dps().saveAllowHardenExchangeCalculation(0);
    apply(EXCHANGE_CREATE_FEE_CODE, 2_048_000_000L);
    Assert.assertEquals(2_048_000_000L, dps().getExchangeCreateFee());
    apply(HARDEN_EXCHANGE_CODE, 1);
    Assert.assertEquals(1, dps().getAllowHardenExchangeCalculation());

    dps().saveCloseExchange(1);
    try {
      // close level >= 1: legacy exchange parameters are skipped...
      apply(EXCHANGE_CREATE_FEE_CODE, 3_072_000_000L);
      Assert.assertEquals("EXCHANGE_CREATE_FEE must not be applied at close level >= 1",
          2_048_000_000L, dps().getExchangeCreateFee());
      apply(HARDEN_EXCHANGE_CODE, 0);
      Assert.assertEquals("ALLOW_HARDEN_EXCHANGE_CALCULATION must not be applied at "
              + "close level >= 1",
          1, dps().getAllowHardenExchangeCalculation());

      // ...without blocking unrelated parameters of the same proposal
      dps().saveCreateAccountFee(1000L);
      Proposal proposal = Proposal.newBuilder()
          .putParameters(EXCHANGE_CREATE_FEE_CODE, 4_096_000_000L)
          .putParameters(HARDEN_EXCHANGE_CODE, 0L)
          .putParameters(CREATE_ACCOUNT_FEE_CODE, 5000L)
          .build();
      Assert.assertTrue(ProposalService.process(dbManager, new ProposalCapsule(proposal)));
      Assert.assertEquals(2_048_000_000L, dps().getExchangeCreateFee());
      Assert.assertEquals(1, dps().getAllowHardenExchangeCalculation());
      Assert.assertEquals(5000L, dps().getCreateAccountFee());

      // CLOSE_EXCHANGE application itself still works at close level >= 1
      dps().saveCloseExchange(0);
      apply(CLOSE_EXCHANGE_CODE, 1);
      Assert.assertEquals(1, dps().getCloseExchange());
    } finally {
      dps().saveCloseExchange(0);
      dps().saveAllowHardenExchangeCalculation(0);
    }
  }

}