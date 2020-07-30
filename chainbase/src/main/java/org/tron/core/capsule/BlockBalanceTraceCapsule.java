package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract.BlockBalanceTrace;
import org.tron.protos.contract.BalanceContract.TransactionBalanceTrace;

import java.util.Objects;

public class BlockBalanceTraceCapsule implements ProtoCapsule<BlockBalanceTrace> {
  private BlockBalanceTrace balanceTrace;

  public BlockBalanceTraceCapsule() {
    balanceTrace = BlockBalanceTrace.newBuilder().build();
  }

  public BlockBalanceTraceCapsule(BlockCapsule blockCapsule) {
    this();
    balanceTrace = balanceTrace.toBuilder()
        .setBlockNumber(blockCapsule.getNum())
        .setTimestamp(blockCapsule.getTimeStamp())
        .build();
  }

  public BlockBalanceTraceCapsule(byte[] data) throws BadItemException {
    try {
      this.balanceTrace = BlockBalanceTrace.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoCapsule proto data parse exception");
    }
  }

  public BlockBalanceTraceCapsule(BlockBalanceTrace blockBalanceTrace) {
    this.balanceTrace = blockBalanceTrace;
  }

  public void addTransactionBalanceTrace(TransactionBalanceTrace transactionBalanceTrace) {
    balanceTrace = balanceTrace.toBuilder()
        .addTransactionBalanceTrace(transactionBalanceTrace)
        .build();
  }

  public void setTransactionBalanceTrace(int index, TransactionBalanceTrace transactionBalanceTrace) {
    balanceTrace = balanceTrace.toBuilder()
        .setTransactionBalanceTrace(index, transactionBalanceTrace)
        .build();
  }

  @Override
  public byte[] getData() {
    if (Objects.isNull(balanceTrace)) {
      return null;
    }
    return balanceTrace.toByteArray();
  }

  @Override
  public BlockBalanceTrace getInstance() {
    return balanceTrace;
  }
}
