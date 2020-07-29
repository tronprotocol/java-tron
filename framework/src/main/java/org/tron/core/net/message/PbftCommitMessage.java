package org.tron.core.net.message;

import org.tron.core.capsule.PbftSignCapsule;
import org.tron.protos.Protocol.PBFTCommitResult;

public class PbftCommitMessage extends TronMessage {

  private PbftSignCapsule pbftSignCapsule;

  public PbftCommitMessage(byte[] data) {
    super(data);
    this.type = MessageTypes.PBFT_COMMIT_MSG.asByte();
    this.pbftSignCapsule = new PbftSignCapsule(data);
  }

  public PbftCommitMessage(PbftSignCapsule pbftSignCapsule) {
    data = pbftSignCapsule.getData();
    this.type = MessageTypes.PBFT_COMMIT_MSG.asByte();
    this.pbftSignCapsule = pbftSignCapsule;
  }

  public PBFTCommitResult getPBFTCommitResult() {
    return getPbftSignCapsule().getPbftCommitResult();
  }

  public PbftSignCapsule getPbftSignCapsule() {
    return pbftSignCapsule;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
  
}
