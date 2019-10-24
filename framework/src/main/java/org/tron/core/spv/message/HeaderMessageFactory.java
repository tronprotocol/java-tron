package org.tron.core.spv.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.overlay.message.MessageFactory;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;

/**
 * msg factory.
 */
public class HeaderMessageFactory extends MessageFactory {

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
          "type=" + data[0] + ", len=" + data.length + ", error msg: " + e.getMessage());
    }
  }

  private TronMessage create(byte type, byte[] packed) throws Exception {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    if (receivedTypes == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          "type=" + type + ", len=" + packed.length);
    }
    switch (receivedTypes) {
      case DOWNLOAD_BLOCK_HEADER:
        return new DownloadHeaderMessage(packed);
      case BLOCK_HEADERS:
        return new BlockHeaderMessage(packed);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
            receivedTypes.toString() + ", len=" + packed.length);
    }
  }
}
