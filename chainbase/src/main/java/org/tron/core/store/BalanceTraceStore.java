package org.tron.core.store;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.TransactionBalanceTrace;

import java.util.Objects;


@Component
@Slf4j(topic = "DB")
public class BalanceTraceStore extends TronStoreWithRevoking<BlockBalanceTraceCapsule>  {

  private final static byte[] BLOCK_PREFIX = "block:".getBytes();
  private final static byte[] ACCOUNT_PREFIX = "account:".getBytes();

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

  @Autowired
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
    recordCurrentBlockNumberToAllAccount();
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
        .setStatus(StringUtils.isEmpty(status) ? "SUCCESS" : status)
        .build();
  }

  private void putBlockBalanceTrace(BlockBalanceTraceCapsule blockBalanceTrace) {
    byte[] key = Bytes.concat(BLOCK_PREFIX, ByteArray.fromLong(getCurrentBlockId().getNum()));
    put(key, blockBalanceTrace);
  }

  public BlockBalanceTraceCapsule getBlockBalanceTrace(BlockCapsule.BlockId blockId) throws BadItemException {
    long blockNumber = blockId.getNum();
    if (blockNumber == -1) {
      return null;
    }

    byte[] key = Bytes.concat(BLOCK_PREFIX, ByteArray.fromLong(blockNumber));
    byte[] value = revokingDB.getUnchecked(key);
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

    byte[] key = Bytes.concat(BLOCK_PREFIX, ByteArray.fromLong(blockNumber));
    byte[] value = revokingDB.getUnchecked(key);
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

  public void recordCurrentBlockNumberToAllAccount() {
    if (CollectionUtils.isEmpty(getCurrentBlockBalanceTraceCapsule().getTransactions())) {
      return;
    }

    getCurrentBlockBalanceTraceCapsule()
        .getTransactions()
        .stream()
        .map(TransactionBalanceTrace::getOperationList)
        .flatMap(List::stream)
        .map(TransactionBalanceTrace.Operation::getAddress)
        .distinct()
        .map(ByteString::toByteArray)
        .forEach(this::recordCurrentBlockNumber);
  }

  public void recordCurrentBlockNumber(byte[] address) {
    BalanceContract.BlockNumber blockNumber = getAllBlockNumberByAddress(address);
    blockNumber = blockNumber.toBuilder().addNumber(getCurrentBlockId().getNum()).build();

    byte[] key = Bytes.concat(ACCOUNT_PREFIX, address);
    revokingDB.put(key, blockNumber.toByteArray());
  }

  public BalanceContract.BlockNumber getAllBlockNumberByAddress(byte[] address) {
    byte[] key = Bytes.concat(ACCOUNT_PREFIX, address);
    byte[] value = revokingDB.getUnchecked(key);

    BalanceContract.BlockNumber blockNumber = BalanceContract.BlockNumber.newBuilder().build();
    if (!ArrayUtils.isEmpty(value)) {
      try {
        blockNumber = BalanceContract.BlockNumber.parseFrom(value);
      } catch (InvalidProtocolBufferException e) {
        logger.error(e.getMessage(), e);
      }
    }

    return blockNumber;
  }
}
