package org.tron.core.net.message;

/**
 * msg factory.
 */
public class MessageFactory {

  /**
   * create msg.
   * @param type msg type
   * @param packed msg data
   * @return
   */
  public Message create(byte type, byte[] packed) {
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
}
