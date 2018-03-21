package org.tron.core.net.message;

public class MessageRegistry {

  public static Message getMessageByKey(String key, byte[] content) {

    switch (MessageTypes.valueOf(key)) {
      case BLOCK:
        return new BlockMessage(content);
      case TRX:
        return new TransactionMessage(content);
      case SYNC_BLOCK_CHAIN:
        return new SyncBlockChainMessage(content);
      case FETCH_INV_DATA:
        return new FetchInvDataMessage(content);
      case BLOCK_INVENTORY:
        return new BlockInventoryMessage(content);
      case BLOCK_CHAIN_INVENTORY:
        return new ChainInventoryMessage(content);
      case INVENTORY:
        return new InventoryMessage(content);
    }

    throw new IllegalArgumentException("Unknown message");
  }
}
