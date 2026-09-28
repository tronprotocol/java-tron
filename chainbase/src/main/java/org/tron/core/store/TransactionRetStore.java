package org.tron.core.store;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.util.Objects;
import java.util.OptionalLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.db.TransactionStore;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;

@Slf4j(topic = "DB")
@Component
public class TransactionRetStore extends TronStoreWithRevoking<TransactionRetCapsule> {

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  public TransactionRetStore(@Value("transactionRetStore") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionRetCapsule item) {
    if (BooleanUtils.toBoolean(CommonParameter.getInstance()
        .getStorage().getTransactionHistorySwitch())) {
      super.put(key, item);
    }
  }

  /**
   * Lowest block number that has receipts, or empty when the store has none. On a LiteNode
   * this is generally above the lowest block: a snapshot ships block bodies but no receipts.
   *
   * <p>Startup use only — must run before any session is built. With in-flight snapshot
   * layers, {@code getNext} does not merge deletions correctly.
   */
  public OptionalLong getLowestBlockNum() {
    return revokingDB.getNext(ByteArray.fromLong(0), 1).keySet().stream()
        .mapToLong(Longs::fromByteArray).findFirst();
  }

  public TransactionInfoCapsule getTransactionInfo(byte[] key) throws BadItemException {
    long blockNumber = transactionStore.getBlockNumber(key);
    if (blockNumber == -1) {
      return null;
    }
    byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(blockNumber));
    if (Objects.isNull(value)) {
      return null;
    }

    TransactionRetCapsule result = new TransactionRetCapsule(value);
    if (Objects.isNull(result.getInstance())) {
      return null;
    }

    ByteString id = ByteString.copyFrom(key);
    for (TransactionInfo transactionResultInfo : result.getInstance().getTransactioninfoList()) {
      if (transactionResultInfo.getId().equals(id)) {
        Protocol.ResourceReceipt receipt = transactionResultInfo.getReceipt();
        // If query a result with dirty origin usage in receipt, we just reset it.
        if (receipt.getEnergyUsageTotal() == 0 && receipt.getOriginEnergyUsage() > 0) {
          transactionResultInfo =
              transactionResultInfo.toBuilder()
                  .setReceipt(
                      receipt.toBuilder()
                          .clearOriginEnergyUsage()
                          .build())
                  .build();
        }
        return new TransactionInfoCapsule(transactionResultInfo);
      }
    }
    return null;
  }

  public TransactionRetCapsule getTransactionInfoByBlockNum(byte[] key) throws BadItemException {

    byte[] value = revokingDB.getUnchecked(key);
    if (Objects.isNull(value)) {
      return null;
    }

    return new TransactionRetCapsule(value);
  }

}
