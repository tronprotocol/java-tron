package org.tron.core.consensus;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Proposal.State;

@Slf4j(topic = "witness")
public class ProposalController {

  @Setter
  @Getter
  private Manager manager;

  public static ProposalController createInstance(Manager manager) {
    ProposalController instance = new ProposalController();
    instance.setManager(manager);
    return instance;
  }


  public void processProposals() {
    long latestProposalNum = manager.getDynamicPropertiesStore().getLatestProposalNum();
    if (latestProposalNum == 0) {
      logger.info("latestProposalNum is 0, return");
      return;
    }

    long proposalNum = latestProposalNum;

    ProposalCapsule proposalCapsule = null;

    while (proposalNum > 0) {
      try {
        proposalCapsule = manager.getProposalStore()
            .get(ProposalCapsule.calculateDbKey(proposalNum));
      } catch (Exception ex) {
        logger.error("", ex);
        continue;
      }

      if (proposalCapsule.hasProcessed()) {
        logger
            .info("Proposal has processed, id:[{}], skip it and before it",
                proposalCapsule.getID());
        //proposals with number less than this one, have been processed before
        break;
      }

      if (proposalCapsule.hasCanceled()) {
        logger.info("Proposal has canceled, id:[{}], skip it", proposalCapsule.getID());
        proposalNum--;
        continue;
      }

      long currentTime = manager.getDynamicPropertiesStore().getNextMaintenanceTime();
      if (proposalCapsule.hasExpired(currentTime)) {
        processProposal(proposalCapsule);
        proposalNum--;
        continue;
      }

      proposalNum--;
      logger.info("Proposal has not expired, id:[{}], skip it", proposalCapsule.getID());
    }
    logger.info("Processing proposals done, oldest proposal[{}]", proposalNum);
  }

  public void processProposal(ProposalCapsule proposalCapsule) {

    List<ByteString> activeWitnesses = this.manager.getWitnessScheduleStore().getActiveWitnesses();
    if (proposalCapsule.hasMostApprovals(activeWitnesses)) {
      logger.info(
          "Processing proposal,id:{},it has received most approvals, "
              + "begin to set dynamic parameter:{}, "
              + "and set proposal state as APPROVED",
          proposalCapsule.getID(), proposalCapsule.getParameters());
      setDynamicParameters(proposalCapsule);
      proposalCapsule.setState(State.APPROVED);
      manager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    } else {
      logger.info(
          "Processing proposal,id:{}, "
              + "it has not received enough approvals, set proposal state as DISAPPROVED",
          proposalCapsule.getID());
      proposalCapsule.setState(State.DISAPPROVED);
      manager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    }

  }

  public void setDynamicParameters(ProposalCapsule proposalCapsule) {
    ProposalService.process(manager, proposalCapsule);
  }

}
