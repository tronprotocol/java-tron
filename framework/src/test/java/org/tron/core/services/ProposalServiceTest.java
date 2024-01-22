package org.tron.core.services;

import static org.tron.core.utils.ProposalUtil.ProposalType.ALLOW_OLD_REWARD_OPT;
import static org.tron.core.utils.ProposalUtil.ProposalType.ENERGY_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.TRANSACTION_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ProposalService;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.protos.Protocol.Proposal;

@Slf4j
public class ProposalServiceTest extends BaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
      if (proposalType == ALLOW_OLD_REWARD_OPT) {
        thrown.expect(IllegalStateException.class);
      }
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

}