package org.tron.core.actuator.vm;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionStoreTest;
import org.tron.core.vm.trace.OpActions;
import org.tron.core.vm.trace.OpActions.Action;
import org.tron.core.vm.trace.ProgramTraceListener;

@Slf4j(topic = "VM")
public class ProgramTraceListenerTest {
  private static final String dbPath = "output_programTraceListener_test";

  private static final int WORD_SIZE = 32;
  private ProgramTraceListener traceListener;
  private ProgramTraceListener disableTraceListener;
  private DataWord stackWord = new DataWord(1);
  private DataWord storageWordKey = new DataWord(2);
  private DataWord storageWordValue = new DataWord(3);

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);

  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  private void invokeProgramTraceListener(ProgramTraceListener traceListener) {
    traceListener.onMemoryExtend(WORD_SIZE);
    traceListener.onMemoryWrite(10000, TransactionStoreTest.randomBytes(WORD_SIZE), WORD_SIZE);

    traceListener.onStackPush(stackWord);
    traceListener.onStackSwap(10000, 20000);
    traceListener.onStackPop();

    traceListener.onStoragePut(storageWordKey, storageWordValue);
    traceListener.onStorageClear();
  }

  private void validateProgramTraceListener() {
    Field field;
    OpActions opActions;
    try {
      field = traceListener.getClass().getDeclaredField("actions");
      field.setAccessible(true);
      opActions = (OpActions) field.get(traceListener);
      List<Action> memory = opActions.getMemory();
      List<Action> stack = opActions.getStack();
      List<Action> storage = opActions.getStorage();
      Assert.assertEquals(2, memory.size());
      Assert.assertEquals(3, stack.size());
      Assert.assertEquals(2, storage.size());

      Map<String, Object> params;
      for (Action memoryAction : memory) {

        params = memoryAction.getParams();
        switch (memoryAction.getName()) {
          case extend:
            Assert.assertEquals(1, params.size());
            Assert.assertTrue(params.containsKey("delta"));
            Assert.assertEquals(Integer.toString(WORD_SIZE), params.get("delta"));
            break;
          case write:
            Assert.assertEquals(2, params.size());
            Assert.assertEquals(Integer.toString(10000), params.get("address"));
            break;
          default:
            break;
        }
      }
      for (Action stackAction : stack) {
        params = stackAction.getParams();
        switch (stackAction.getName()) {
          case push:
            Assert.assertEquals(1, params.size());
            Assert.assertEquals(stackWord.toString(), params.get("value"));
            break;
          case swap:
            Assert.assertEquals(2, params.size());
            Assert.assertEquals(Integer.toString(10000), params.get("from"));
            Assert.assertEquals(Integer.toString(20000), params.get("to"));
            break;
          case pop:
            Assert.assertEquals(null, params);
            break;
          default:
            break;
        }
      }

      for (Action storageAction : storage) {
        params = storageAction.getParams();
        switch (storageAction.getName()) {
          case put:
            Assert.assertEquals(2, params.size());
            Assert.assertEquals(storageWordKey.toString(), params.get("key"));
            Assert.assertEquals(storageWordValue.toString(), params.get("value"));
            break;
          case clear:
            Assert.assertEquals(null, params);
            break;
          default:
            break;
        }
      }
    } catch (NoSuchFieldException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof NoSuchFieldException);
    } catch (IllegalAccessException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof IllegalAccessException);
    }


    traceListener.resetActions();

    try {
      field = traceListener.getClass().getDeclaredField("actions");
      field.setAccessible(true);
      opActions = (OpActions) field.get(traceListener);
      Assert.assertEquals(0, opActions.getMemory().size());
      Assert.assertEquals(0, opActions.getStack().size());
      Assert.assertEquals(0, opActions.getStorage().size());
    } catch (NoSuchFieldException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof NoSuchFieldException);
    } catch (IllegalAccessException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof IllegalAccessException);
    }
  }

  public void validateDisableTraceListener() {
    try {
      Field field = disableTraceListener.getClass().getDeclaredField("actions");
      field.setAccessible(true);
      OpActions opActions = (OpActions) field.get(disableTraceListener);
      Assert.assertEquals(0, opActions.getMemory().size());
      Assert.assertEquals(0, opActions.getStorage().size());
      Assert.assertEquals(0, opActions.getStack().size());
    } catch (NoSuchFieldException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof NoSuchFieldException);
    } catch (IllegalAccessException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof IllegalAccessException);
    }
  }


  @Test
  public void programTraceListenerTest() {
    traceListener = new ProgramTraceListener(true);
    disableTraceListener = new ProgramTraceListener(false);

    invokeProgramTraceListener(traceListener);
    invokeProgramTraceListener(disableTraceListener);

    validateProgramTraceListener();
    validateDisableTraceListener();
  }

}
