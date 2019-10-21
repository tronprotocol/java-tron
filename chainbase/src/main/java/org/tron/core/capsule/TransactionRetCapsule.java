package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionRet;

@Slf4j(topic = "capsule")
public class TransactionRetCapsule implements ProtoCapsule<TransactionRet> {

  private TransactionRet transactionRet;

  public TransactionRetCapsule(BlockCapsule blockCapsule) {
    transactionRet = TransactionRet.newBuilder().build();
    if (Objects.isNull(blockCapsule)) {
      return;
    }
    TransactionRet.Builder build = transactionRet.toBuilder()
        .setBlockNumber(blockCapsule.getNum()).setBlockTimeStamp(blockCapsule.getTimeStamp());
    transactionRet = build.build();
  }

  // only for test
  public TransactionRetCapsule() {
    transactionRet = TransactionRet.newBuilder().build();
  }

  public TransactionRetCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionRet = transactionRet.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoCapsule proto data parse exception");
    }
  }

  public void addTransactionInfo(TransactionInfo result) {
    this.transactionRet = this.transactionRet.toBuilder().addTransactioninfo(result).build();
  }

  @Override
  public byte[] getData() {
    if (Objects.isNull(transactionRet)) {
      return null;
    }
    return transactionRet.toByteArray();
  }

  @Override
  public TransactionRet getInstance() {
    return transactionRet;
  }
}