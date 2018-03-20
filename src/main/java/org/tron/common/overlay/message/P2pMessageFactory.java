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
import org.tron.core.net.message.MessageTypes;

public class P2pMessageFactory extends MessageFactory {

  @Override
  protected Message create(byte type, byte[] rawData) {
    MessageTypes messageType = MessageTypes.fromByte(type);
    switch (messageType) {
      case P2P_HELLO:
        break;
      case P2P_DISCONNECT:
        break;
      case P2P_PING:
        break;
      case P2P_PONG:
        break;
      default:
        throw new IllegalArgumentException("No such message");

    }
    return null;
  }

  @Override
  protected Message create(byte[] data) {
    byte type = data[0];
    byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
    create(type, rawData);
    return null;
  }
//
//  @Override
//  public Message create(byte code, byte[] data) {
//
//    P2pMessageCodes receivedCommand = P2pMessageCodes.fromByte(code);
//    switch (receivedCommand) {
//      case HELLO:
//        return new HelloMessage(encoded);
//      case DISCONNECT:
//        return new DisconnectMessage(encoded);
//      case PING:
//        return StaticMessages.PING_MESSAGE;
//      case PONG:
//        return StaticMessages.PONG_MESSAGE;
//      default:
//        throw new IllegalArgumentException("No such message");
//    }
//  }
}
