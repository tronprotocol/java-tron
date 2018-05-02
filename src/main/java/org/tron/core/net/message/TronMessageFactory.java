package org.tron.core.net.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.overlay.message.MessageFactory;

/**
 * msg factory.
 */
public class TronMessageFactory extends MessageFactory {

  @Override
  public TronMessage create(byte[] data) {
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (Exception e) {
      if (e.getMessage() != null && e.getMessage().contains(MessageFactory.ERR_NO_SUCH_MSG)){
        throw e;
      }else {
        throw new Error(MessageFactory.ERR_PARSE_FAILED + ", type=" + data[0] + ", len=" + data.length);
      }
    }
  }

  private TronMessage create(byte type, byte[] packed) {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    if (receivedTypes == null){
      throw new RuntimeException(MessageFactory.ERR_NO_SUCH_MSG +  ", type=" + type);
    }
    switch (receivedTypes) {
      case TRX:
        return new TransactionMessage(packed);
      case BLOCK:
        return new BlockMessage(packed);
      case TRXS:
        return new TransactionsMessage(packed);
      case BLOCKS:
        return new BlocksMessage(packed);
      case BLOCKHEADERS:
        return new BlockHeadersMessage(packed);
      case INVENTORY:
        return new InventoryMessage(packed);
      case FETCH_INV_DATA:
        return new FetchInvDataMessage(packed);
      case SYNC_BLOCK_CHAIN:
        return new SyncBlockChainMessage(packed);
      case BLOCK_CHAIN_INVENTORY:
        return new ChainInventoryMessage(packed);
      case ITEM_NOT_FOUND:
        return new ItemNotFound();
      case FETCH_BLOCK_HEADERS:
        return new FetchBlockHeadersMessage(packed);
      case BLOCK_INVENTORY:
        return new BlockInventoryMessage(packed);
      case TRX_INVENTORY:
        return new TransactionInventoryMessage(packed);
      default:
        throw new Error(MessageFactory.ERR_NO_SUCH_MSG +  ", " + receivedTypes);
    }
  }
}
