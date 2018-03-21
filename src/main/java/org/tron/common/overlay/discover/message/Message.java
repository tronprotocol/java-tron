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
package org.tron.common.overlay.discover.message;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.Sha256Hash;

public abstract class Message {

  public static int verison = 1;

  public static byte PING = 1;
  public static byte PONG = 2;
  public static byte FINE_PEERS = 3;
  public static byte GET_PEERS = 4;

  protected byte type;
  protected byte[] data;

  public Message(byte type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  public byte getType(){
    return this.type;
  }

  public byte[] getData() {
    return this.data;
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public abstract byte[] getNodeId();

  @Override
  public String toString() {
    return "[Message Type: " + getType() + ", data: " + (data == null? "":String.valueOf(data)) + "]";
  }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }

  public static Message parse(byte[] encode) {
    byte type = encode[0];
    byte[] data = ArrayUtils.subarray(encode, 1, encode.length);
    switch (type) {
      case 1:
        return new PingMessage(data);
      case 2:
        return new PongMessage(data);
      case 3:
        return new FindNodeMessage(data);
      case 4:
        return new NeighborsMessage(data);
      default:
        throw new IllegalArgumentException("No such message");
    }
  }

}
