package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.core.exception.ZksnarkException;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucher;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.PedersenHash;

@Slf4j
public class IncrementalMerkleVoucherCapsule implements ProtoCapsule<IncrementalMerkleVoucher> {

  private IncrementalMerkleVoucher voucher;

  public IncrementalMerkleVoucherCapsule() {
    voucher = IncrementalMerkleVoucher.getDefaultInstance();
  }

  public IncrementalMerkleVoucherCapsule(IncrementalMerkleVoucher voucher) {
    this.voucher = voucher;
  }

  public IncrementalMerkleVoucherCapsule(final byte[] data) {
    try {
      this.voucher = IncrementalMerkleVoucher.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public IncrementalMerkleTreeCapsule getTree() {
    return new IncrementalMerkleTreeCapsule(this.voucher.getTree());
  }

  public void setTree(IncrementalMerkleTreeCapsule merkleTreeCapsule) {
    this.voucher = this.voucher.toBuilder().setTree(merkleTreeCapsule.getInstance()).build();
  }

  public List<PedersenHash> getFilled() {
    return this.voucher.getFilledList();
  }

  public void addFilled(PedersenHash value) {
    this.voucher = this.voucher.toBuilder().addFilled(value).build();
  }

  public IncrementalMerkleTreeCapsule getCursor() {
    return new IncrementalMerkleTreeCapsule(this.voucher.getCursor());
  }

  public void setCursor(IncrementalMerkleTreeCapsule cursor) {
    this.voucher = this.voucher.toBuilder().setCursor(cursor.getInstance()).build();
  }

  public void clearCursor() {
    this.voucher = this.voucher.toBuilder().clearCursor().build();
  }

  public long getCursorDepth() {
    return this.voucher.getCursorDepth();
  }

  public void setCursorDepth(long cursorDepth) {
    this.voucher = this.voucher.toBuilder().setCursorDepth(cursorDepth).build();
  }

  public void resetRt() throws ZksnarkException {
    this.voucher =
        this.voucher.toBuilder().setRt(toMerkleVoucherContainer().root().getContent()).build();
  }

  public OutputPoint getOutputPoint() {
    return this.voucher.getOutputPoint();
  }

  public void setOutputPoint(ByteString hash, int index) {
    this.voucher =
        this.voucher
            .toBuilder()
            .setOutputPoint(OutputPoint.newBuilder().setHash(hash).setIndex(index).build())
            .build();
  }

  @Override
  public byte[] getData() {
    return this.voucher.toByteArray();
  }

  @Override
  public IncrementalMerkleVoucher getInstance() {
    return this.voucher;
  }

  public IncrementalMerkleVoucherContainer toMerkleVoucherContainer() {
    return new IncrementalMerkleVoucherContainer(this);
  }
}
