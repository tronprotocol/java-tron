package org.tron.program;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;

public class ManagerAndManagerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testManager() {
    Args.setParam(new String[]{"-d", "111"}, Configuration.getByPath("config-junit.conf"));
    logger.info("1111:" + Args.getInstance().getOutputDirectory());
    Args.setParam(new String[]{"-d", "222"}, Configuration.getByPath("config-junit.conf"));
    logger.info("2222:" + Args.getInstance().getOutputDirectory());
    Args.clearParam();
    Args.setParam(new String[]{}, Configuration.getByPath("config-junit.conf"));
    logger.info("3333:" + Args.getInstance().getOutputDirectory());

  }

}
