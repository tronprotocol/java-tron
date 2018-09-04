/**
 * UPnP PortMapper - A tool for managing port forwardings via UPnP Copyright (C) 2015 Christoph
 * Pirkl <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 */
package org.tron.common.overlay.portmap.util;

import java.util.HashMap;
import java.util.Map;

public class EncodingUtilities {

  private static Map<Character, String> knownEncodings;

  static {
    knownEncodings = new HashMap<>();
    knownEncodings.put('<', "&lt;");
    knownEncodings.put('>', "&gt;");
    knownEncodings.put('&', "&amp;");
  }

  /**
   * Replace all special characters with their html entities.
   *
   * @param s the string in which to replace the special characters.
   * @return the result of the replacement.
   */
  public static String htmlEntityEncode(final String s) {
    final StringBuffer buf = new StringBuffer();

    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
        buf.append(c);
      } else {
        if (knownEncodings.containsKey(c)) {
          buf.append(knownEncodings.get(c));
        } else {
          buf.append("&#" + (int) c + ";");
        }
      }
    }
    return buf.toString();
  }
}
