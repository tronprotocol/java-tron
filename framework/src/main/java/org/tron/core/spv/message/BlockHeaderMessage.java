package org.tron.core.spv.message;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Items;

public class BlockHeaderMessage extends TronMessage {

  private Items items;

  private List<BlockHeader> blockHeaders;

  public BlockHeaderMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.BLOCKS.asByte();
    Items items = Items.parseFrom(getCodedInputStream(data));
    blockHeaders = items.getBlockHeadersList();

    if (isFilter() && CollectionUtils.isNotEmpty(blockHeaders)) {
      compareBytes(data, items.toByteArray());
    }
  }

  public BlockHeaderMessage(List<BlockCapsule> blockCapsules, ByteString uuid) {
    blockHeaders = new ArrayList<>();
    for (BlockCapsule blockCapsule : blockCapsules) {
      blockHeaders.add(blockCapsule.getInstance().getBlockHeader());
    }
    Items.Builder builder = Items.newBuilder();
    builder.addAllBlockHeaders(blockHeaders);
    items = builder.build();
  }

  public List<BlockHeader> getBlockHeaders() {
    return blockHeaders;
  }

  public Items getItems() {
    return items;
  }

  @Override
  public String toString() {
    return super.toString() + "size: " + (CollectionUtils.isNotEmpty(blockHeaders) ? blockHeaders
        .size() : 0);
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
