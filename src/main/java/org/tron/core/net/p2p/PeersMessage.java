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
import java.util.List;
import org.tron.protos.Message;
import org.tron.protos.Message.P2pMessageCode;

public class PeersMessage extends P2pMessage {

  private boolean parsed = false;

  private Message.PeersMessage peersMessage;

  public PeersMessage(byte[] payload) {
    super(payload);
  }

  public PeersMessage(List<Message.Peer> peers) {
    this.peersMessage = Message.PeersMessage
        .newBuilder()
        .addAllPeers(peers)
        .build();
    parsed = true;
  }

  private void parse() {
    try {
      this.peersMessage = Message.PeersMessage.parseFrom(encoded);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    this.parsed = true;
  }

  private void encode() {
    this.encoded = this.peersMessage.toByteArray();
  }

  @Override
  public byte[] getEncoded() {
    if (encoded == null) {
      encode();
    }
    return encoded;
  }

  public List<Message.Peer> getPeers() {
    if (!parsed) {
      this.parse();
    }
    return this.peersMessage.getPeersList();
  }

  @Override
  public P2pMessageCode getCommand() {
    return P2pMessageCode.PEERS;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public String toString() {
    if (!parsed) {
      this.parse();
    }

    StringBuilder sb = new StringBuilder();
    for (Message.Peer peerData : this.getPeers()) {
      sb.append("\n       ").append(peerData);
    }
    return "[" + this.getCommand().name() + sb.toString() + "]";
  }
}