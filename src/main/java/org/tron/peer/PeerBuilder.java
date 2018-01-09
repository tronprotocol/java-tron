package org.tron.peer;

import org.tron.consensus.server.Server;
import org.tron.core.Blockchain;
import org.tron.core.UTXOSet;
import org.tron.crypto.ECKey;
import org.tron.utils.ByteArray;
import org.tron.wallet.Wallet;

/**
 * Builds a peer
 *
 * Set the key and type before calling build
 */
public class PeerBuilder {

    private Blockchain blockchain;
    private UTXOSet utxoSet;
    private Wallet wallet;
    private ECKey key;
    private String type;

    public PeerBuilder() {
        Server.serverRun();
    }

    private void buildBlockchain() {
        if (wallet == null) throw new IllegalStateException("Wallet must be set before building the blockchain");
        if (type == null) throw new IllegalStateException("Type must be set before building the blockchain");

        blockchain = new Blockchain(ByteArray.toHexString(wallet.getAddress()), this.type);
        System.out.println();
    }

    private void buildUTXOSet() {
        if (blockchain == null) throw new IllegalStateException("Blockchain must be set before building the UTXOSet");

        utxoSet = new UTXOSet();
        utxoSet.setBlockchain(blockchain);
        utxoSet.reindex();
    }

    private void buildWallet() {
        if (key == null) throw new IllegalStateException("Key must be set before building the wallet");

        wallet = new Wallet();
        wallet.init(key);
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
        return new Peer(type, blockchain, utxoSet, wallet, key);
    }
}
