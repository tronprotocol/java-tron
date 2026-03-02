package org.tron.core.vm.trace;

import java.math.BigInteger;

public class Op {

  private int code;
  private int deep;
  private int pc;
  private BigInteger energy;
  private OpActions actions;

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
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
