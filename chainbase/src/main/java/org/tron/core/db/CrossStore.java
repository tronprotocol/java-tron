package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.CrossMessage;

@Slf4j
@Component
public class CrossStore extends TronDatabase<byte[]> {

  public CrossStore() {
    super("cross-database");
  }

  @Override
  public void put(byte[] key, byte[] item) {
    dbSource.putData(key, item);
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public byte[] get(byte[] key) {
    return dbSource.getData(key);
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  public void saveSendCrossMsg(Sha256Hash txId, CrossMessage crossMessage) {
    this.put(buildSendKey(txId), crossMessage.toByteArray());
  }

  public void removeSendCrossMsg(Sha256Hash txId) {
    delete(buildSendKey(txId));
  }

  public CrossMessage getSendCrossMsg(Sha256Hash txId) throws InvalidProtocolBufferException {
    byte[] data = get(buildSendKey(txId));
    if (!ByteUtil.isNullOrZeroArray(data)) {
      return CrossMessage.parseFrom(data);
    }
    return null;
  }

  public void saveReceiveCrossMsg(Sha256Hash txId, CrossMessage crossMessage) {
    this.put(buildReceiveKey(txId), crossMessage.toByteArray());
  }

  public CrossMessage getReceiveCrossMsg(Sha256Hash txId) throws InvalidProtocolBufferException {
    if (txId == null) {
      return null;
    }
    byte[] data = get(buildReceiveKey(txId));
    if (!ByteUtil.isNullOrZeroArray(data)) {
      return CrossMessage.parseFrom(data);
    }
    return null;
  }

  public CrossMessage getReceiveCrossMsgUnEx(Sha256Hash txId) {
    if (txId == null) {
      return null;
    }
    byte[] data = get(buildReceiveKey(txId));
    if (!ByteUtil.isNullOrZeroArray(data)) {
      try {
        return CrossMessage.parseFrom(data);
      } catch (InvalidProtocolBufferException e) {
        logger.error("{}", e.getMessage());
      }
    }
    return null;
  }

  public void saveTokenMapping(long sourceToken, long descToken) {
    this.put(buildTokenKey(sourceToken), ByteArray.fromLong(descToken));
  }

  public Long getDescTokenFromMapping(long sourceToken) {
    byte[] data = get(buildTokenKey(sourceToken));
    if (!ByteUtil.isNullOrZeroArray(data)) {
      return ByteArray.toLong(data);
    }
    return null;
  }

  public void saveOutTokenCount(long tokenId, long count) {
    this.put(buildOutKey(tokenId), ByteArray.fromLong(count));
  }

  public long getOutTokenCount(long tokenId) {
    byte[] data = get(buildOutKey(tokenId));
    if (!ByteUtil.isNullOrZeroArray(data)) {
      return ByteArray.toLong(data);
    } else {
      return 0;
    }
  }

  public void saveInTokenCount(long tokenId, long count) {
    this.put(buildInKey(tokenId), ByteArray.fromLong(count));
  }

  public long getInTokenCount(long tokenId) {
    byte[] data = get(buildInKey(tokenId));
    if (!ByteUtil.isNullOrZeroArray(data)) {
      return ByteArray.toLong(data);
    } else {
      return 0;
    }
  }

  private byte[] buildSendKey(Sha256Hash txId) {
    return ("send_" + txId.toString()).getBytes();
  }

  private byte[] buildReceiveKey(Sha256Hash txId) {
    return ("receive_" + txId.toString()).getBytes();
  }

  private byte[] buildTokenKey(long tokenId) {
    return ("token_" + tokenId).getBytes();
  }

  private byte[] buildOutKey(long tokenId) {
    return ("out_" + tokenId).getBytes();
  }

  private byte[] buildInKey(long tokenId) {
    return ("in_" + tokenId).getBytes();
  }
}
