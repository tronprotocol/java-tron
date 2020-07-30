package org.tron.core.store;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
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

  private static final byte[] CURRENT_BLOCK_ID = "current_block_id".getBytes();
  private static final byte[] CURRENT_TRANSACTION_ID = "current_transaction_id".getBytes();
  private static final byte[] CURRENT_OWNER = "owner".getBytes();
  private static final byte[] EMPTY = new byte[0];

  protected BalanceTraceStore(@Value("balance-trace") String dbName) {
    super(dbName);
  }

  public void setCurrentTransactionId(Sha256Hash transactionId) {
    revokingDB.put(CURRENT_TRANSACTION_ID, transactionId.getBytes());
  }

  public void setCurrentBlockId(Sha256Hash blockId) {
    revokingDB.put(CURRENT_BLOCK_ID, blockId.getBytes());
  }

  public void setCurrentOwner(TransactionCapsule transactionCapsule) {
    Protocol.Transaction.Contract contract = transactionCapsule.getInstance().getRawData().getContractList().get(0);
    int permissionId = contract.getPermissionId();
    byte[] owner = TransactionCapsule.getOwner(contract);
    revokingDB.put(CURRENT_OWNER, owner);
  }

  public Sha256Hash getCurrentTransactionId() {
    return Sha256Hash.wrap(revokingDB.getUnchecked(CURRENT_TRANSACTION_ID));
  }

  public Sha256Hash getCurrentBlockId() {
    return Sha256Hash.wrap(revokingDB.getUnchecked(CURRENT_BLOCK_ID));
  }

  public ByteString getCurrentOwner() {
    return ByteString.copyFrom(revokingDB.getUnchecked(CURRENT_OWNER));
  }

  public void resetCurrentTransactionId() {
    revokingDB.put(CURRENT_TRANSACTION_ID, EMPTY);
  }

  public void resetCurrentBlockId() {
    revokingDB.put(CURRENT_BLOCK_ID, EMPTY);
  }

  public void initCurrentBlockBalanceTrace(BlockCapsule blockCapsule) {
    BlockBalanceTraceCapsule blockBalanceTraceCapsule = new BlockBalanceTraceCapsule(blockCapsule);
    setCurrentBlockId(blockCapsule.getBlockId());
    putBlockBalanceTrace(blockBalanceTraceCapsule);
  }

  public void updateCurrentTransactionBalanceTrace(String type, String status) {
    TransactionBalanceTrace transactionBalanceTrace = null;
    BlockBalanceTraceCapsule balanceTraceCapsule = null;
    try {
      balanceTraceCapsule = getCurrentBlockBalanceTrace();
    } catch (BadItemException e) {
      logger.error(e.getMessage(), e);
    }

    if (balanceTraceCapsule == null) {
      return;
    }

    int index = 0;
    for(; index < balanceTraceCapsule.getInstance().getTransactionBalanceTraceCount(); index++) {
      TransactionBalanceTrace tmp = balanceTraceCapsule.getInstance().getTransactionBalanceTrace(index);
      if (tmp.getTransactionIdentifier().equals(getCurrentTransactionId().getByteString())) {
        transactionBalanceTrace = tmp;
        break;
      }
    }

    if (transactionBalanceTrace != null) {
      transactionBalanceTrace = transactionBalanceTrace.toBuilder()
          .setStatus(type)
          .setType(status)
          .build();
      BalanceContract.BlockBalanceTrace blockBalanceTrace =
          balanceTraceCapsule.getInstance().toBuilder().setTransactionBalanceTrace(index, transactionBalanceTrace).build();
      balanceTraceCapsule = new BlockBalanceTraceCapsule(blockBalanceTrace);
      putBlockBalanceTrace(balanceTraceCapsule);
    }
  }

  public void putBlockBalanceTrace(BlockBalanceTraceCapsule blockBalanceTrace) {
    put(getCurrentBlockId().getBytes(), blockBalanceTrace);
  }

  public BlockBalanceTraceCapsule getCurrentBlockBalanceTrace() throws BadItemException {
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(getCurrentBlockId());
    return getBlockBalanceTrace(blockId);
  }

  public TransactionBalanceTrace getCurrentTransactionBalanceTrace() throws BadItemException {
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(getCurrentBlockId());
    return getTransactionBalanceTrace(blockId, getCurrentTransactionId());
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
