package org.tron.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.HashMap;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.entity.NodeInfo;
import org.tron.common.entity.NodeInfo.MachineInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.DeadLockThreadInfo;

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
    NodeInfo nodeInfo = new NodeInfo();
    nodeInfo.setTotalFlow(1L);
    nodeInfo.setCheatWitnessInfoMap(new HashMap<>());
    assertEquals(1, nodeInfo.getTotalFlow());
    assertNotNull(nodeInfo.getCheatWitnessInfoMap());
    nodeInfo.setMachineInfo(machineInfo);
    nodeInfo.setBlock("block");
    nodeInfo.setSolidityBlock("solidityBlock");
    nodeInfo.transferToProtoEntity();
  }
}
