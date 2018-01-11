package org.tron.peer;

import com.google.inject.Injector;
import javax.inject.Inject;

import org.tron.consensus.client.Client;
import org.tron.core.Blockchain;
import org.tron.core.UTXOSet;
import org.tron.crypto.ECKey;
import org.tron.utils.ByteArray;
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
  private Injector injector;

  @Inject
  public PeerBuilder(Injector injector) {
    this.injector = injector;
  }

  private void buildBlockchain() {
    if (wallet == null) {
      throw new IllegalStateException("Wallet must be set before building the blockchain");
    }
    if (type == null) {
      throw new IllegalStateException("Type must be set before building the blockchain");
    }

    blockchain = new Blockchain(ByteArray.toHexString(wallet.getAddress()), this.type);
    blockchain.setClient(injector.getInstance(Client.class));
  }

  private void buildUTXOSet() {
    if (blockchain == null) {
      throw new IllegalStateException("Blockchain must be set before building the UTXOSet");
    }

    utxoSet = injector.getInstance(UTXOSet.class);
    utxoSet.setBlockchain(blockchain);
    utxoSet.reindex();
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
    buildBlockchain();
    buildUTXOSet();
    Peer peer = new Peer(type, blockchain, utxoSet, wallet, key);
    peer.setClient(injector.getInstance(Client.class));
    return peer;
  }
}
