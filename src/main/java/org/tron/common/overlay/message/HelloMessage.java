package org.tron.common.overlay.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

  Protocol.HelloMessage helloMessage;

  public HelloMessage(byte[] rawData) {
    super(rawData);
    this.type = MessageTypes.P2P_HELLO.asByte();
  }

  public HelloMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  /**
   * Create hello message.
   */
  public HelloMessage(Node from, int version, String clientId, int listenPort, String peerId) {

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Builder builder = Protocol.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(version);
    builder.setClientId(clientId);
    builder.setListenPort(listenPort);
    builder.setPeerId(peerId);

    this.helloMessage = builder.build();
    this.unpacked = true;
    this.type = MessageTypes.P2P_HELLO.asByte();
    pack();
  }

  private void unPack() {
    try {
      this.helloMessage = Protocol.HelloMessage.parseFrom(this.data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    this.unpacked = true;
  }

  private void pack() {
    this.data = this.helloMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  /**
   * Get the version of p2p protocol.
   */
  public byte getVersion() {
    if (!this.unpacked) {
      this.unPack();
    }
    return (byte) this.helloMessage.getVersion();
  }

  /**
   * Get client ID.
   */
  public String getClientId() {
    if (!this.unpacked) {
      this.unPack();
    }
    return this.helloMessage.getClientId();
  }

  /**
   * Get listen port.
   */
  public int getListenPort() {
    if (!this.unpacked) {
      this.unPack();
    }
    return this.helloMessage.getListenPort();
  }

  /**
   * Get peer ID.
   */
  public String getPeerId() {
    if (!this.unpacked) {
      this.unPack();
    }
    return this.helloMessage.getPeerId();
  }

  /**
   * Set peer ID.
   */
  public void setPeerId(String peerId) {
    Builder builder = this.helloMessage.toBuilder();
    builder.setPeerId(peerId);
    this.helloMessage = builder.build();
  }

  /**
   * Set version of p2p protocol.
   */
  public void setVersion(byte version) {
    Builder builder = this.helloMessage.toBuilder();
    builder.setVersion(version);
    this.helloMessage = builder.build();
  }

  /**
   * Get string.
   */
  public String toString() {
    return helloMessage.toString();
//    if (!this.unpacked) {
//      this.unPack();
//    }
//    return "[" + this.getCommand().name() + " p2pVersion="
//        + this.getVersion() + " clientId=" + this.getClientId()
//        + " peerPort=" + this.getListenPort() + " peerId="
//        + this.getPeerId() + "]";
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}