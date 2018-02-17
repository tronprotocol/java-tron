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

import java.security.Provider;
import java.security.Security;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.tron.common.crypto.cryptohash.Keccak256;
import org.tron.common.crypto.cryptohash.Keccak512;

public final class TronCastleProvider {

  public static Provider getInstance() {
    return Holder.INSTANCE;
  }

  private static class Holder {

    private static final Provider INSTANCE;

    static {
      Provider p = Security.getProvider("SC");

      INSTANCE = (p != null) ? p : new BouncyCastleProvider();
      INSTANCE.put("MessageDigest.TRON-KECCAK-256", Keccak256.class.getName());
      INSTANCE.put("MessageDigest.TRON-KECCAK-512", Keccak512.class.getName());
    }
  }
}
