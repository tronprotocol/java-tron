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

package org.tron.crypto.jce;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

public final class ECKeyFactory {

  public static final String ALGORITHM = "EC";

  private static final String algorithmAssertionMsg =
      "Assumed the JRE supports EC key factories";

  private ECKeyFactory() {
  }

  public static KeyFactory getInstance() {
    return Holder.INSTANCE;
  }

  public static KeyFactory getInstance(final String provider) throws
      NoSuchProviderException {
    try {
      return KeyFactory.getInstance(ALGORITHM, provider);
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(algorithmAssertionMsg, ex);
    }
  }

  public static KeyFactory getInstance(final Provider provider) {
    try {
      return KeyFactory.getInstance(ALGORITHM, provider);
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(algorithmAssertionMsg, ex);
    }
  }

  private static class Holder {
    private static final KeyFactory INSTANCE;

    static {
      try {
        INSTANCE = KeyFactory.getInstance(ALGORITHM);
      } catch (NoSuchAlgorithmException ex) {
        throw new AssertionError(algorithmAssertionMsg, ex);
      }
    }
  }
}
