package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message;
import org.tron.protos.Message.P2pMessageCode;

import java.util.List;

public class FetchPeersMessage extends P2pMessage {

  private Message.PeersMessage peersMessage;

  public FetchPeersMessage(byte[] payload) {
    super(payload);
    this.type = MessageTypes.P2P_FETCH_PEERS.asByte();
  }

  /**
   * Get peers.
   */
  public FetchPeersMessage(List<Message.Peer> peers) {
    this.peersMessage = Message.PeersMessage
        .newBuilder()
        .addAllPeers(peers)
        .build();
    this.unpacked = true;
    this.type = MessageTypes.P2P_FETCH_PEERS.asByte();
  }

  private void unPack() {
    try {
      this.peersMessage = Message.PeersMessage.parseFrom(this.data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    this.unpacked = true;
  }

  private void pack() {
    this.data = this.peersMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  /**
   * Get peers.
   */
  public List<Message.Peer> getPeers() {
    if (!this.unpacked) {
      this.unPack();
    }
    return this.peersMessage.getPeersList();
  }

  @Override
  public P2pMessageCode getCommand() {
    return P2pMessageCode.PEERS;
  }

  /**
   * Get information.
   */
  public String toString() {
    if (!this.unpacked) {
      this.unPack();
    }

    StringBuilder sb = new StringBuilder();
    for (Message.Peer peerData : this.getPeers()) {
      sb.append("\n       ").append(peerData);
    }
    return "[" + this.getCommand().name() + sb.toString() + "]";
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}