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
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ProposalService;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.protos.Protocol.Proposal;

@Slf4j
public class ProposalServiceTest extends BaseTest {

  private static boolean init;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
    
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

}