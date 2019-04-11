package org.tron.common.zksnark.sapling.transaction;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.protos.Contract.GrothProof;
import org.tron.protos.Contract.SpendDescription;

@Slf4j(topic = "capsule")
public class SpendDescriptionCapsule implements ProtoCapsule<SpendDescription> {

  private SpendDescription spendDescription;

  public SpendDescriptionCapsule() {
  }

  public SpendDescriptionCapsule(final SpendDescription spendDescription) {
    this.spendDescription = spendDescription;
  }

  public SpendDescriptionCapsule(final byte[] data) {
    try {
      this.spendDescription = SpendDescription.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public SpendDescriptionCapsule(
      ByteString cv,
      ByteString anchor,
      ByteString nf,
      ByteString rk,
      GrothProof zkproof,
      ByteString sig) {
    this.spendDescription =
        SpendDescription.newBuilder()
            .setValueCommitment(cv)
            .setAnchor(anchor)
            .setNullifier(nf)
            .setRk(rk)
            .setZkproof(zkproof)
            .setSpendAuthoritySignature(sig)
            .build();
  }

  public ByteString getValueCommitment() {
    return this.spendDescription.getValueCommitment();
  }

  public void setValueCommitment(byte[] bytes) {
    this.spendDescription =
        this.spendDescription.toBuilder().setValueCommitment(ByteString.copyFrom(bytes)).build();
  }

  public void setValueCommitment(ByteString bytes) {
    this.spendDescription = this.spendDescription.toBuilder().setValueCommitment(bytes).build();
  }

  public ByteString getAnchor() {
    return this.spendDescription.getAnchor();
  }

  public void setAnchor(byte[] bytes) {
    this.spendDescription =
        this.spendDescription.toBuilder().setAnchor(ByteString.copyFrom(bytes)).build();
  }

  public void setAnchor(ByteString bytes) {
    this.spendDescription = this.spendDescription.toBuilder().setAnchor(bytes).build();
  }

  public ByteString getNullifier() {
    return this.spendDescription.getNullifier();
  }

  public void setNullifier(byte[] bytes) {
    this.spendDescription =
        this.spendDescription.toBuilder().setNullifier(ByteString.copyFrom(bytes)).build();
  }

  public void setNullifier(ByteString bytes) {
    this.spendDescription = this.spendDescription.toBuilder().setNullifier(bytes).build();
  }

  public ByteString getRk() {
    return this.spendDescription.getRk();
  }

  public void setRk(byte[] bytes) {
    this.spendDescription =
        this.spendDescription.toBuilder().setRk(ByteString.copyFrom(bytes)).build();
  }

  public void setRk(ByteString bytes) {
    this.spendDescription = this.spendDescription.toBuilder().setRk(bytes).build();
  }

  public GrothProof getZkproof() {
    return this.spendDescription.getZkproof();
  }

  public void setZkproof(byte[] proof) {
    GrothProof proof1 = GrothProof.newBuilder().setValues(ByteString.copyFrom(proof)).build();
    this.spendDescription = this.spendDescription.toBuilder().setZkproof(proof1).build();
  }

  public void setZkproof(GrothProof proof) {
    this.spendDescription = this.spendDescription.toBuilder().setZkproof(proof).build();
  }

  public ByteString getSpendAuthSig() {
    return this.spendDescription.getSpendAuthoritySignature();
  }

  public void setSpendAuthoritySignature(ByteString bytes) {
    this.spendDescription = this.spendDescription.toBuilder().setSpendAuthoritySignature(bytes)
        .build();
  }

  @Override
  public byte[] getData() {
    return this.spendDescription.toByteArray();
  }

  @Override
  public SpendDescription getInstance() {
    return this.spendDescription;
  }
}
