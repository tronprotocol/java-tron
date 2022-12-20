package org.tron.core.vm;

import static org.tron.core.Constant.DYNAMIC_ENERGY_FACTOR_DECIMAL;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Program.JVMStackOverFlowException;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.program.Program.TransferException;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Type;
import org.tron.core.vm.repository.Value;

@Slf4j(topic = "VM")
public class VM {

  public static void play(Program program, JumpTable jumpTable) {
    try {
      ContractCapsule contextContract = null;
      long factor = DYNAMIC_ENERGY_FACTOR_DECIMAL;

      boolean allowDynamicEnergy =
          program.getContractState().getDynamicPropertiesStore().supportAllowDynamicEnergy();

      if (allowDynamicEnergy) {
        contextContract = program.getContractState().getContract(program.getContextAddress());

        if (contextContract.catchUpToCycle(
            program.getContractState().getDynamicPropertiesStore().getCurrentCycleNumber(),
            program.getContractState().getDynamicPropertiesStore().getDynamicEnergyThreshold(),
            program.getContractState().getDynamicPropertiesStore().getDynamicEnergyIncreaseFactor(),
            program.getContractState().getDynamicPropertiesStore().getDynamicEnergyMaxFactor())) {
          program.getContractState().putContract(
              Key.create(program.getContextAddress()),
              Value.create(contextContract, Type.DIRTY));
        }

        factor = contextContract.getEnergyFactor();
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
            contextContract.addEnergyUsage(energy);
            program.getContractState().putContract(
                Key.create(program.getContextAddress()),
                Value.create(contextContract, Type.DIRTY));

            if (factor != DYNAMIC_ENERGY_FACTOR_DECIMAL) {
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
