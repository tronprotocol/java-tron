package org.tron.core.vm;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import org.tron.core.vm.program.Program;

public class Operation {

  private final int opcode;
  private final int require;
  private final int ret;
  private final Function<Program, Long> cost;
  private final Consumer<Program> action;
  private final BooleanSupplier enabled;

  public Operation(int opcode, int require, int ret,
                      Function<Program, Long> cost, Consumer<Program> action) {
    this(opcode, require, ret, cost, action, () -> true);
  }

  public Operation(int opcode, int require, int ret,
      Function<Program, Long> cost, Consumer<Program> action, BooleanSupplier enabled) {
    this.opcode = opcode;
    this.require = require;
    this.ret = ret;
    this.cost = cost;
    this.action = action;
    this.enabled = enabled;
  }

  public int getOpcode() {
    return opcode;
  }

  public int getRequire() {
    return require;
  }

  public int getRet() {
    return ret;
  }

  public long getEnergyCost(Program program) {
    return this.cost.apply(program);
  }

  public void execute(Program program) {
    this.action.accept(program);
  }

  public boolean isEnabled() {
    return enabled.getAsBoolean();
  }

  public Operation adjustCost(Function<Program, Long> newCost) {
    return new Operation(opcode, require, ret, newCost, action, enabled);
  }

  public Operation adjustAction(Consumer<Program> newAction) {
    return new Operation(opcode, require, ret, cost, newAction, enabled);
  }
}
