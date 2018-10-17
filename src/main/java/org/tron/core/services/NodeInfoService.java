package org.tron.core.services;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.NodeInfo;
import org.tron.common.entity.PeerInfo;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.Time;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.peer.PeerConnection;
import org.tron.program.Version;
import org.tron.protos.Protocol.ReasonCode;

@Component
public class NodeInfoService {

  private MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
  private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
      .getOperatingSystemMXBean();
  private Args args = Args.getInstance();

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private Manager dbManager;

  public NodeInfo getNodeInfo() {
    NodeInfo nodeInfo = new NodeInfo();
    setConnectInfo(nodeInfo);
    setMachineInfo(nodeInfo);
    setNodeInfo(nodeInfo);
    setBlockInfo(nodeInfo);
    return nodeInfo;
  }

  private void setMachineInfo(NodeInfo nodeInfo) {
    nodeInfo.setThreadCount(threadMXBean.getThreadCount());
    nodeInfo.setCpuCount(Runtime.getRuntime().availableProcessors());
    nodeInfo.setTotalMemory(operatingSystemMXBean.getTotalPhysicalMemorySize());
    nodeInfo.setFreeMemory(operatingSystemMXBean.getFreePhysicalMemorySize());
    nodeInfo.setCpuRate(operatingSystemMXBean.getSystemCpuLoad());
    nodeInfo.setJavaVersion(runtimeMXBean.getSystemProperties().get("java.version"));
    nodeInfo.setOsName(operatingSystemMXBean.getName() + " " + operatingSystemMXBean.getVersion());
    nodeInfo.setJvmTotalMemoery(operatingSystemMXBean.getTotalSwapSpaceSize());
    nodeInfo.setJvmFreeMemory(operatingSystemMXBean.getFreeSwapSpaceSize());
    nodeInfo.setProcessCpuRate(operatingSystemMXBean.getProcessCpuLoad());
  }

  private void setConnectInfo(NodeInfo nodeInfo) {
    nodeInfo.setCurrentConnectCount(syncPool.getActivePeers().size());
    nodeInfo.setActiveConnectCount(syncPool.getActivePeersCount().get());
    nodeInfo.setPassiveConnectCount(syncPool.getPassivePeersCount().get());
    long totalFlow = 0;
    List<PeerInfo> peerInfoList = new ArrayList<>();
    for (PeerConnection peerConnection : syncPool.getActivePeers()) {
      PeerInfo peerInfo = new PeerInfo();
      peerInfo.setActive(peerConnection.isActive());
      peerInfo.setAvgLatency(peerConnection.getPeerStats().getAvgLatency());
      peerInfo.setBlockInPorcSize(peerConnection.getBlockInProc().size());
      peerInfo.setConnectTime(Time.getTimeString(peerConnection.getStartTime()));
      peerInfo.setDisconnectTimes(peerConnection.getNodeStatistics().getDisconnectTimes());
      peerInfo.setHeadBlockTimeWeBothHave(peerConnection.getHeadBlockTimeWeBothHave());
      peerInfo.setHost(peerConnection.getNode().getHost());
      peerInfo.setInFlow(peerConnection.getNodeStatistics().tcpFlow.getTotalCount());
      peerInfo.setLastBlockUpdateTime(peerConnection.getLastBlockUpdateTime());
      peerInfo.setLastSyncBlock(peerConnection.getLastSyncBlockId() == null ? ""
          : peerConnection.getLastSyncBlockId().getString());
      ReasonCode reasonCode = peerConnection.getNodeStatistics().getTronLastLocalDisconnectReason();
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
      peerInfo.setSyncFlag(peerConnection.getSyncFlag());
      peerInfo.setSyncToFetchSize(peerConnection.getSyncBlockToFetch().size());
      peerInfo.setSyncToFetchSizePeekNum(
          peerConnection.getSyncBlockToFetch().size() > 0 ? peerConnection.getSyncBlockToFetch()
              .peek().getNum() : -1);
      peerInfo.setUnFetchSynNum(peerConnection.getUnfetchSyncNum());
      totalFlow += peerConnection.getNodeStatistics().tcpFlow.getTotalCount();
      peerInfoList.add(peerInfo);
    }
    nodeInfo.setPeerList(peerInfoList);
    nodeInfo.setTotalFlow(totalFlow);
  }

  private void setNodeInfo(NodeInfo nodeInfo) {
    nodeInfo.setCodeVersion(Version.getVersion());
    nodeInfo.setP2pVersion(String.valueOf(args.getNodeP2pVersion()));
    nodeInfo.setListenPort(args.getNodeListenPort());
    nodeInfo.setDiscoverEnable(args.isNodeDiscoveryEnable());
    nodeInfo.setActiveNodeSize(args.getActiveNodes().size());
    nodeInfo.setPassiveNodeSize(args.getPassiveNodes().size());
    nodeInfo.setSendNodeSize(args.getSeedNodes().size());
    nodeInfo.setMaxConnectCount(args.getNodeMaxActiveNodes());
    nodeInfo.setSameIpMaxConnectCount(args.getNodeMaxActiveNodesWithSameIp());
    nodeInfo.setBackupListenPort(args.getBackupPort());
    nodeInfo.setBackupMemberSize(args.getBackupMembers().size());
    nodeInfo.setBackupPriority(args.getBackupPriority());
  }

  private void setBlockInfo(NodeInfo nodeInfo) {
    nodeInfo.setBlockNum(dbManager.getHeadBlockNum());
    nodeInfo.setSolidityNum(dbManager.getSolidBlockId().getNum());
    nodeInfo.setBeginSyncNum(dbManager.getSyncBeginNumber());
    nodeInfo.setBlockHash(dbManager.getHeadBlockId().getString());
    nodeInfo.setSolidityHash(dbManager.getSolidBlockId().getString());
  }

}
