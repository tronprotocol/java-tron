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
package org.tron.common.crypto.zksnark;

/**
 * Implementation of specific cyclic subgroup of points belonging to {@link BN128Fp} <br/> Members
 * of this subgroup are passed as a first param to pairing input {@link
 * PairingCheck#addPair(BN128G1, BN128G2)} <br/>
 *
 * Subgroup generator G = (1; 2)
 *
 * @author Mikhail Kalinin
 * @since 01.09.2017
 */
public class BN128G1 extends BN128Fp {

  BN128G1(BN128<Fp> p) {
    super(p.x, p.y, p.z);
  }

  /**
   * Checks whether point is a member of subgroup, returns a point if check has been passed and null
   * otherwise
   */
  public static BN128G1 create(byte[] x, byte[] y) {

    BN128<Fp> p = BN128Fp.create(x, y);

    if (p == null) {
      return null;
    }

    if (!isGroupMember(p)) {
      return null;
    }

    return new BN128G1(p);
  }

  /**
   * Formally we have to do this check but in our domain it's not necessary, thus always return
   * true
   */
  private static boolean isGroupMember(BN128<Fp> p) {
    return true;
  }

  @Override
  public BN128G1 toAffine() {
    return new BN128G1(super.toAffine());
  }
}
