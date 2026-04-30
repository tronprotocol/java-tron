package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.bloom.Bloom;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;

public class LogsFilterCapsuleTest {

  private LogsFilterCapsule capsule;
  private TronJsonRpcImpl jsonRpc;

  @Before
  public void setUp() {
    jsonRpc = new TronJsonRpcImpl(null, null, null);
    capsule = new LogsFilterCapsule(0,
        "e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f",
        new Bloom(), new ArrayList<>(), true, false, jsonRpc);
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
