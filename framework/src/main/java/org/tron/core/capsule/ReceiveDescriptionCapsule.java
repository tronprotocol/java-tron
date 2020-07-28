package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.contract.ShieldContract.ReceiveDescription;

@Slf4j(topic = "capsule")
public class ReceiveDescriptionCapsule implements ProtoCapsule<ReceiveDescription> {

  private ReceiveDescription receiveDescription;

  public ReceiveDescriptionCapsule() {
    receiveDescription = ReceiveDescription.newBuilder().build();
  }

  public ReceiveDescriptionCapsule(final ReceiveDescription outputDescription) {
    this.receiveDescription = receiveDescription;
  }

  public ReceiveDescriptionCapsule(final byte[] data) {
    try {
      this.receiveDescription = ReceiveDescription.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ReceiveDescriptionCapsule(
      ByteString cv,
      ByteString cm,
      ByteString ephemeralKey,
      ByteString encCiphertext,
      ByteString outCiphertext,
      ByteString zkproof) {
    this.receiveDescription =
        ReceiveDescription.newBuilder()
            .setValueCommitment(cv)
            .setNoteCommitment(cm)
            .setEpk(ephemeralKey)
            .setCEnc(encCiphertext)
            .setCOut(outCiphertext)
            .setZkproof(zkproof)
            .build();
  }

  public ByteString getValueCommitment() {
    return this.receiveDescription.getValueCommitment();
  }

  public void setValueCommitment(byte[] bytes) {
    this.receiveDescription =
        this.receiveDescription.toBuilder().setValueCommitment(ByteString.copyFrom(bytes)).build();
  }

  public void setValueCommitment(ByteString bytes) {
    this.receiveDescription = this.receiveDescription.toBuilder().setValueCommitment(bytes).build();
  }

  public ByteString getEphemeralKey() {
    return this.receiveDescription.getEpk();
  }

  public void setEpk(byte[] bytes) {
    this.receiveDescription =
        this.receiveDescription.toBuilder().setEpk(ByteString.copyFrom(bytes)).build();
  }

  public void setEpk(ByteString bytes) {
    this.receiveDescription = this.receiveDescription.toBuilder().setEpk(bytes).build();
  }

  public ByteString getEncCiphertext() {
    return this.receiveDescription.getCEnc();
  }

  public void setCEnc(byte[] bytes) {
    this.receiveDescription =
        this.receiveDescription.toBuilder().setCEnc(ByteString.copyFrom(bytes)).build();
  }

  public void setCEnc(ByteString bytes) {
    this.receiveDescription = this.receiveDescription.toBuilder().setCEnc(bytes).build();
  }

  public ByteString getOutCiphertext() {
    return this.receiveDescription.getCOut();
  }

  public void setCOut(byte[] bytes) {
    this.receiveDescription =
        this.receiveDescription.toBuilder().setCOut(ByteString.copyFrom(bytes)).build();
  }

  public void setCOut(ByteString bytes) {
    this.receiveDescription = this.receiveDescription.toBuilder().setCOut(bytes).build();
  }

  public ByteString getCm() {
    return this.receiveDescription.getNoteCommitment();
  }

  public void setNoteCommitment(byte[] bytes) {
    this.receiveDescription =
        this.receiveDescription.toBuilder().setNoteCommitment(ByteString.copyFrom(bytes)).build();
  }

  public void setNoteCommitment(ByteString bytes) {
    this.receiveDescription = this.receiveDescription.toBuilder().setNoteCommitment(bytes).build();
  }

  public ByteString getZkproof() {
    return this.receiveDescription.getZkproof();
  }

  public void setZkproof(byte[] proof) {
    ByteString proof1 = ByteString.copyFrom(proof);
    this.receiveDescription = this.receiveDescription.toBuilder().setZkproof(proof1).build();
  }

  public void setZkproof(ByteString proof) {
    this.receiveDescription = this.receiveDescription.toBuilder().setZkproof(proof).build();
  }

  @Override
  public byte[] getData() {
    return this.receiveDescription.toByteArray();
  }

  @Override
  public ReceiveDescription getInstance() {
    return this.receiveDescription;
  }
}
