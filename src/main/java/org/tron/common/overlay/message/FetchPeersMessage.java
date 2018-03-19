package org.tron.common.overlay.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.core.net.tmsg.MessageTypes;
import org.tron.protos.Message;
import org.tron.protos.Message.P2pMessageCode;

public class FetchPeersMessage extends P2pMessage {

  private Message.PeersMessage peersMessage;

  public FetchPeersMessage(byte[] payload) {
    super(payload);
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
    return null;
  }
}