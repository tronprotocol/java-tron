package org.tron.core.db.api;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.protos.Protocol.Proposal.State;

@Slf4j(topic = "DB")
public class BandwidthPriceHistoryLoader {

  private final ChainBaseManager chainBaseManager;
  private List<ProposalCapsule> proposalCapsuleList = new ArrayList<>();

  public BandwidthPriceHistoryLoader(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start to load bandwidth price");

    getBandwidthProposals();
    if (!proposalCapsuleList.isEmpty()) {
      String bandwidthPriceHistory = parseProposalsToStr();
      chainBaseManager.getDynamicPropertiesStore().saveBandwidthPriceHistory(bandwidthPriceHistory);
    }
    finish();

    logger.info(
        "Complete bandwidth price load, total time: {} milliseconds, total proposal count: {}",
        System.currentTimeMillis() - start, proposalCapsuleList.size());
  }

  public void getBandwidthProposals() {
    proposalCapsuleList = chainBaseManager.getProposalStore()
        .getSpecifiedProposals(State.APPROVED, ProposalType.TRANSACTION_FEE.getCode());
  }

  public String parseProposalsToStr() {
    StringBuilder builder =
        new StringBuilder(DynamicPropertiesStore.DEFAULT_BANDWIDTH_PRICE_HISTORY);

    for (ProposalCapsule proposalCapsule : proposalCapsuleList) {
      builder.append(",")
          .append(proposalCapsule.getExpirationTime())
          .append(":")
          .append(proposalCapsule.getParameters().get(ProposalType.TRANSACTION_FEE.getCode()));
    }

    return builder.toString();
  }

  public void finish() {
    chainBaseManager.getDynamicPropertiesStore().saveBandwidthPriceHistoryDone(1);
  }

}
