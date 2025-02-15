package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.bloom.Bloom;

public class LogsFilterCapsuleTest {

  private LogsFilterCapsule capsule;

  @Before
  public void setUp() {
    capsule = new LogsFilterCapsule(0,
        "e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f",
        new Bloom(), new ArrayList<>(), true, false);
  }

  @Test
  public void testSetAndGetLogsFilterCapsule() {
    capsule.setBlockNumber(capsule.getBlockNumber());
    capsule.setBlockHash(capsule.getBlockHash());
    capsule.setSolidified(capsule.isSolidified());
    capsule.setBloom(capsule.getBloom());
    capsule.setRemoved(capsule.isRemoved());
    capsule.setTxInfoList(capsule.getTxInfoList());
    assertNotNull(capsule.toString());
    capsule.processFilterTrigger();
  }

}
