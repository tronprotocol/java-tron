package org.tron.core.actuator;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;


@Slf4j(topic = "actuator")
public class ActuatorConstantTest {

  private static final String dbPath = "output_actuatorConstant_test";
  public static Application AppT;
  private static TronApplicationContext context;

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void variablecheck() {
    ActuatorConstant actuator = new ActuatorConstant();
    Assert.assertEquals("Account[", actuator.ACCOUNT_EXCEPTION_STR);
    Assert.assertEquals("Witness[", actuator.WITNESS_EXCEPTION_STR);
    Assert.assertEquals("Proposal[", actuator.PROPOSAL_EXCEPTION_STR);
    Assert.assertEquals("] not exists", actuator.NOT_EXIST_STR);
    Assert.assertTrue(actuator instanceof ActuatorConstant);
  }

}
