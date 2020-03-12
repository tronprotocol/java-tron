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
package org.tron.core.vm.trace;

import java.math.BigInteger;
import org.tron.core.vm.OpCode;

public class Op {

  private OpCode code;
  private int deep;
  private int pc;
  private BigInteger energy;
  private OpActions actions;

  public OpCode getCode() {
    return code;
  }

  public void setCode(OpCode code) {
    this.code = code;
  }

  public int getDeep() {
    return deep;
  }

  public void setDeep(int deep) {
    this.deep = deep;
  }

  public int getPc() {
    return pc;
  }

  public void setPc(int pc) {
    this.pc = pc;
  }

  public BigInteger getEnergy() {
    return energy;
  }

  public void setEnergy(BigInteger energy) {
    this.energy = energy;
  }

  public OpActions getActions() {
    return actions;
  }

  public void setActions(OpActions actions) {
    this.actions = actions;
  }
}
