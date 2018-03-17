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

import org.tron.core.net.message.MessageRegistry;
import org.tron.core.net.peer.PeerConnectionDelegate;

import java.io.UnsupportedEncodingException;

public class StartWorker implements Runnable {

  private io.scalecube.transport.Message msg;

  private PeerConnectionDelegate peerDel;

  /**
   * Handle the received message.
   */
  public StartWorker(io.scalecube.transport.Message msg, final PeerConnectionDelegate peerDel) {
    this.msg = msg;
    this.peerDel = peerDel;
  }

  @Override
  public void run() {
    try {
      String key = msg.header("type");
      byte[] newValueBytes = msg.data().toString().getBytes("ISO-8859-1");

      org.tron.core.net.message.Message message = MessageRegistry.getMessageByKey(key, newValueBytes);

      peerDel.onMessage(peerDel.getPeer(msg), message);

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }
}
