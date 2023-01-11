package org.tron.core.services.stop;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.tron.common.parameter.CommonParameter;

@Slf4j
public class BlockSyncCountStopTest extends ConditionallyStopTest {

  private static final long sync = 512;

  protected void initParameter(CommonParameter parameter) {
    parameter.setShutdownBlockCount(sync);
  }

  @Override
  protected void check() throws Exception {

    Assert.assertEquals(sync + currentHeader, dbManager
        .getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB());
  }

  @Override
  protected void initDbPath() {
    dbPath = "output-sync-stop";
  }

}
