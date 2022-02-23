package org.tron.core.vm;

import java.util.Arrays;

public class JumpTable {

  private static final Operation UNDEFINED =
      new Operation(
          -1, 0, 0,
          p -> 0L,
          p -> { },
          () -> false);

  private final Operation[] table = new Operation[256];

  public JumpTable() {
    // fill all op slots to undefined
    Arrays.fill(table, UNDEFINED);
  }
  
  public Operation get(int op) {
    return table[op];
  }

  public void set(Operation op) {
    table[op.getOpcode()] = op;
  }
}
