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

package org.tron.core.net.p2p;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Message;

public class DisconnectMessage extends P2pMessage {

  private Message.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte[] encoded) {
    super(encoded);
  }

  public DisconnectMessage(Message.ReasonCode reason) {
    this.disconnectMessage = Message.DisconnectMessage
        .newBuilder()
        .setReason(reason)
        .build();
    parsed = true;
  }

  private void parse() {
    try {
      this.disconnectMessage = Message.DisconnectMessage.parseFrom(encoded);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    parsed = true;
  }

  private void encode() {
    this.encoded = this.disconnectMessage.toByteArray();
  }

  @Override
  public byte[] getEncoded() {
    if (encoded == null) {
      encode();
    }
    return encoded;
  }

  @Override
  public P2pMessageCodes getCommand() {
    return P2pMessageCodes.DISCONNECT;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public Message.ReasonCode getReason() {
    if (!parsed) {
      parse();
    }
    return this.disconnectMessage.getReason();
  }

  public String toString() {
    if (!parsed) {
      parse();
    }
    return "[" + this.getCommand().name() + " reason=" + this.getReason() + "]";
  }
}