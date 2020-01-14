package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.message.Message;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;

public class BlockHeaderUpdatedNoticeMessage extends TronMessage {

  private Protocol.BlockHeaderUpdatedNotice blockHeaderUpdatedNotice;

  public BlockHeaderUpdatedNoticeMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_UPDATED_NOTICE.asByte(), packed);
    blockHeaderUpdatedNotice = Protocol.BlockHeaderUpdatedNotice.parseFrom(packed);
  }

  public byte[] getChainId() {
    return blockHeaderUpdatedNotice.getChainId().toByteArray();
  }

  public long getCurrentBlockHeight() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader().getBlockHeader().getRawData().getNumber();
  }

  public Protocol.BlockHeader getBlockHeader() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader().getBlockHeader();
  }

  public Protocol.SignedBlockHeader getSignedBlockHeader() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
