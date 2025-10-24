package org.tron.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.entity.NodeInfo;
import org.tron.common.entity.NodeInfo.MachineInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.DeadLockThreadInfo;
import org.tron.common.entity.PeerInfo;

public class EntityTest {

  private final MachineInfo machineInfo = new MachineInfo();
  private final DeadLockThreadInfo deadLockThreadInfo = new DeadLockThreadInfo();

  @Before
  public void setup() {
    deadLockThreadInfo.setName("name");
    deadLockThreadInfo.setLockName("lockName");
    deadLockThreadInfo.setLockOwner("lockOwner");
    deadLockThreadInfo.setState("state");
    deadLockThreadInfo.setStackTrace("stackTrace");
    deadLockThreadInfo.setWaitTime(0L);
    deadLockThreadInfo.setBlockTime(0L);
    machineInfo.setDeadLockThreadInfoList(Lists.newArrayList(deadLockThreadInfo));
    machineInfo.setJavaVersion("1.8");
    machineInfo.setOsName("linux");
  }

  @Test
  public void testMachineInfo() {
    machineInfo.setDeadLockThreadCount(3);
    assertTrue(CollectionUtils.isNotEmpty(machineInfo.getDeadLockThreadInfoList()));
    assertEquals(3, machineInfo.getDeadLockThreadCount());

  }

  @Test
  public void testDeadLockThreadInfo() {
    assertEquals("name", deadLockThreadInfo.getName());
    assertEquals("lockName", deadLockThreadInfo.getLockName());
    assertEquals("lockOwner", deadLockThreadInfo.getLockOwner());
    assertEquals("state", deadLockThreadInfo.getState());
    assertEquals("stackTrace", deadLockThreadInfo.getStackTrace());
    assertEquals(0, deadLockThreadInfo.getBlockTime());
    assertEquals(0, deadLockThreadInfo.getWaitTime());

  }

  @Test
  public void testNodeInfo() {
    List<PeerInfo> peerInfoList = new ArrayList<>();
    peerInfoList.add(getDefaultPeerInfo());

    NodeInfo nodeInfo = new NodeInfo();
    nodeInfo.setTotalFlow(1L);
    nodeInfo.setCheatWitnessInfoMap(new HashMap<>());
    assertEquals(1, nodeInfo.getTotalFlow());
    assertNotNull(nodeInfo.getCheatWitnessInfoMap());
    nodeInfo.setMachineInfo(machineInfo);
    nodeInfo.setBlock("block");
    nodeInfo.setSolidityBlock("solidityBlock");
    nodeInfo.setPeerList(peerInfoList);
    nodeInfo.transferToProtoEntity();
  }

  private PeerInfo getDefaultPeerInfo() {
    PeerInfo peerInfo = new PeerInfo();
    peerInfo.setAvgLatency(peerInfo.getAvgLatency());
    peerInfo.setBlockInPorcSize(peerInfo.getBlockInPorcSize());
    peerInfo.setConnectTime(peerInfo.getConnectTime());
    peerInfo.setDisconnectTimes(peerInfo.getDisconnectTimes());
    peerInfo.setHeadBlockTimeWeBothHave(peerInfo.getHeadBlockTimeWeBothHave());
    peerInfo.setHeadBlockWeBothHave(peerInfo.getHeadBlockWeBothHave());
    peerInfo.setHost("host");
    peerInfo.setInFlow(peerInfo.getInFlow());
    peerInfo.setLastBlockUpdateTime(peerInfo.getLastBlockUpdateTime());
    peerInfo.setLastSyncBlock("last");
    peerInfo.setLocalDisconnectReason("localDisconnectReason");
    peerInfo.setNodeCount(peerInfo.getNodeCount());
    peerInfo.setNodeId("nodeId");
    peerInfo.setHeadBlockWeBothHave("headBlockWeBothHave");
    peerInfo.setRemainNum(peerInfo.getRemainNum());
    peerInfo.setRemoteDisconnectReason("remoteDisconnectReason");
    peerInfo.setScore(peerInfo.getScore());
    peerInfo.setPort(peerInfo.getPort());
    peerInfo.setSyncFlag(peerInfo.isSyncFlag());
    peerInfo.setNeedSyncFromPeer(peerInfo.isNeedSyncFromPeer());
    peerInfo.setNeedSyncFromUs(peerInfo.isNeedSyncFromUs());
    peerInfo.setSyncToFetchSize(peerInfo.getSyncToFetchSize());
    peerInfo.setSyncToFetchSizePeekNum(peerInfo.getSyncToFetchSizePeekNum());
    peerInfo.setSyncBlockRequestedSize(peerInfo.getSyncBlockRequestedSize());
    peerInfo.setUnFetchSynNum(peerInfo.getUnFetchSynNum());
    peerInfo.setActive(peerInfo.isActive());

    return peerInfo;
  }
}
