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

package org.tron.common.overlay.discover.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.MessageFactory;
import org.tron.common.overlay.message.PingMessage;
import org.tron.common.overlay.message.PongMessage;
import org.tron.core.net.message.MessageTypes;

public class DiscoverMessageFactory extends MessageFactory {

  @Override
  protected Message create(byte type, byte[] rawData) {
    MessageTypes messageType = MessageTypes.fromByte(type);
    switch (messageType) {
      case DISCOVER_PING:
        return new HelloMessage(type, rawData);
      case DISCOVER_PONG:
        return new DisconnectMessage(type, rawData);
      case DISCOVER_PEERS:
        return new PingMessage(type, rawData);
      case DISCOVER_FIND_PEER:
        return new PongMessage(type, rawData);
      default:
        throw new IllegalArgumentException("No such message");
    }
  }

  @Override
  protected Message create(byte[] data) {
    byte type = data[0];
    byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
    return create(type, rawData);
  }
}
