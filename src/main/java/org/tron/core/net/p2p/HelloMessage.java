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

import com.google.common.base.Joiner;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.protos.Message;
import org.tron.protos.Message.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

  Message.HelloMessage helloMessage;

  public HelloMessage(byte[] encoded) {
    super(encoded);
  }

  public HelloMessage(byte p2pVersion, String clientId,
      List<Message.Capability> capabilities, int listenPort, String peerId) {

    Builder builder = this.helloMessage.toBuilder();

    builder.setP2PVersion(p2pVersion);
    builder.setClientId(clientId);
    builder.addAllCapabilities(capabilities);
    builder.setListenPort(listenPort);
    builder.setPeerId(peerId);

    this.helloMessage = builder.build();
    this.parsed = true;
  }

  private void parse() {
    try {
      this.helloMessage = Message.HelloMessage.parseFrom(encoded);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    this.parsed = true;
  }

  private void encode() {
    this.encoded = this.helloMessage.toByteArray();
  }

  @Override
  public byte[] getEncoded() {
    if (encoded == null) {
      encode();
    }
    return encoded;
  }

  public byte getP2PVersion() {
    if (!parsed) {
      parse();
    }
    return (byte) this.helloMessage.getP2PVersion();
  }

  public String getClientId() {
    if (!parsed) {
      parse();
    }
    return this.helloMessage.getClientId();
  }

  public List<Message.Capability> getCapabilities() {
    if (!parsed) {
      parse();
    }
    return this.helloMessage.getCapabilitiesList();
  }

  public int getListenPort() {
    if (!parsed) {
      parse();
    }
    return this.helloMessage.getListenPort();
  }

  public String getPeerId() {
    if (!parsed) {
      parse();
    }
    return this.helloMessage.getPeerId();
  }

  @Override
  public P2pMessageCodes getCommand() {
    return P2pMessageCodes.HELLO;
  }

  public void setPeerId(String peerId) {
    Builder builder = this.helloMessage.toBuilder();
    builder.setPeerId(peerId);
    this.helloMessage = builder.build();
  }

  public void setP2pVersion(byte p2pVersion) {
    Builder builder = this.helloMessage.toBuilder();
    builder.setP2PVersion(p2pVersion);
    this.helloMessage = builder.build();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public String toString() {
    if (!parsed) {
      parse();
    }
    return "[" + this.getCommand().name() + " p2pVersion="
        + this.getP2PVersion() + " clientId=" + this.getClientId()
        + " capabilities=[" + Joiner.on(" ").join(this.getCapabilities())
        + "]" + " peerPort=" + this.getListenPort() + " peerId="
        + this.getPeerId() + "]";
  }
}