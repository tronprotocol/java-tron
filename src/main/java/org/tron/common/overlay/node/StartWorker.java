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

package org.tron.common.overlay.node;

import io.scalecube.cluster.Cluster;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;

public class StartWorker implements Runnable {

  private io.scalecube.transport.Message msg;

  private PeerConnectionDelegate peerDel;

  private HashMap<Integer, PeerConnection> listPeer;

  private Cluster cluster;

  /**
   * Handle the received message.
   */
  public StartWorker(io.scalecube.transport.Message msg, final PeerConnectionDelegate peerDel,
      final HashMap<Integer, PeerConnection> listPeer, final Cluster cluster) {
    this.msg = msg;
    this.peerDel = peerDel;
    this.listPeer = listPeer;
    this.cluster = cluster;
  }

  @Override
  public void run() {
    byte[] newValueBytes = null;
    String key = "";
    try {
      key = msg.header("type");
      newValueBytes = msg.data().toString().getBytes("ISO-8859-1");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    org.tron.core.net.message.Message message = getMessageByKey(key, newValueBytes);
    peerDel.onMessage(listPeer.get(cluster.member(msg.sender()).get().hashCode()), message);
  }

  private Message getMessageByKey(String key, byte[] content) {
    Message message = null;

    switch (MessageTypes.valueOf(key)) {
      case BLOCK:
        message = new BlockMessage(content);
        break;
      case TRX:
        message = new TransactionMessage(content);
        break;
      case SYNC_BLOCK_CHAIN:
        message = new SyncBlockChainMessage(content);
        break;
      case FETCH_INV_DATA:
        message = new FetchInvDataMessage(content);
        break;
      case BLOCK_INVENTORY:
        message = new BlockInventoryMessage(content);
        break;
      default:
        try {
          throw new IllegalArgumentException("No such message");
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
    }

    return message;
  }
}
