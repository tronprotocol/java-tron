/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.overlay.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.MessageTypes;

public class P2pMessageFactory extends MessageFactory {

  @Override
  public P2pMessage create(byte[] data) throws Exception {
    if (data.length <= 1) {
      throw new P2pException(TypeEnum.MESSAGE_WITH_WRONG_LENGTH,
          "messageType=" + (data.length == 1 ? data[0] : "unknown"));
    }
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (Exception e) {
      if (e instanceof P2pException) {
        throw e;
      } else {
        throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
            "type=" + data[0] + ", len=" + data.length);
      }
    }
  }

  private P2pMessage create(byte type, byte[] rawData) throws Exception {
    MessageTypes messageType = MessageTypes.fromByte(type);
    if (messageType == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          "type=" + type + ", len=" + rawData.length);
    }
    switch (messageType) {
      case P2P_HELLO:
        return new HelloMessage(type, rawData);
      case P2P_DISCONNECT:
        return new DisconnectMessage(type, rawData);
      case P2P_PING:
        return new PingMessage(type, rawData);
      case P2P_PONG:
        return new PongMessage(type, rawData);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, messageType.toString());
    }
  }
}
