package org.tron.core.net.peer;

import org.junit.Assert;
import org.junit.Test;

public class PeerConnectionTest {

  @Test
  public void testVariableDefaultValue() {
    PeerConnection peerConnection = new PeerConnection();
    Assert.assertTrue(!peerConnection.isBadPeer());
    Assert.assertTrue(!peerConnection.isFetchAble());
    Assert.assertTrue(peerConnection.isIdle());
    Assert.assertTrue(!peerConnection.isRelayPeer());
    Assert.assertTrue(peerConnection.isNeedSyncFromPeer());
    Assert.assertTrue(peerConnection.isNeedSyncFromUs());
    Assert.assertTrue(!peerConnection.isSyncFinish());
  }
}
