package org.tron.core.vm;

import static org.tron.core.Constant.DYNAMIC_ENERGY_FACTOR_DECIMAL;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tron.core.trace.TracerManager;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Program.JVMStackOverFlowException;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.program.Program.TransferException;

@Slf4j(topic = "VM")
public class VM {

  private static final Set<Integer> CALL_OPS = ImmutableSet.of(Op.CALL, Op.STATICCALL,
      Op.DELEGATECALL, Op.CALLCODE, Op.CALLTOKEN);

  public static void play(Program program, JumpTable jumpTable) {
    Operation op = null;
    String opName = null;
    long energy = 0;

    try {
      long factor = DYNAMIC_ENERGY_FACTOR_DECIMAL;
      long energyUsage = 0L;

      if (VMConfig.allowDynamicEnergy()) {
        factor = program.updateContextContractFactor();
      }

      while (!program.isStopped()) {
        if (VMConfig.vmTrace()) {
          program.saveOpTrace();
        }

        try {
          op = jumpTable.get(program.getCurrentOpIntValue());
          if (!op.isEnabled()) {
            throw Program.Exception.invalidOpCode(program.getCurrentOp());
          }
          program.setLastOp((byte) op.getOpcode());

          /* stack underflow/overflow check */
          program.verifyStackSize(op.getRequire());
          program.verifyStackOverflow(op.getRequire(), op.getRet());

          opName = Op.getNameOf(op.getOpcode());
          /* spend energy before execution */
          energy = op.getEnergyCost(program);
          if (VMConfig.allowDynamicEnergy()) {
            long actualEnergy = energy;
            // CALL Ops have special calculation on energy.
            if (CALL_OPS.contains(op.getOpcode())) {
              actualEnergy = energy
                  - program.getAdjustedCallEnergy().longValueSafe()
                  - program.getCallPenaltyEnergy();
            }
            energyUsage += actualEnergy;

            if (factor > DYNAMIC_ENERGY_FACTOR_DECIMAL) {
              long penalty;

              // CALL Ops have special calculation on energy.
              if (CALL_OPS.contains(op.getOpcode())) {
                penalty = program.getCallPenaltyEnergy();
              } else {
                penalty = energy * factor / DYNAMIC_ENERGY_FACTOR_DECIMAL - energy;
                if (penalty < 0) {
                  penalty = 0;
                }
                energy += penalty;
              }

              program.spendEnergyWithPenalty(energy, penalty, opName);
            } else {
              program.spendEnergy(energy, opName);
            }

          } else {
            program.spendEnergy(energy, opName);
          }


          /* check if cpu time out */
          program.checkCPUTimeLimit(opName);

          TracerManager.getTracer().captureState(op.getOpcode(), opName, energy, program.getPC(), program.getCallDeep());

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

          TracerManager.getTracer().captureFault(
                  op.getOpcode(),
                  opName,
                  program.getEnergylimitLeftLong(),
                  program.getStack(),
                  program.getCallerAddress().getData(),
                  program.getContractAddress().getData(),
                  program.getCallValue().getData(),
                  program.getPC(),
                  program.getMemory(),
                  program.getCallDeep(),
                  e
          );

          // We need to exit from call anyway, even if call was executed with error.
          TracerManager.getTracer().captureExit(program.getEnergylimitLeftLong(), e);

          throw e;
        } finally {
          program.fullTrace();
        }
      }

      if (VMConfig.allowDynamicEnergy()) {
        program.addContextContractUsage(energyUsage);
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
