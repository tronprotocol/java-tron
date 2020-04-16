package org.tron.core.net.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.overlay.message.MessageFactory;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.exception.P2pException;

/**
 * msg factory.
 */
public class PbftMessageFactory extends MessageFactory {

  private static String LEN = ", len=";
  private static String TYPE = "type=";

  @Override
  public PbftBaseMessage create(byte[] data) throws Exception {
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (final P2pException e) {
      throw e;
    } catch (final Exception e) {
      throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
          TYPE + data[0] + LEN + data.length + ", error msg: " + e.getMessage());
    }
  }

  private PbftBaseMessage create(byte type, byte[] packed) throws Exception {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    if (receivedTypes == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          TYPE + type + LEN + packed.length);
    }
    switch (receivedTypes) {
      case PBFT_MSG:
        return new PbftMessage(packed);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
            receivedTypes.toString() + LEN + packed.length);
    }
  }
}