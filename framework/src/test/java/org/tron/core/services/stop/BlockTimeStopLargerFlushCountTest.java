package org.tron.core.services.stop;

import org.tron.common.parameter.CommonParameter;

public class BlockTimeStopLargerFlushCountTest extends BlockTimeStopTest {

  @Override
  protected void initParameter(CommonParameter parameter) {
    super.initParameter(parameter);
    parameter.getStorage().setMaxFlushCount(20);
  }
}
