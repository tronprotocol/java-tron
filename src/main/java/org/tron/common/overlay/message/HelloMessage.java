package org.tron.common.overlay.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

  Protocol.HelloMessage helloMessage;

  public HelloMessage(byte[] rawData) {
    super(rawData);
    this.type = MessageTypes.P2P_HELLO.asByte();
    unPack();
  }

  public HelloMessage(byte type, byte[] rawData) {
    super(type, rawData);
    try {
      this.helloMessage = Protocol.HelloMessage.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
    unPack();
  }

  /**
   * Create hello message.
   */
  public HelloMessage(Node from) {

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Builder builder = Protocol.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(Args.getInstance().getNodeP2pVersion());

    this.helloMessage = builder.build();
    this.type = MessageTypes.P2P_HELLO.asByte();
    this.data = this.helloMessage.toByteArray();
  }

  public void unPack() {
    try {
      this.helloMessage = Protocol.HelloMessage.parseFrom(this.data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  @Override
  public byte[] getData() {
    return this.data;
  }

  /**
   * Get the version of p2p protocol.
   */
  public int getVersion() {
    return this.helloMessage.getVersion();
  }

  /**
   * Get listen port.
   */
  public int getListenPort() {
    return this.helloMessage.getFrom().getPort();
  }

  /**
   * Get peer ID.
   */
  public String getPeerId() {
    return ByteArray.toHexString(this.helloMessage.getFrom().getNodeId().toByteArray());
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

  public Node getFrom() {
    Endpoint from = this.helloMessage.getFrom();
    return new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
  }
}