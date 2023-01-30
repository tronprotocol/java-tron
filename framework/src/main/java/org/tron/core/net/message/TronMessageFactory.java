package org.tron.core.net.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.exception.P2pException;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.message.keepalive.PongMessage;
import org.tron.core.net.message.pbft.PbftCommitMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;

public class TronMessageFactory {

  private static final String DATA_LEN = ", len=";

  public static TronMessage create(byte[] data) throws Exception {
    boolean isException = false;
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (final P2pException e) {
      isException = true;
      throw e;
    } catch (final Exception e) {
      isException = true;
      throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
          "type=" + data[0] + DATA_LEN + data.length + ", error msg: " + e.getMessage());
    } finally {
      if (isException) {
        MetricsUtil.counterInc(MetricsKey.NET_ERROR_PROTO_COUNT);
      }
    }
  }

  private static TronMessage create(byte type, byte[] packed) throws Exception {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    if (receivedTypes == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          "type=" + type + DATA_LEN + packed.length);
    }
    switch (receivedTypes) {
      case P2P_HELLO:
        return new HelloMessage(packed);
      case P2P_DISCONNECT:
        return new DisconnectMessage(packed);
      case P2P_PING:
        return new PingMessage(packed);
      case P2P_PONG:
        return new PongMessage(packed);
      case TRX:
        return new TransactionMessage(packed);
      case BLOCK:
        return new BlockMessage(packed);
      case TRXS:
        return new TransactionsMessage(packed);
      case INVENTORY:
        return new InventoryMessage(packed);
      case FETCH_INV_DATA:
        return new FetchInvDataMessage(packed);
      case SYNC_BLOCK_CHAIN:
        return new SyncBlockChainMessage(packed);
      case BLOCK_CHAIN_INVENTORY:
        return new ChainInventoryMessage(packed);
      case PBFT_COMMIT_MSG:
        return new PbftCommitMessage(packed);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
            receivedTypes.toString() + DATA_LEN + packed.length);
    }
  }
}
