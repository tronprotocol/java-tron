package org.tron.core.ibc.common;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class CrossUtils {

  public static Transaction addSourceTxId(Transaction transaction) {
    Sha256Hash txId = Sha256Hash.of(transaction.getRawData().toByteArray());
    return transaction.toBuilder().setRawData(
        transaction.getRawData().toBuilder().setSourceTxId(txId.getByteString()).build()).build();
  }

  public static Sha256Hash getSourceTxId(Transaction transaction) {
    return Sha256Hash.of(transaction.toBuilder()
        .setRawData(transaction.getRawData().toBuilder().clearSourceTxId().build()).build()
        .getRawData().toByteArray());
  }
}
