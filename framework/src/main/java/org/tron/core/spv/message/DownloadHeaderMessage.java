package org.tron.core.spv.message;

import com.google.protobuf.ByteString;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol.DownloadHeader;


public class DownloadHeaderMessage extends TronMessage {

  protected DownloadHeader downloadHeader;

  public DownloadHeaderMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.DOWNLOAD_BLOCK_HEADER.asByte();
    this.downloadHeader = DownloadHeader.parseFrom(data);
  }

  public DownloadHeaderMessage(DownloadHeader downloadHeader) {
    this.downloadHeader = downloadHeader;
    this.type = MessageTypes.DOWNLOAD_BLOCK_HEADER.asByte();
    this.data = downloadHeader.toByteArray();
  }


  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public DownloadHeader getDownloadHeader() {
    return downloadHeader;
  }

  public BlockId getHeadBlockId() {
    return new BlockId(downloadHeader.getHeaderId().getHash(),
        downloadHeader.getHeaderId().getNumber());
  }

  public BlockId getSolidityBlockId() {
    return new BlockId(downloadHeader.getSolidityId().getHash(),
        downloadHeader.getSolidityId().getNumber());
  }

  public ByteString getUuid() {
    return downloadHeader.getUuid();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(super.toString())
        .append(", solidity hash: ").append(downloadHeader.getSolidityId())
        .append(", header hash: ").append(downloadHeader.getHeaderId());
    return builder.toString();
  }

}
