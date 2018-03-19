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

package org.tron.core.config.args;

import java.util.List;

public class LocalWitnesses {

  private List<String> privateKeys;

  public List<String> getPrivateKeys() {
    return this.privateKeys;
  }

  /**
   * Private key of ECKey.
   */
  public void setPrivateKeys(final List<String> privateKeys) {
    if (null == privateKeys) {
      return;
    }
    this.privateKeys = privateKeys;
    for (String privateKey : privateKeys) {
      if (privateKey != null && privateKey.toUpperCase().startsWith("0X")) {
        privateKey = privateKey.substring(2);
      }

      if (privateKey != null && privateKey.length() != 0
          && privateKey.length() != 66) {
        throw new IllegalArgumentException(
            "Private key(" + privateKey + ") must be 66-bits hex string.");
      }
    }

  }
}
