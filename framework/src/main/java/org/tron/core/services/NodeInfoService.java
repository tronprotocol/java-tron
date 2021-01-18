package org.tron.core.services;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.NodeInfo;
import org.tron.common.entity.NodeInfo.ConfigNodeInfo;
import org.tron.common.entity.NodeInfo.MachineInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.DeadLockThreadInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.MemoryDescInfo;
import org.tron.common.entity.PeerInfo;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.Manager;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.WitnessProductBlockService.CheatWitnessInfo;
import org.tron.program.Version;
import org.tron.protos.Protocol.ReasonCode;

@Component
public class NodeInfoService {

  private MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
  private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
      .getOperatingSystemMXBean();
  private CommonParameter parameter = CommonParameter.getInstance();

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private WitnessProductBlockService witnessProductBlockService;

  public NodeInfo getNodeInfo() {
    NodeInfo nodeInfo = new NodeInfo();
    setConnectInfo(nodeInfo);
    setMachineInfo(nodeInfo);
    setConfigNodeInfo(nodeInfo);
    setBlockInfo(nodeInfo);
    setCheatWitnessInfo(nodeInfo);
    return nodeInfo;
  }

  private void setMachineInfo(NodeInfo nodeInfo) {
    MachineInfo machineInfo = new MachineInfo();
    machineInfo.setThreadCount(threadMXBean.getThreadCount());
    machineInfo.setCpuCount(Runtime.getRuntime().availableProcessors());
    machineInfo.setTotalMemory(operatingSystemMXBean.getTotalPhysicalMemorySize());
    machineInfo.setFreeMemory(operatingSystemMXBean.getFreePhysicalMemorySize());
    machineInfo.setCpuRate(operatingSystemMXBean.getSystemCpuLoad());
    machineInfo.setJavaVersion(runtimeMXBean.getSystemProperties().get("java.version"));
    machineInfo
        .setOsName(operatingSystemMXBean.getName() + " " + operatingSystemMXBean.getVersion());
    machineInfo.setJvmTotalMemory(memoryMXBean.getHeapMemoryUsage().getMax());
    machineInfo.setJvmFreeMemory(
        memoryMXBean.getHeapMemoryUsage().getMax() - memoryMXBean.getHeapMemoryUsage().getUsed());
    machineInfo.setProcessCpuRate(operatingSystemMXBean.getProcessCpuLoad());
    List<MemoryDescInfo> memoryDescInfoList = new ArrayList<>();
    List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
    if (CollectionUtils.isNotEmpty(pools)) {
      for (MemoryPoolMXBean pool : pools) {
        MemoryDescInfo memoryDescInfo = new MemoryDescInfo();
        memoryDescInfo.setName(pool.getName());
        memoryDescInfo.setInitSize(pool.getUsage().getInit());
        memoryDescInfo.setUseSize(pool.getUsage().getUsed());
        memoryDescInfo.setMaxSize(pool.getUsage().getMax());
        if (pool.getUsage().getMax() > 0) {
          memoryDescInfo.setUseRate((double) pool.getUsage().getUsed() / pool.getUsage().getMax());
        } else {
          memoryDescInfo
              .setUseRate((double) pool.getUsage().getUsed() / pool.getUsage().getCommitted());
        }
        memoryDescInfoList.add(memoryDescInfo);
      }
    }
    machineInfo.setMemoryDescInfoList(memoryDescInfoList);
    //dead lock thread
    long[] deadlockedIds = threadMXBean.findDeadlockedThreads();
    if (ArrayUtils.isNotEmpty(deadlockedIds)) {
      machineInfo.setDeadLockThreadCount(deadlockedIds.length);
      ThreadInfo[] deadlockInfos = threadMXBean.getThreadInfo(deadlockedIds);
      List<DeadLockThreadInfo> deadLockThreadInfoList = new ArrayList<>();
      for (ThreadInfo deadlockInfo : deadlockInfos) {
        DeadLockThreadInfo deadLockThreadInfo = new DeadLockThreadInfo();
        deadLockThreadInfo.setName(deadlockInfo.getThreadName());
        deadLockThreadInfo.setLockName(deadlockInfo.getLockName());
        deadLockThreadInfo.setLockOwner(deadlockInfo.getLockOwnerName());
        deadLockThreadInfo.setBlockTime(deadlockInfo.getBlockedTime());
        deadLockThreadInfo.setWaitTime(deadlockInfo.getWaitedTime());
        deadLockThreadInfo.setState(deadlockInfo.getThreadState().name());
        deadLockThreadInfo.setStackTrace(Arrays.toString(deadlockInfo.getStackTrace()));
        deadLockThreadInfoList.add(deadLockThreadInfo);
      }
      machineInfo.setDeadLockThreadInfoList(deadLockThreadInfoList);
    }
    nodeInfo.setMachineInfo(machineInfo);
  }

