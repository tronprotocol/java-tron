package org.tron.common.overlay.discover.message;

import com.google.common.base.Joiner;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message;
import org.tron.protos.Message.HelloMessage.Builder;
import org.tron.protos.Message.P2pMessageCode;

import java.util.List;

public class HelloMessage extends P2pMessage {

  Message.HelloMessage helloMessage;

  public HelloMessage(byte[] encoded) {
    super(encoded);
    this.type = MessageTypes.P2P_HELLO.asByte();
  }

  /**
   * Create hello message.
   */
  public HelloMessage(byte p2pVersion, String clientId,
      List<Message.Capability> capabilities, int listenPort, String peerId) {

    Builder builder = this.helloMessage.toBuilder();

    builder.setP2PVersion(p2pVersion);
    builder.setClientId(clientId);
    builder.addAllCapabilities(capabilities);
    builder.setListenPort(listenPort);
    builder.setPeerId(peerId);

    this.helloMessage = builder.build();
    this.unpacked = true;
    this.type = MessageTypes.P2P_HELLO.asByte();
  }

  private void unPack() {
    try {
      this.helloMessage = Message.HelloMessage.parseFrom(data);
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
  public byte getP2PVersion() {
    if (!this.unpacked) {
      this.unPack();
    }
    return (byte) this.helloMessage.getP2PVersion();
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
   * Get capabilities.
   */
  public List<Message.Capability> getCapabilities() {
    if (!this.unpacked) {
      this.unPack();
    }
    return this.helloMessage.getCapabilitiesList();
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

  @Override
  public P2pMessageCode getCommand() {
    return P2pMessageCode.HELLO;
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
  public void setP2pVersion(byte p2pVersion) {
    Builder builder = this.helloMessage.toBuilder();
    builder.setP2PVersion(p2pVersion);
    this.helloMessage = builder.build();
  }

  /**
   * Get string.
   */
  public String toString() {
    if (!this.unpacked) {
      this.unPack();
    }
    return "[" + this.getCommand().name() + " p2pVersion="
        + this.getP2PVersion() + " clientId=" + this.getClientId()
        + " capabilities=[" + Joiner.on(" ").join(this.getCapabilities())
        + "]" + " peerPort=" + this.getListenPort() + " peerId="
        + this.getPeerId() + "]";
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}