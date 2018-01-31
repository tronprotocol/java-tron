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

package org.tron.core.consensus.client;

import org.tron.common.overlay.Net;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.Type;
import org.tron.common.utils.ByteArray;
import org.tron.core.events.BlockchainListener;
import org.tron.core.peer.Peer;
import org.tron.core.peer.PeerType;
import org.tron.protos.Protocal.Block;

public class BlockchainClientListener implements BlockchainListener {

  private Client client;
  private Peer peer;

  public BlockchainClientListener(Client client, Peer peer) {
    this.client = client;
    this.peer = peer;
  }

  @Override
  public void addBlock(Block block) {
    String value = ByteArray.toHexString(block.toByteArray());

    if (peer.getType().equals(PeerType.PEER_SERVER)) {
      Message message = new Message(value, Type.BLOCK);
      //net.broadcast(message);
      client.putMessage1(message); // consensus: put message
    }
  }

  @Override
  public void addBlockNet(Block block, Net net) {
    if (peer.getType().equals(PeerType.PEER_SERVER)) {
      String value = ByteArray.toHexString(block.toByteArray());
      Message message = new Message(value, Type.BLOCK);
      net.broadcast(message);
    }
  }

  @Override
  public void addGenesisBlock(Block block) {
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
