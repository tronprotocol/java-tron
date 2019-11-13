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

package org.tron.common.crypto.cryptohash;

public class Keccak512 extends KeccakCore {

  /**
   * Create the engine.
   */
  public Keccak512() {
    super("tron-keccak-512");
  }

  public Digest copy() {
    return copyState(new Keccak512());
  }

  public int engineGetDigestLength() {
    return 64;
  }

  @Override
  protected byte[] engineDigest() {
    return null;
  }

  @Override
  protected void engineUpdate(byte input) {
  }

  @Override
  protected void engineUpdate(byte[] input, int offset, int len) {
  }
}
