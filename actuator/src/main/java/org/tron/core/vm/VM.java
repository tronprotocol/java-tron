package org.tron.core.vm;

import static org.tron.core.Constant.DYNAMIC_ENERGY_FACTOR_DECIMAL;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Program.JVMStackOverFlowException;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.program.Program.TransferException;

@Slf4j(topic = "VM")
public class VM {

  public static void play(Program program, JumpTable jumpTable) {
    try {
      ContractStateCapsule contextContractState = null;
      long factor = DYNAMIC_ENERGY_FACTOR_DECIMAL;
      long energyUsage = 0L;

      boolean allowDynamicEnergy =
          program.getContractState().getDynamicPropertiesStore().supportAllowDynamicEnergy();

      if (allowDynamicEnergy) {
        contextContractState = program.getContractState().getContractState(program.getContextAddress());

        if (contextContractState.catchUpToCycle(
            program.getContractState().getDynamicPropertiesStore().getCurrentCycleNumber(),
            program.getContractState().getDynamicPropertiesStore().getDynamicEnergyThreshold(),
            program.getContractState().getDynamicPropertiesStore().getDynamicEnergyIncreaseFactor(),
            program.getContractState().getDynamicPropertiesStore().getDynamicEnergyMaxFactor())) {

          program.getContractState().updateContractState(
              program.getContextAddress(), contextContractState);
        }

        factor = contextContractState.getEnergyFactor();
      }

      while (!program.isStopped()) {
        if (VMConfig.vmTrace()) {
          program.saveOpTrace();
        }

        try {
          Operation op = jumpTable.get(program.getCurrentOpIntValue());
          if (!op.isEnabled()) {
            throw Program.Exception.invalidOpCode(program.getCurrentOp());
          }
          program.setLastOp((byte) op.getOpcode());

          /* stack underflow/overflow check */
          program.verifyStackSize(op.getRequire());
          program.verifyStackOverflow(op.getRequire(), op.getRet());

          String opName = Op.getNameOf(op.getOpcode());
          /* spend energy before execution */
          long energy = op.getEnergyCost(program);
          if (allowDynamicEnergy) {
            energyUsage += energy;

            if (factor > DYNAMIC_ENERGY_FACTOR_DECIMAL) {
              energy = energy * factor / DYNAMIC_ENERGY_FACTOR_DECIMAL;
            }
          }
          program.spendEnergy(energy, opName);

          /* check if cpu time out */
          program.checkCPUTimeLimit(opName);

          /* exec op action */
          op.execute(program);

          program.setPreviouslyExecutedOp((byte) op.getOpcode());
        } catch (RuntimeException e) {
          logger.info("VM halted: [{}]", e.getMessage());
          if (!(e instanceof TransferException)) {
            program.spendAllEnergy();
          }
          //program.resetFutureRefund();
          program.stop();
          throw e;
        } finally {
          program.fullTrace();
        }
      }

      if (allowDynamicEnergy) {
        contextContractState = program.getContractState().getContractState(program.getContextAddress());
        contextContractState.addEnergyUsage(energyUsage);
        program.getContractState().updateContractState(
            program.getContextAddress(),
            contextContractState);
      }

    } catch (JVMStackOverFlowException | OutOfTimeException e) {
      throw e;
    } catch (RuntimeException e) {
      if (StringUtils.isEmpty(e.getMessage())) {
        logger.warn("Unknown Exception occurred, tx id: {}",
            Hex.toHexString(program.getRootTransactionId()), e);
        program.setRuntimeFailure(new RuntimeException("Unknown Exception"));
      } else {
        program.setRuntimeFailure(e);
      }
    } catch (StackOverflowError soe) {
      logger.info("\n !!! StackOverflowError: update your java run command with -Xss !!!\n", soe);
      throw new JVMStackOverFlowException();
    }
  }
}
