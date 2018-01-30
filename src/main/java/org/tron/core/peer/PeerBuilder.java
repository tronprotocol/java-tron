/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core.peer;

import javax.inject.Inject;
import org.tron.common.crypto.ECKey;
import org.tron.core.Blockchain;
import org.tron.core.UTXOSet;
import org.tron.core.Wallet;
import org.tron.core.consensus.client.Client;

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

    Peer peer = new Peer(type, blockchain, utxoSet, wallet, client, key);
    //peer.setClient(client);
    //blockchain.addListener(new BlockchainClientListener(client, peer));
    return peer;
  }
}
