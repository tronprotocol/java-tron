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
package org.tron.core.consensus;

import org.tron.protos.Protocal.Block;

public class ConsensusServer {

  public void ConsensusServer() {

  }

  // This method is called when p2p receives the block
  public void receiveBlockFromP2P(Block block) {
    //Verify the received block, if the verification is passed, add the block to the blockchain
    System.out.println("Add the block to the blockchain");
  }

  // Generate block and send block to p2p
  public void sendBlockToP2P() {
    //Judge whether this is leader at this moment
    if (!isLeader()) {
      System.out.println("This is't leader at this moment");
    } else {
      System.out.println("Leader");
    }
  }

  //To determine whether it is leader
  private boolean isLeader() {
    return true;
  }
}
