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

public class LocalWitness {

  private String privateKey;

  public String getPrivateKey() {
    return this.privateKey;
  }

  /**
   * Private key of ECKey.
   */
  public void setPrivateKey(final String privateKey) {
    this.privateKey = privateKey;

    if (this.privateKey != null && this.privateKey.toUpperCase().startsWith("0X")) {
      this.privateKey = this.privateKey.substring(2);
    }

    if (this.privateKey != null && this.privateKey.length() != 0
        && this.privateKey.length() != 66) {
      throw new IllegalArgumentException();
    }
  }
}
