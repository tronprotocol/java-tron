package org.tron.core.actuator;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;


@Slf4j(topic = "actuator")
public class ActuatorConstantTest extends BaseTest {

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    dbPath = "output_actuatorConstant_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
  }

  @Test
  public void variableCheck() {
    ActuatorConstant actuator = new ActuatorConstant();
    Assert.assertEquals("Account[", actuator.ACCOUNT_EXCEPTION_STR);
    Assert.assertEquals("Witness[", actuator.WITNESS_EXCEPTION_STR);
    Assert.assertEquals("Proposal[", actuator.PROPOSAL_EXCEPTION_STR);
    Assert.assertEquals("] not exists", actuator.NOT_EXIST_STR);
  }

}
