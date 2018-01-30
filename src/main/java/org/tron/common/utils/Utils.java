/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import java.security.SecureRandom;
import java.nio.*;
import java.nio.charset.Charset;

public class Utils {
  private static SecureRandom random = new SecureRandom();

  public static SecureRandom getRandom() {
    return random;
  }

  public static byte[] getBytes (char[] chars) {
    Charset cs = Charset.forName ("UTF-8");
    CharBuffer cb = CharBuffer.allocate (chars.length);
    cb.put (chars);
    cb.flip ();
    ByteBuffer bb = cs.encode (cb);

    return bb.array();
  }

  private char[] getChars (byte[] bytes) {
    Charset cs = Charset.forName ("UTF-8");
    ByteBuffer bb = ByteBuffer.allocate (bytes.length);
    bb.put (bytes);
    bb.flip ();
    CharBuffer cb = cs.decode (bb);

    return cb.array();
  }
}
