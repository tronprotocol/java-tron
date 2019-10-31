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

public class Keccak256 extends KeccakCore {

  /**
   * Create the engine.
   */
  public Keccak256() {
    super("tron-keccak-256");
  }

  public Digest copy() {
    return copyState(new Keccak256());
  }

  public int engineGetDigestLength() {
    return 32;
  }

  @Override
  protected byte[] engineDigest() {
    return null;
  }

  @Override
  protected void engineUpdate(byte arg0) {
  }

  @Override
  protected void engineUpdate(byte[] arg0, int arg1, int arg2) {
  }
}
