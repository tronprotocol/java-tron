package org.tron.peer;

import javax.inject.Inject;
import org.tron.consensus.client.BlockchainClientListener;
import org.tron.consensus.client.Client;
import org.tron.core.Blockchain;
import org.tron.core.UTXOSet;
import org.tron.crypto.ECKey;
import org.tron.wallet.Wallet;

/**
 * Builds a peer
 * <p>
 * Set the key and type before calling build
 */
public class PeerBuilder {

  private Blockchain blockchain;
  private UTXOSet utxoSet;
  private Wallet wallet;
  private ECKey key;
  private String type;
  private Client client;

  @Inject
  public PeerBuilder(Blockchain blockchain, UTXOSet utxoSet, Client client) {
    this.blockchain = blockchain;
    this.utxoSet = utxoSet;
    this.client = client;
  }

  private void buildWallet() {
    if (key == null) {
      throw new IllegalStateException("Key must be set before building the wallet");
    }

    wallet = new Wallet(key);
  }

  public PeerBuilder setType(String type) {
    this.type = type;
    return this;
  }

  public PeerBuilder setKey(ECKey key) {
    this.key = key;
    return this;
  }

  public Peer build() {
    buildWallet();
    utxoSet.reindex();

    Peer peer = new Peer(type, blockchain, utxoSet, wallet, key);
    peer.setClient(client);
    blockchain.addListener(new BlockchainClientListener(client, peer));
    return peer;
  }
}
