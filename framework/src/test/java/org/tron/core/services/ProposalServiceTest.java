package org.tron.core.services;

import static org.tron.core.utils.ProposalUtil.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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

public class ProposalServiceTest {

  private TronApplicationContext context;
  private Manager manager;
  private String dbPath = "output_proposal_test";

  @Before
  public void init() {
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


  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
