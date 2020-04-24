package org.tron.core.ibc.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.overlay.server.Channel;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class CrossUtils {

  public static Transaction addSourceTxId(Transaction transaction) {
    Sha256Hash txId = Sha256Hash.of(transaction.getRawData().toByteArray());
    return transaction.toBuilder().setRawData(
        transaction.getRawData().toBuilder().setSourceTxId(txId.getByteString()).build()).build();
  }

  public static Sha256Hash getAddSourceTxId(Transaction transaction) {
    Sha256Hash txId = Sha256Hash.of(transaction.getRawData().toByteArray());
    Transaction ts = transaction.toBuilder().setRawData(
        transaction.getRawData().toBuilder().setSourceTxId(txId.getByteString()).build()).build();
    return Sha256Hash.of(ts.getRawData().toByteArray());
  }

  public static Sha256Hash getSourceTxId(Transaction transaction) {
    return Sha256Hash.of(transaction.toBuilder()
        .setRawData(transaction.getRawData().toBuilder().clearSourceTxId().build()).build()
        .getRawData().toByteArray());
  }

  public static Sha256Hash getSourceMerkleTxHash(Transaction transaction) {
    return Sha256Hash.of(transaction.toBuilder()
        .setRawData(transaction.getRawData().toBuilder().clearSourceTxId().build()).build()
        .toByteArray());
  }

  public static Map<ByteArrayWrapper, Channel> listToMap(List<PeerConnection> peerConnections) {
    Map<ByteArrayWrapper, Channel> channelMap = new HashMap<>();
    if (CollectionUtils.isEmpty(peerConnections)) {
      return channelMap;
    }
    peerConnections.forEach(peerConnection -> {
      channelMap.put(peerConnection.getNodeIdWrapper(), peerConnection);
    });
    return channelMap;
  }
}

