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

package org.tron.utils;

import java.nio.ByteBuffer;

public class TypeConversion {
  private static ByteBuffer buffer = ByteBuffer.allocate(8);

  public static byte[] longToBytes(long x) {
    buffer.putLong(0, x);
    return buffer.array();
  }

  public static long bytesToLong(byte[] bytes) {
    buffer.put(bytes, 0, bytes.length);
    buffer.flip();
    return buffer.getLong();
  }

  public static String bytesToHexString(byte[] src) {
    StringBuilder stringBuilder = new StringBuilder("");

    if (src == null || src.length <= 0) {
      return null;
    }

    for (int i = 0; i < src.length; i++) {
      int v = src[i] & 0xFF;
      String hv = Integer.toHexString(v);

      if (hv.length() < 2) {
        stringBuilder.append(0);
      }

      stringBuilder.append(hv);
    }

    return stringBuilder.toString();
  }

  public static byte[] hexStringToBytes(String hexString) {
    if (hexString == null || hexString.equals("")) {
      return null;
    }

    hexString = hexString.toUpperCase();
    int length = hexString.length() / 2;
    char[] hexChars = hexString.toCharArray();
    byte[] d = new byte[length];

    for (int i = 0; i < length; i++) {
      int pos = i * 2;
      d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte
          (hexChars[pos + 1]));
    }

    return d;
  }

  private static byte charToByte(char c) {
    return (byte) "0123456789ABCDEF".indexOf(c);
  }

  public static boolean increment(byte[] bytes) {
    final int startIndex = 0;
    int i;
    for (i = bytes.length - 1; i >= startIndex; i--) {
      bytes[i]++;
      if (bytes[i] != 0) {
        break;
      }
    }

    return (i >= startIndex || bytes[startIndex] != 0);
  }
}
