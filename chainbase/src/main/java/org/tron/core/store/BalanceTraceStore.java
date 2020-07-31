package org.tron.core.store;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockBalanceTraceCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.TransactionBalanceTrace;

import java.util.Objects;


@Component
@Slf4j(topic = "DB")
public class BalanceTraceStore extends TronStoreWithRevoking<BlockBalanceTraceCapsule>  {

  @Getter
  private BlockCapsule.BlockId currentBlockId;

  @Getter
  private Sha256Hash currentTransactionId;

  @Getter
  @Setter
  private BlockBalanceTraceCapsule currentBlockBalanceTraceCapsule;

  @Getter
  @Setter
  private TransactionBalanceTrace currentTransactionBalanceTrace;

  protected BalanceTraceStore(@Value("balance-trace") String dbName) {
    super(dbName);
  }

  public void setCurrentTransactionId(TransactionCapsule transactionCapsule) {
    if (currentBlockId == null) {
      return;
    }
    currentTransactionId = transactionCapsule.getTransactionId();
  }

  public void setCurrentBlockId(BlockCapsule blockCapsule) {
    currentBlockId = blockCapsule.getBlockId();
  }

  public void resetCurrentTransactionTrace() {
    if (currentBlockId == null) {
      return;
    }

    if (!CollectionUtils.isEmpty(currentTransactionBalanceTrace.getOperationList())) {
      currentBlockBalanceTraceCapsule.addTransactionBalanceTrace(currentTransactionBalanceTrace);
    }

    currentTransactionId = null;
    currentTransactionBalanceTrace = null;
  }

  public void resetCurrentBlockTrace() {
    putBlockBalanceTrace(currentBlockBalanceTraceCapsule);
    currentBlockId = null;
    currentBlockBalanceTraceCapsule = null;
  }

  public void initCurrentBlockBalanceTrace(BlockCapsule blockCapsule) {
    setCurrentBlockId(blockCapsule);
    currentBlockBalanceTraceCapsule = new BlockBalanceTraceCapsule(blockCapsule);
  }

  public void initCurrentTransactionBalanceTrace(TransactionCapsule transactionCapsule) {
    if (currentBlockId == null) {
      return;
    }

    setCurrentTransactionId(transactionCapsule);
    currentTransactionBalanceTrace = TransactionBalanceTrace.newBuilder()
        .setTransactionIdentifier(transactionCapsule.getTransactionId().getByteString())
        .setType(transactionCapsule.getInstance().getRawData().getContract(0).getType().name())
        .build();
  }

  public void updateCurrentTransactionStatus(String status) {
    if (currentBlockId == null) {
      return;
    }

    currentTransactionBalanceTrace = currentTransactionBalanceTrace.toBuilder()
        .setType(status)
        .build();
  }

  private void putBlockBalanceTrace(BlockBalanceTraceCapsule blockBalanceTrace) {
    put(ByteArray.fromLong(getCurrentBlockId().getNum()), blockBalanceTrace);
  }

  public BlockBalanceTraceCapsule getBlockBalanceTrace(BlockCapsule.BlockId blockId) throws BadItemException {
    long blockNumber = blockId.getNum();
    if (blockNumber == -1) {
      return null;
    }
    byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(blockNumber));
    if (Objects.isNull(value)) {
      return null;
    }

    BlockBalanceTraceCapsule blockBalanceTraceCapsule = new BlockBalanceTraceCapsule(value);
    if (Objects.isNull(blockBalanceTraceCapsule.getInstance())) {
      return null;
    }

    return blockBalanceTraceCapsule;
  }

  public TransactionBalanceTrace getTransactionBalanceTrace(BlockCapsule.BlockId blockId, Sha256Hash transactionId) throws BadItemException {
    long blockNumber = blockId.getNum();
    if (blockNumber == -1) {
      return null;
    }
    byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(blockNumber));
    if (Objects.isNull(value)) {
      return null;
    }

    BlockBalanceTraceCapsule blockBalanceTraceCapsule = new BlockBalanceTraceCapsule(value);
    if (Objects.isNull(blockBalanceTraceCapsule.getInstance())) {
      return null;
    }

    for (TransactionBalanceTrace transactionBalanceTrace : blockBalanceTraceCapsule.getInstance().getTransactionBalanceTraceList()) {
      if (transactionBalanceTrace.getTransactionIdentifier().equals(transactionId.getByteString())) {
        return transactionBalanceTrace;
      }
    }

    return null;
  }
}
