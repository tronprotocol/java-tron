package org.tron.core.ibc.spv.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol;

public class BlockHeaderUpdatedNoticeMessage extends TronMessage {

  private Protocol.BlockHeaderUpdatedNotice blockHeaderUpdatedNotice;

  public BlockHeaderUpdatedNoticeMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_UPDATED_NOTICE.asByte(), packed);
    blockHeaderUpdatedNotice = Protocol.BlockHeaderUpdatedNotice.parseFrom(packed);
  }

  public BlockHeaderUpdatedNoticeMessage(
      Protocol.BlockHeaderUpdatedNotice blockHeaderUpdatedNotice) {
    this.blockHeaderUpdatedNotice = blockHeaderUpdatedNotice;
    super.type = MessageTypes.HEADER_UPDATED_NOTICE.asByte();
    super.data = blockHeaderUpdatedNotice.toByteArray();
  }

  public byte[] getChainId() {
    return blockHeaderUpdatedNotice.getChainId().toByteArray();
  }

  public long getCurrentBlockHeight() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader().getBlockHeader().getRawData()
        .getNumber();
  }

  public Protocol.BlockHeader getBlockHeader() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader().getBlockHeader();
  }

  public Protocol.SignedBlockHeader getSignedBlockHeader() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader();
  }

  public List<ByteString> getAllSrsSignature() {
    return blockHeaderUpdatedNotice.getSignedBlockHeader().getSrsSignatureList();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
