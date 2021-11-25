package org.tron.core.vm;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Program.JVMStackOverFlowException;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.program.Program.TransferException;

@Slf4j(topic = "VM")
public class VM {

  private final VMConfig config;

  public VM() {
    config = VMConfig.getInstance();
  }

  public VM(VMConfig config) {
    this.config = config;
  }

  public void play(Program program) {
    try {

      while (!program.isStopped()) {
        if (VMConfig.vmTrace()) {
          program.saveOpTrace();
        }
        try {
          Operation op = OperationRegistry.get(program.getCurrentOpIntValue());
          if (op == null) {
            throw Program.Exception.invalidOpCode(program.getCurrentOp());
          }
          program.setLastOp((byte) op.getOpcode());
          program.verifyStackSize(op.getRequire());
          //Check not exceeding stack limits
          program.verifyStackOverflow(op.getRequire(), op.getRet());

          program.spendEnergy(op.getEnergyCost(program), Op.getNameOf(op.getOpcode()));
          program.checkCPUTimeLimit(Op.getNameOf(op.getOpcode()));
          op.execute(program);
          program.setPreviouslyExecutedOp((byte) op.getOpcode());
        } catch (RuntimeException e) {
          logger.info("VM halted: [{}]", e.getMessage());
          if (!(e instanceof TransferException)) {
            program.spendAllEnergy();
          }
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
