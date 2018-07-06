package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Proposal;

@Slf4j
public class ProposalCapsule implements ProtoCapsule<Proposal> {

  private Proposal proposal;

  public ProposalCapsule(final Proposal proposal) {
    this.proposal = proposal;
  }

  public ProposalCapsule(final byte[] data) {
    try {
      this.proposal = Proposal.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ProposalCapsule(ByteString address, final long id) {
    this.proposal = Proposal.newBuilder()
        .setProposerAddress(address)
        .setProposalId(id)
        .build();
  }

  public long getID() {
    return this.proposal.getProposalId();
  }

  public void setID(long id) {
    this.proposal = this.proposal.toBuilder()
        .setProposalId(id)
        .build();
  }

  public ByteString getProposalAddress() {
    return this.proposal.getProposerAddress();
  }

  public void setProposalAddress(ByteString address) {
    this.proposal = this.proposal.toBuilder()
        .setProposerAddress(address)
        .build();
  }

  public Map<Long, Long> getParameters() {
    return this.proposal.getParametersMap();
  }

  public void setParameters(Map<Long, Long> parameters) {
    this.proposal = this.proposal.toBuilder()
        .putAllParameters(parameters)
        .build();
  }

  public long getExpirationTime() {
    return this.proposal.getExpirationTime();
  }

  public void setExpirationTime(long time) {
    this.proposal = this.proposal.toBuilder()
        .setExpirationTime(time)
        .build();
  }

  public long getEffectivePeriodTime() {
    return this.proposal.getEffectivePeriodTime();
  }

  public void setEffectivePeriodTime(long time) {
    this.proposal = this.proposal.toBuilder()
        .setEffectivePeriodTime(time)
        .build();
  }

  public List<ByteString> getApprovals() {
    return this.proposal.getApprovalsList();
  }

  public void removeApproval(ByteString address) {
    List<ByteString> approvals = getApprovals();
    approvals.remove(address);
    this.proposal = this.proposal.toBuilder()
        .addAllApprovals(approvals)
        .build();
  }

  public void addApproval(ByteString committeeAddress) {
    this.proposal = this.proposal.toBuilder()
        .addApprovals(committeeAddress)
        .build();
  }

  public int getState() {
    return this.proposal.getStateValue();
  }

  public void setState(int state) {
    this.proposal = this.proposal.toBuilder()
        .setStateValue(state)
        .build();
  }

  public byte[] createDbKey() {
    return ByteArray.fromLong(getID());
  }

  @Override
  public byte[] getData() {
    return this.proposal.toByteArray();
  }

  @Override
  public Proposal getInstance() {
    return this.proposal;
  }

}
