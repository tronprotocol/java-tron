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

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public final class ECAlgorithmParameters {

  public static final String ALGORITHM = "EC";
  public static final String CURVE_NAME = "secp256k1";

  private ECAlgorithmParameters() {
  }

  public static ECParameterSpec getParameterSpec() {
    try {
      return Holder.INSTANCE.getParameterSpec(ECParameterSpec.class);
    } catch (InvalidParameterSpecException ex) {
      throw new AssertionError(
          "Assumed correct key spec statically", ex);
    }
  }

  public static byte[] getASN1Encoding() {
    try {
      return Holder.INSTANCE.getEncoded();
    } catch (IOException ex) {
      throw new AssertionError(
          "Assumed algo params has been initialized", ex);
    }
  }

  private static class Holder {

    private static final AlgorithmParameters INSTANCE;

    private static final ECGenParameterSpec SECP256K1_CURVE
        = new ECGenParameterSpec(CURVE_NAME);

    static {
      try {
        INSTANCE = AlgorithmParameters.getInstance(ALGORITHM);
        INSTANCE.init(SECP256K1_CURVE);
      } catch (NoSuchAlgorithmException ex) {
        throw new AssertionError(
            "Assumed the JRE supports EC algorithm params", ex);
      } catch (InvalidParameterSpecException ex) {
        throw new AssertionError(
            "Assumed correct key spec statically", ex);
      }
    }
  }
}
