package org.tron.core.capsule;

import static org.tron.core.Constant.DYNAMIC_ENERGY_DECREASE_DIVISION;
import static org.tron.core.Constant.DYNAMIC_ENERGY_FACTOR_DECIMAL;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.protos.contract.SmartContractOuterClass.ContractState;

@Slf4j(topic = "capsule")
public class ContractStateCapsule implements ProtoCapsule<ContractState> {

  private ContractState contractState;

  public ContractStateCapsule(ContractState contractState) {
    this.contractState = contractState;
  }

  public ContractStateCapsule(byte[] data) {
    try {
      this.contractState = SmartContractOuterClass.ContractState.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      // logger.debug(e.getMessage());
    }
  }

  @Override
  public byte[] getData() {
    return this.contractState.toByteArray();
  }

  @Override
  public ContractState getInstance() {
    return this.contractState;
  }

  public long getEnergyUsage() {
    return this.contractState.getEnergyUsage();
  }

  public void setEnergyUsage(long value) {
    this.contractState = this.contractState.toBuilder().setEnergyUsage(value).build();
  }

  public void addEnergyUsage(long toAdd) {
    setEnergyUsage(getEnergyUsage() + toAdd);
  }

  public long getEnergyFactor() {
    return this.contractState.getEnergyFactor();
  }

  public void setEnergyFactor(long value) {
    this.contractState = this.contractState.toBuilder().setEnergyFactor(value).build();
  }

  public long getUpdateCycle() {
    return this.contractState.getUpdateCycle();
  }

  public void setUpdateCycle(long value) {
    this.contractState = this.contractState.toBuilder().setUpdateCycle(value).build();
  }

  public void addUpdateCycle(long toAdd) {
    setUpdateCycle(getUpdateCycle() + toAdd);
  }

  public boolean catchUpToCycle(long newCycle, long threshold, long increaseFactor, long maxFactor) {
    long lastCycle = getUpdateCycle();
    if (lastCycle == newCycle) {
      return false;
    }

    if (lastCycle > newCycle || lastCycle == 0L || newCycle - lastCycle >= 10) {
      this.contractState = this.contractState.toBuilder()
          .setUpdateCycle(newCycle)
          .setEnergyUsage(0L)
          .setEnergyFactor(DYNAMIC_ENERGY_FACTOR_DECIMAL)
          .build();
      return true;
    }

    // increase first
    if (getEnergyUsage() >= threshold) {
      lastCycle += 1;
      this.contractState = this.contractState.toBuilder()
          .setUpdateCycle(lastCycle)
          .setEnergyUsage(0L)
          .setEnergyFactor(Math.min(
              maxFactor,
              getEnergyFactor() * increaseFactor / DYNAMIC_ENERGY_FACTOR_DECIMAL))
          .build();
    }

    // decrease
    long cycleCount = newCycle - lastCycle;
    if (cycleCount <= 0) {
      return true;
    }

    // no need to decrease
    if (getEnergyFactor() <= DYNAMIC_ENERGY_FACTOR_DECIMAL) {
      this.contractState = this.contractState.toBuilder()
          .setUpdateCycle(newCycle)
          .setEnergyUsage(0L)
          .setEnergyFactor(DYNAMIC_ENERGY_FACTOR_DECIMAL)
          .build();
      return true;
    }

    double decreaseFactor = 1 - ((double) increaseFactor / DYNAMIC_ENERGY_FACTOR_DECIMAL - 1)
        / DYNAMIC_ENERGY_DECREASE_DIVISION;
    if (cycleCount > 1) {
      decreaseFactor = Math.pow(
          decreaseFactor,
          cycleCount);
    }

    this.contractState = this.contractState.toBuilder()
        .setUpdateCycle(newCycle)
        .setEnergyUsage(0L)
        .setEnergyFactor(Math.max(
            DYNAMIC_ENERGY_FACTOR_DECIMAL,
            (long) (getEnergyFactor() * decreaseFactor)))
        .build();

    return true;
  }
}
