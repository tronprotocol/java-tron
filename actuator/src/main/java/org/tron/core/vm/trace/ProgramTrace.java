package org.tron.core.vm.trace;

import static java.lang.String.format;
import static org.tron.common.utils.ByteArray.toHexString;
import static org.tron.core.vm.trace.Serializers.serializeFieldsOnly;

import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.invoke.ProgramInvoke;

public class ProgramTrace {

  private List<Op> ops = new ArrayList<>();
  private String result;
  private String error;
  private String contractAddress;

  public ProgramTrace() {
    this(null);
  }

  public ProgramTrace(ProgramInvoke programInvoke) {
    if (programInvoke != null && VMConfig.vmTrace()) {
      contractAddress = Hex.toHexString(programInvoke.getContractAddress().toTronAddress());
    }
  }

  public List<Op> getOps() {
    return ops;
  }

  public void setOps(List<Op> ops) {
    this.ops = ops;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public ProgramTrace result(byte[] result) {
    setResult(toHexString(result));
    return this;
  }

  public ProgramTrace error(Exception error) {
    setError(error == null ? "" : format("%s: %s", error.getClass(), error.getMessage()));
    return this;
  }

  public Op addOp(byte code, int pc, int deep, DataWord energy, OpActions actions) {
    Op op = new Op();
    op.setActions(actions);
    op.setCode(code & 0xff);
    op.setDeep(deep);
    op.setEnergy(energy.value());
    op.setPc(pc);

    ops.add(op);

    return op;
  }

  /**
   * Used for merging sub calls execution.
   */
  public void merge(ProgramTrace programTrace) {
    this.ops.addAll(programTrace.ops);
  }

  public String asJsonString(boolean formatted) {
    return serializeFieldsOnly(this, formatted);
  }

  @Override
  public String toString() {
    return asJsonString(true);
  }
}
