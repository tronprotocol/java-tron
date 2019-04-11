package org.tron.common.zksnark.sapling.transaction;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.protos.Contract.GrothProof;
import org.tron.protos.Contract.OutputDescription;

@Slf4j(topic = "capsule")
public class OutCapsule implements ProtoCapsule<OutputDescription> {

  private OutputDescription outputDescription;

  public OutCapsule() {
  }

  public OutCapsule(final OutputDescription outputDescription) {
    this.outputDescription = outputDescription;
  }

  public OutCapsule(final byte[] data) {
    try {
      this.outputDescription = OutputDescription.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public OutCapsule(
      ByteString cv,
      ByteString cm,
      ByteString ephemeralKey,
      ByteString encCiphertext,
      ByteString outCiphertext,
      GrothProof zkproof) {
    this.outputDescription =
        OutputDescription.newBuilder()
            .setValueCommitment(cv)
            .setNoteCommitment(cm)
            .setEpk(ephemeralKey)
            .setCEnc(encCiphertext)
            .setCEnc(encCiphertext)
            .setCOut(outCiphertext)
            .setZkproof(zkproof)
            .build();
  }

  public ByteString getValueCommitment() {
    return this.outputDescription.getValueCommitment();
  }

  public void setValueCommitment(byte[] bytes) {
    this.outputDescription =
        this.outputDescription.toBuilder().setValueCommitment(ByteString.copyFrom(bytes)).build();
  }

  public void setValueCommitment(ByteString bytes) {
    this.outputDescription = this.outputDescription.toBuilder().setValueCommitment(bytes).build();
  }

  public ByteString getEphemeralKey() {
    return this.outputDescription.getEpk();
  }

  public void setEpk(byte[] bytes) {
    this.outputDescription =
        this.outputDescription.toBuilder().setEpk(ByteString.copyFrom(bytes)).build();
  }

  public void setEpk(ByteString bytes) {
    this.outputDescription = this.outputDescription.toBuilder().setEpk(bytes).build();
  }

  public ByteString getEncCiphertext() {
    return this.outputDescription.getCEnc();
  }

  public void setCEnc(byte[] bytes) {
    this.outputDescription =
        this.outputDescription.toBuilder().setCEnc(ByteString.copyFrom(bytes)).build();
  }

  public void setCEnc(ByteString bytes) {
    this.outputDescription = this.outputDescription.toBuilder().setCEnc(bytes).build();
  }

  public ByteString getOutCiphertext() {
    return this.outputDescription.getCOut();
  }

  public void setCOut(byte[] bytes) {
    this.outputDescription =
        this.outputDescription.toBuilder().setCOut(ByteString.copyFrom(bytes)).build();
  }

  public void setCOut(ByteString bytes) {
    this.outputDescription = this.outputDescription.toBuilder().setCOut(bytes).build();
  }

  public ByteString getCm() {
    return this.outputDescription.getNoteCommitment();
  }

  public void setNoteCommitment(byte[] bytes) {
    this.outputDescription =
        this.outputDescription.toBuilder().setNoteCommitment(ByteString.copyFrom(bytes)).build();
  }

  public void setNoteCommitment(ByteString bytes) {
    this.outputDescription = this.outputDescription.toBuilder().setNoteCommitment(bytes).build();
  }

  public GrothProof getZkproof() {
    return this.outputDescription.getZkproof();
  }

  public void setZkproof(byte[] proof) {
    GrothProof proof1 = GrothProof.newBuilder().setValues(ByteString.copyFrom(proof)).build();
    this.outputDescription = this.outputDescription.toBuilder().setZkproof(proof1).build();
  }

  public void setZkproof(GrothProof proof) {
    this.outputDescription = this.outputDescription.toBuilder().setZkproof(proof).build();
  }

  @Override
  public byte[] getData() {
    return this.outputDescription.toByteArray();
  }

  @Override
  public OutputDescription getInstance() {
    return this.outputDescription;
  }
}
