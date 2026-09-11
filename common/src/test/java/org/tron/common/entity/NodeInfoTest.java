package org.tron.common.entity;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.protos.Protocol;

public class NodeInfoTest {

  private PeerInfo newPeerInfo(boolean syncFlag, boolean needSyncFromPeer,
      boolean needSyncFromUs) {
    PeerInfo peerInfo = new PeerInfo();
    peerInfo.setSyncFlag(syncFlag);
    peerInfo.setNeedSyncFromPeer(needSyncFromPeer);
    peerInfo.setNeedSyncFromUs(needSyncFromUs);
    // string fields must be non-null, otherwise the protobuf setters throw NPE
    peerInfo.setLastSyncBlock("");
    peerInfo.setHost("127.0.0.1");
    peerInfo.setNodeId("");
    peerInfo.setHeadBlockWeBothHave("");
    peerInfo.setLocalDisconnectReason("");
    peerInfo.setRemoteDisconnectReason("");
    return peerInfo;
  }

  /**
   * The protobuf conversion must map each peer flag from its own source field. A previous
   * copy-and-paste defect populated needSyncFromPeer from isSyncFlag(); distinct values for
   * syncFlag and needSyncFromPeer are required so that such a mismatch is detected.
   */
  @Test
  public void testPeerFlagMappingIsIndependent() {
    NodeInfo nodeInfo = new NodeInfo();
    nodeInfo.setBlock("");
    nodeInfo.setSolidityBlock("");
    List<PeerInfo> peerList = new ArrayList<>();
    // syncFlag != needSyncFromPeer so the two fields cannot be confused
    peerList.add(newPeerInfo(false, true, false));
    peerList.add(newPeerInfo(true, false, true));
    nodeInfo.setPeerList(peerList);
    nodeInfo.setCheatWitnessInfoMap(new java.util.HashMap<>());

    Protocol.NodeInfo proto = nodeInfo.transferToProtoEntity();

    Assert.assertEquals(2, proto.getPeerInfoListCount());

    Protocol.NodeInfo.PeerInfo peer0 = proto.getPeerInfoList(0);
    Assert.assertFalse(peer0.getSyncFlag());
    Assert.assertTrue(peer0.getNeedSyncFromPeer());
    Assert.assertFalse(peer0.getNeedSyncFromUs());

    Protocol.NodeInfo.PeerInfo peer1 = proto.getPeerInfoList(1);
    Assert.assertTrue(peer1.getSyncFlag());
    Assert.assertFalse(peer1.getNeedSyncFromPeer());
    Assert.assertTrue(peer1.getNeedSyncFromUs());
  }
}
