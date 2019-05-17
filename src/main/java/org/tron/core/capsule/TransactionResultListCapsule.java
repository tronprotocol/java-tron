package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.Protocol.TransactionResultList;

@Slf4j(topic = "capsule")
public class TransactionResultListCapsule implements ProtoCapsule<TransactionResultList> {
  private TransactionResultList transactionResultList;

  public TransactionResultListCapsule(BlockCapsule blockCapsule) {
    transactionResultList = TransactionResultList.newBuilder().build();
    if (Objects.isNull(blockCapsule)) {
      return;
    }
    TransactionResultList.Builder build = transactionResultList.toBuilder().
        setBlockNumber(blockCapsule.getNum()).setBlockTimeStamp(blockCapsule.getTimeStamp());
    transactionResultList = build.build();
  }

  public TransactionResultListCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionResultList = transactionResultList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoCapsule proto data parse exception");
    }
  }

  public void addTransactionResult(TransactionInfo result) {
    this.transactionResultList = this.transactionResultList.toBuilder().addTransactioninfo(result).build();
  }

  @Override
  public byte[] getData() {
    return transactionResultList.toByteArray();
  }

  @Override
  public TransactionResultList getInstance() {
    return transactionResultList;
  }
}