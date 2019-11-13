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

import static org.tron.common.crypto.zksnark.Params.B_Fp2;

import java.math.BigInteger;

/**
 * Definition of {@link BN128} over F_p2, where "p" equals {@link Params#P} <br/>
 *
 * Curve equation: <br/> Y^2 = X^3 + b, where "b" equals {@link Params#B_Fp2} <br/>
 *
 * @author Mikhail Kalinin
 * @since 31.08.2017
 */
public class BN128Fp2 extends BN128<Fp2> {

  // the point at infinity
  static final BN128<Fp2> ZERO = new BN128Fp2(Fp2.ZERO, Fp2.ZERO, Fp2.ZERO);

  protected BN128Fp2(Fp2 x, Fp2 y, Fp2 z) {
    super(x, y, z);
  }

  protected BN128Fp2(BigInteger a, BigInteger b, BigInteger c, BigInteger d) {
    super(Fp2.create(a, b), Fp2.create(c, d), Fp2._1);
  }

  /**
   * Checks whether provided data are coordinates of a point on the curve, then checks if this point
   * is a member of subgroup of order "r" and if checks have been passed it returns a point,
   * otherwise returns null
   */
  public static BN128<Fp2> create(byte[] aa, byte[] bb, byte[] cc, byte[] dd) {

    Fp2 x = Fp2.create(aa, bb);
    Fp2 y = Fp2.create(cc, dd);

    // check for point at infinity
    if (x.isZero() && y.isZero()) {
      return ZERO;
    }

    BN128<Fp2> p = new BN128Fp2(x, y, Fp2._1);

    // check whether point is a valid one
    if (p.isValid()) {
      return p;
    } else {
      return null;
    }
  }

  @Override
  protected BN128<Fp2> zero() {
    return ZERO;
  }

  @Override
  protected BN128<Fp2> instance(Fp2 x, Fp2 y, Fp2 z) {
    return new BN128Fp2(x, y, z);
  }

  @Override
  protected Fp2 b() {
    return B_Fp2;
  }

  @Override
  protected Fp2 one() {
    return Fp2._1;
  }
}
