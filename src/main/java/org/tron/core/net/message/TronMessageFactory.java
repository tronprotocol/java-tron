package org.tron.core.net.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.overlay.message.MessageFactory;

/**
 * msg factory.
 */
public class TronMessageFactory extends MessageFactory {

  @Override
  protected TronMessage create(byte type, byte[] packed) {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    switch (receivedTypes) {
      case TRX:
        return new TransactionMessage(packed);
      case TRXS:
        return new TransactionsMessage(packed);
      case BLOCK:
        return new BlockMessage(packed);
      case BLOCKS:
        return new BlocksMessage(packed);
      case BLOCKHEADERS:
        return new BlockHeadersMessage(packed);
      case INVENTORY:
        return new InventoryMessage(packed);
      default:
        throw new IllegalArgumentException("No such message");
    }
  }

  @Override
  public TronMessage create(byte[] data) {
    byte type = data[0];
    byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
    return create(type, rawData);
  }
}
