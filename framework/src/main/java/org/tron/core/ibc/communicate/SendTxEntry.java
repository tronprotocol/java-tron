package org.tron.core.ibc.communicate;

import com.google.protobuf.ByteString;
import org.tron.common.utils.Sha256Hash;

public class SendTxEntry {

  private Sha256Hash txHash;
  private long time;
  private long height;
  private ByteString toChainId;

  public SendTxEntry(Sha256Hash txHash, long time, long height,
      ByteString toChainId) {
    this.txHash = txHash;
    this.time = time;
    this.height = height;
    this.toChainId = toChainId;
  }

  public Sha256Hash getTxHash() {
    return txHash;
  }

  public SendTxEntry setTxHash(Sha256Hash txHash) {
    this.txHash = txHash;
    return this;
  }

  public long getTime() {
    return time;
  }

  public SendTxEntry setTime(long time) {
    this.time = time;
    return this;
  }

  public long getHeight() {
    return height;
  }

  public SendTxEntry setHeight(long height) {
    this.height = height;
    return this;
  }

  public ByteString getToChainId() {
    return toChainId;
  }

  public SendTxEntry setToChainId(ByteString toChainId) {
    this.toChainId = toChainId;
    return this;
  }
}
