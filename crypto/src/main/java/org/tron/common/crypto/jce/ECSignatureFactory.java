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

package org.tron.common.crypto.jce;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;

public final class ECSignatureFactory {

  public static final String RAW_ALGORITHM = "NONEwithECDSA";

  private static final String rawAlgorithmAssertionMsg =
      "Assumed the JRE supports NONEwithECDSA signatures";

  private ECSignatureFactory() {
  }

  public static Signature getRawInstance() {
    try {
      return Signature.getInstance(RAW_ALGORITHM);
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(rawAlgorithmAssertionMsg, ex);
    }
  }

  public static Signature getRawInstance(final String provider) throws
      NoSuchProviderException {
    try {
      return Signature.getInstance(RAW_ALGORITHM, provider);
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(rawAlgorithmAssertionMsg, ex);
    }
  }

  public static Signature getRawInstance(final Provider provider) {
    try {
      return Signature.getInstance(RAW_ALGORITHM, provider);
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(rawAlgorithmAssertionMsg, ex);
    }
  }
}
