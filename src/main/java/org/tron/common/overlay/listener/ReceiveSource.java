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

package org.tron.common.overlay.listener;

import java.util.Enumeration;
import java.util.Vector;
import org.tron.common.overlay.message.Message;

public class ReceiveSource {
  private Vector repository = new Vector();

  public ReceiveSource() {

  }

  public void addReceiveListener(ReceiveListener listener) {
    repository.addElement(listener);
  }

  public void notifyReceiveEvent(Message message) {
    Enumeration enumeration = repository.elements();

    while (enumeration.hasMoreElements()) {
      ReceiveListener listener = (ReceiveListener) enumeration.nextElement();
      listener.handleReceive(message);
    }
  }
}