  private void setConnectInfo(NodeInfo nodeInfo) {
    nodeInfo.setCurrentConnectCount(syncPool.getActivePeers().size());
    nodeInfo.setActiveConnectCount(syncPool.getActivePeersCount().get());
    nodeInfo.setPassiveConnectCount(syncPool.getPassivePeersCount().get());
    long totalFlow = 0;
    List<PeerInfo> peerInfoList = new ArrayList<>();
    for (PeerConnection peerConnection : syncPool.getActivePeers()) {
      PeerInfo peerInfo = new PeerInfo();
      peerInfo.setHeadBlockWeBothHave(peerConnection.getBlockBothHave().getString());
      peerInfo.setActive(peerConnection.isActive());
      peerInfo.setAvgLatency(peerConnection.getNodeStatistics().pingMessageLatency.getAvg());
      peerInfo.setBlockInPorcSize(peerConnection.getSyncBlockInProcess().size());
      peerInfo.setConnectTime(peerConnection.getStartTime());
      peerInfo.setDisconnectTimes(peerConnection.getNodeStatistics().getDisconnectTimes());
      //peerInfo.setHeadBlockTimeWeBothHave(peerConnection.getHeadBlockTimeWeBothHave());
      peerInfo.setHost(peerConnection.getNode().getHost());
      peerInfo.setInFlow(peerConnection.getNodeStatistics().tcpFlow.getTotalCount());
      peerInfo.setLastBlockUpdateTime(peerConnection.getBlockBothHaveUpdateTime());
      peerInfo.setLastSyncBlock(peerConnection.getLastSyncBlockId() == null ? ""
          : peerConnection.getLastSyncBlockId().getString());
      ReasonCode reasonCode = peerConnection.getNodeStatistics()
          .getTronLastLocalDisconnectReason();
      peerInfo.setLocalDisconnectReason(reasonCode == null ? "" : reasonCode.toString());
      reasonCode = peerConnection.getNodeStatistics().getTronLastRemoteDisconnectReason();
      peerInfo.setRemoteDisconnectReason(reasonCode == null ? "" : reasonCode.toString());
      peerInfo.setNeedSyncFromPeer(peerConnection.isNeedSyncFromPeer());
      peerInfo.setNeedSyncFromUs(peerConnection.isNeedSyncFromUs());
      peerInfo.setNodeCount(nodeManager.getTable().getAllNodes().size());
      peerInfo.setNodeId(peerConnection.getNode().getHexId());
      peerInfo.setPort(peerConnection.getNode().getPort());
      peerInfo.setRemainNum(peerConnection.getRemainNum());
      peerInfo.setScore(peerConnection.getNodeStatistics().getReputation());
      peerInfo.setSyncBlockRequestedSize(peerConnection.getSyncBlockRequested().size());
      peerInfo.setSyncFlag(peerConnection.isDisconnect());
      peerInfo.setSyncToFetchSize(peerConnection.getSyncBlockToFetch().size());
      peerInfo.setSyncToFetchSizePeekNum(peerConnection.getSyncBlockToFetch().size() > 0
          ? peerConnection.getSyncBlockToFetch().peek().getNum() : -1);
      peerInfo.setUnFetchSynNum(peerConnection.getRemainNum());
      totalFlow += peerConnection.getNodeStatistics().tcpFlow.getTotalCount();
      peerInfoList.add(peerInfo);
    }
    nodeInfo.setPeerList(peerInfoList);
    nodeInfo.setTotalFlow(totalFlow);
  }

  private void setConfigNodeInfo(NodeInfo nodeInfo) {
    ConfigNodeInfo configNodeInfo = new ConfigNodeInfo();
    configNodeInfo.setCodeVersion(Version.getVersion());
    configNodeInfo.setVersionNum(Version.VERSION_CODE);
    configNodeInfo.setP2pVersion(String.valueOf(parameter.getNodeP2pVersion()));
    configNodeInfo.setListenPort(parameter.getNodeListenPort());
    configNodeInfo.setDiscoverEnable(parameter.isNodeDiscoveryEnable());
    configNodeInfo.setActiveNodeSize(parameter.getActiveNodes().size());
    configNodeInfo.setPassiveNodeSize(parameter.getPassiveNodes().size());
    configNodeInfo.setSendNodeSize(parameter.getSeedNodes().size());
    configNodeInfo.setMaxConnectCount(parameter.getNodeMaxActiveNodes());
    configNodeInfo.setSameIpMaxConnectCount(parameter.getNodeMaxActiveNodesWithSameIp());
    configNodeInfo.setBackupListenPort(parameter.getBackupPort());
    configNodeInfo.setBackupMemberSize(parameter.getBackupMembers().size());
    configNodeInfo.setBackupPriority(parameter.getBackupPriority());
    configNodeInfo.setDbVersion(parameter.getStorage().getDbVersion());
    configNodeInfo.setMinParticipationRate(parameter.getMinParticipationRate());
    configNodeInfo.setSupportConstant(parameter.isSupportConstant());
    configNodeInfo.setMinTimeRatio(parameter.getMinTimeRatio());
    configNodeInfo.setMaxTimeRatio(parameter.getMaxTimeRatio());
    configNodeInfo.setAllowCreationOfContracts(parameter.getAllowCreationOfContracts());
    configNodeInfo.setAllowAdaptiveEnergy(parameter.getAllowAdaptiveEnergy());
    nodeInfo.setConfigNodeInfo(configNodeInfo);
  }

  protected void setBlockInfo(NodeInfo nodeInfo) {
    nodeInfo.setBeginSyncNum(dbManager.getSyncBeginNumber());
    nodeInfo.setBlock(chainBaseManager.getHeadBlockId().getString());
    nodeInfo.setSolidityBlock(chainBaseManager.getSolidBlockId().getString());
  }

  protected void setCheatWitnessInfo(NodeInfo nodeInfo) {
    for (Entry<String, CheatWitnessInfo> entry : witnessProductBlockService.queryCheatWitnessInfo()
        .entrySet()) {
      nodeInfo.getCheatWitnessInfoMap().put(entry.getKey(), entry.getValue().toString());
    }
  }

}
