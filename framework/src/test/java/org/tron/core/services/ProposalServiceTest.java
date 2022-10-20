package org.tron.core.services;

import static org.tron.core.utils.ProposalUtil.ProposalType.ENERGY_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.TRANSACTION_FEE;
import static org.tron.core.utils.ProposalUtil.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ProposalService;
import org.tron.core.db.Manager;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.protos.Protocol.Proposal;

@Slf4j
public class ProposalServiceTest {

  private static TronApplicationContext context;
  private static Manager manager;
  private static String dbPath = "output_proposal_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
  }

  @Test
  public void test() {
    Set<Long> set = new HashSet<>();
    for (ProposalType proposalType : ProposalType.values()) {
      Assert.assertTrue(set.add(proposalType.getCode()));
    }

    Proposal proposal = Proposal.newBuilder().putParameters(1, 1).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    boolean result = ProposalService.process(manager, proposalCapsule);
    Assert.assertTrue(result);
    //
    proposal = Proposal.newBuilder().putParameters(1000, 1).build();
    proposalCapsule = new ProposalCapsule(proposal);
    result = ProposalService.process(manager, proposalCapsule);
    Assert.assertFalse(result);
    //
    for (ProposalType proposalType : ProposalType.values()) {
      if (proposalType == WITNESS_127_PAY_PER_BLOCK) {
        proposal = Proposal.newBuilder().putParameters(proposalType.getCode(), 16160).build();
      } else {
        proposal = Proposal.newBuilder().putParameters(proposalType.getCode(), 1).build();
      }
      proposalCapsule = new ProposalCapsule(proposal);
      result = ProposalService.process(manager, proposalCapsule);
      Assert.assertTrue(result);
    }
  }

  @Test
  public void testUpdateEnergyFee() {
    String preHistory = manager.getDynamicPropertiesStore().getEnergyPriceHistory();

    long newPrice = 500;
    Proposal proposal = Proposal.newBuilder().putParameters(ENERGY_FEE.getCode(), newPrice).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    boolean result = ProposalService.process(manager, proposalCapsule);
    Assert.assertTrue(result);

    long currentPrice = manager.getDynamicPropertiesStore().getEnergyFee();
    Assert.assertEquals(currentPrice, newPrice);

    String currentHistory = manager.getDynamicPropertiesStore().getEnergyPriceHistory();
    Assert.assertEquals(preHistory + "," + proposalCapsule.getExpirationTime() + ":" + newPrice,
        currentHistory);
  }

  @Test
  public void testUpdateTransactionFee() {
    String preHistory = manager.getDynamicPropertiesStore().getBandwidthPriceHistory();

    long newPrice = 1500;
    Proposal proposal =
        Proposal.newBuilder().putParameters(TRANSACTION_FEE.getCode(), newPrice).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    proposalCapsule.setExpirationTime(1627279200000L);
    boolean result = ProposalService.process(manager, proposalCapsule);
    Assert.assertTrue(result);

    long currentPrice = manager.getDynamicPropertiesStore().getTransactionFee();
    Assert.assertEquals(currentPrice, newPrice);

    String expResult = preHistory + "," + proposalCapsule.getExpirationTime() + ":" + newPrice;
    String currentHistory = manager.getDynamicPropertiesStore().getBandwidthPriceHistory();
    Assert.assertEquals(expResult, currentHistory);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}