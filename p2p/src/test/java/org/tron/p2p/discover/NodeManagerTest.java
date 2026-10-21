package org.tron.p2p.discover;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;

public class NodeManagerTest {
  @Test
  public void testNoSeeds() {
    P2pConfig config = new P2pConfig();
    Parameter.p2pConfig = config;
    try {
      NodeManager.init();
      Thread.sleep(100);
      Assert.assertEquals(0, NodeManager.getAllNodes().size());
      Assert.assertEquals(0, NodeManager.getTableNodes().size());
      Assert.assertEquals(0, NodeManager.getConnectableNodes().size());
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      NodeManager.close();
    }
  }
}
