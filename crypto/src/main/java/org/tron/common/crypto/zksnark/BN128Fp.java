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

import static org.tron.common.crypto.zksnark.Params.B_Fp;

/**
 * Definition of {@link BN128} over F_p, where "p" equals {@link Params#P} <br/>
 *
 * Curve equation: <br/> Y^2 = X^3 + b, where "b" equals {@link Params#B_Fp} <br/>
 *
 * @author Mikhail Kalinin
 * @since 21.08.2017
 */
public class BN128Fp extends BN128<Fp> {

  // the point at infinity
  static final BN128<Fp> ZERO = new BN128Fp(Fp.ZERO, Fp.ZERO, Fp.ZERO);

  protected BN128Fp(Fp x, Fp y, Fp z) {
    super(x, y, z);
  }

  /**
   * Checks whether x and y belong to Fp, then checks whether point with (x; y) coordinates lays on
   * the curve.
   *
   * Returns new point if all checks have been passed, otherwise returns null
   */
  public static BN128<Fp> create(byte[] xx, byte[] yy) {

    Fp x = Fp.create(xx);
    Fp y = Fp.create(yy);

    // check for point at infinity
    if (x.isZero() && y.isZero()) {
      return ZERO;
    }

    BN128<Fp> p = new BN128Fp(x, y, Fp._1);

    // check whether point is a valid one
    if (p.isValid()) {
      return p;
    } else {
      return null;
    }
  }

  @Override
  protected BN128<Fp> zero() {
    return ZERO;
  }

  @Override
  protected BN128<Fp> instance(Fp x, Fp y, Fp z) {
    return new BN128Fp(x, y, z);
  }

  @Override
  protected Fp b() {
    return B_Fp;
  }

  @Override
  protected Fp one() {
    return Fp._1;
  }
}
