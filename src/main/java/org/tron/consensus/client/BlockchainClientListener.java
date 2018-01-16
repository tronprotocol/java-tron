package org.tron.consensus.client;

import org.tron.core.events.BlockchainListener;
import org.tron.overlay.Net;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.peer.Peer;
import org.tron.peer.PeerType;
import org.tron.protos.core.TronBlock;
import org.tron.utils.ByteArray;

public class BlockchainClientListener implements BlockchainListener {

  private Client client;
  private Peer peer;

  public BlockchainClientListener(Client client, Peer peer) {
    this.client = client;
    this.peer = peer;
  }

  @Override
  public void addBlock(TronBlock.Block block) {
    String value = ByteArray.toHexString(block.toByteArray());

    if (peer.getType().equals(PeerType.PEER_SERVER)) {
      Message message = new Message(value, Type.BLOCK);
      //net.broadcast(message);
      client.putMessage1(message); // consensus: put message
    }
  }

  @Override
  public void addBlockNet(TronBlock.Block block, Net net) {
    if (peer.getType().equals(PeerType.PEER_SERVER)) {
      String value = ByteArray.toHexString(block.toByteArray());
      Message message = new Message(value, Type.BLOCK);
      net.broadcast(message);
    }
  }

  @Override
  public void addGenesisBlock(TronBlock.Block block) {
    if (peer.getType().equals(PeerType.PEER_SERVER)) {
      String value = ByteArray.toHexString(block.toByteArray());
      Message message = new Message(value, Type.BLOCK);
      client.putMessage1(message); // consensus: put message GenesisBlock
      //Merely for the placeholders, no real meaning
      //client.getMessage1("block");
      Message time = new Message(value, Type.TRANSACTION);
      client.putMessage1(time);
    }
  }
}
