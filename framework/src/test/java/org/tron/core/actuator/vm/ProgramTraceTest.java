package org.tron.core.actuator.vm;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.vm.trace.Op;
import org.tron.core.vm.trace.OpActions;
import org.tron.core.vm.trace.ProgramTrace;

public class ProgramTraceTest {
  private static final String dbPath = "output_programTrace_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void programTraceTest() {

    ProgramTrace programTrace = new ProgramTrace();
    ProgramTrace anotherProgramTrace = new ProgramTrace();
    DataWord energyDataWord = new DataWord(4);
    OpActions opActions = new OpActions();
    byte addOpCode = org.tron.core.vm.Op.ADD;
    byte subOpCode = org.tron.core.vm.Op.SUB;
    programTrace.addOp(addOpCode, 2, 3, energyDataWord, opActions);
    anotherProgramTrace.addOp(subOpCode, 5, 6, energyDataWord, opActions);

    programTrace.merge(anotherProgramTrace);

    List<Op> ops = programTrace.getOps();
    Assert.assertFalse(ops.isEmpty());
    Assert.assertEquals(2, ops.size());
    for (Op op : ops) {
      if (op.getCode() == org.tron.core.vm.Op.ADD) {
        Assert.assertEquals(3, op.getDeep());
        Assert.assertEquals(2, op.getPc());
        Assert.assertEquals(BigInteger.valueOf(4), op.getEnergy());
      } else if (op.getCode() == org.tron.core.vm.Op.SUB) {
        Assert.assertEquals(6, op.getDeep());
        Assert.assertEquals(5, op.getPc());
        Assert.assertEquals(BigInteger.valueOf(4), op.getEnergy());
      } else {
        Assert.fail("Invalid op code");
      }
    }
  }


}
