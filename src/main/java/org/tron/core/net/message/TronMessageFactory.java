package org.tron.core.net.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.overlay.message.MessageFactory;
import org.tron.core.exception.P2pException;

/**
 * msg factory.
 */
public class TronMessageFactory extends MessageFactory {

  @Override
  public TronMessage create(byte[] data) throws Exception {
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (final P2pException e) {
      throw e;
    } catch (final Exception e) {
      throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
          "type=" + data[0] + ", len=" + data.length);
    }
  }

  private TronMessage create(byte type, byte[] packed) throws Exception {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    if (receivedTypes == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          "type=" + type + ", len=" + packed.length);
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
      case TRX_INVENTORY:
        return new TransactionInventoryMessage(packed);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
            receivedTypes.toString() + ", len=" + packed.length);
    }
  }
}
