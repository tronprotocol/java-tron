package org.tron.common.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tron.common.entity.NodeInfo.MachineInfo.DeadLockThreadInfo;
import org.tron.common.entity.NodeInfo.MachineInfo.MemoryDescInfo;
import org.tron.protos.Protocol;

public class NodeInfo {

  /*block information*/
  private long beginSyncNum;
  private String block;
  private String solidityBlock;

  /*connect information*/
  private int currentConnectCount;
  private int activeConnectCount;
  private int passiveConnectCount;
  private long totalFlow;
  private List<PeerInfo> peerList = new ArrayList<>();

  /*node config information*/
  private ConfigNodeInfo configNodeInfo;
  /*machine information*/
  private MachineInfo machineInfo;

  private Map<String, String> cheatWitnessInfoMap = new HashMap<>();

  public static class MachineInfo {

    /*machine information*/
    private int threadCount;
    private int deadLockThreadCount;
    private int cpuCount;
    private long totalMemory;
    private long freeMemory;
    private double cpuRate;
    private String javaVersion;
    private String osName;
    private long jvmTotalMemoery;
    private long jvmFreeMemory;
    private double processCpuRate;
    private List<MemoryDescInfo> memoryDescInfoList = new ArrayList<>();
    private List<DeadLockThreadInfo> deadLockThreadInfoList = new ArrayList<>();

    public static class MemoryDescInfo {

      private String name;
      private long initSize;
      private long useSize;
      private long maxSize;
      private double useRate;

      public String getName() {
        return name;
      }

      public MemoryDescInfo setName(String name) {
        this.name = name;
        return this;
      }

      public long getInitSize() {
        return initSize;
      }

      public MemoryDescInfo setInitSize(long initSize) {
        this.initSize = initSize;
        return this;
      }

      public long getUseSize() {
        return useSize;
      }

      public MemoryDescInfo setUseSize(long useSize) {
        this.useSize = useSize;
        return this;
      }

      public long getMaxSize() {
        return maxSize;
      }

      public MemoryDescInfo setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        return this;
      }

      public double getUseRate() {
        return useRate;
      }

      public MemoryDescInfo setUseRate(double useRate) {
        this.useRate = useRate;
        return this;
      }
    }

    public static class DeadLockThreadInfo {

      private String name;
      private String lockName;
      private String lockOwner;
      private String state;
      private long blockTime;
      private long waitTime;
      private String stackTrace;

      public String getName() {
        return name;
      }

      public DeadLockThreadInfo setName(String name) {
        this.name = name;
        return this;
      }

      public String getLockName() {
        return lockName;
      }

      public DeadLockThreadInfo setLockName(String lockName) {
        this.lockName = lockName;
        return this;
      }

      public String getLockOwner() {
        return lockOwner;
      }

      public DeadLockThreadInfo setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
        return this;
      }

      public String getState() {
        return state;
      }

      public DeadLockThreadInfo setState(String state) {
        this.state = state;
        return this;
      }

      public long getBlockTime() {
        return blockTime;
      }

      public DeadLockThreadInfo setBlockTime(long blockTime) {
        this.blockTime = blockTime;
        return this;
      }

      public long getWaitTime() {
        return waitTime;
      }

      public DeadLockThreadInfo setWaitTime(long waitTime) {
        this.waitTime = waitTime;
        return this;
      }

      public String getStackTrace() {
        return stackTrace;
      }

      public DeadLockThreadInfo setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
      }
    }

    public int getThreadCount() {
      return threadCount;
    }

    public MachineInfo setThreadCount(int threadCount) {
      this.threadCount = threadCount;
      return this;
    }

    public int getCpuCount() {
      return cpuCount;
    }

    public MachineInfo setCpuCount(int cpuCount) {
      this.cpuCount = cpuCount;
      return this;
    }

    public long getTotalMemory() {
      return totalMemory;
    }

    public MachineInfo setTotalMemory(long totalMemory) {
      this.totalMemory = totalMemory;
      return this;
    }

    public long getFreeMemory() {
      return freeMemory;
    }

    public MachineInfo setFreeMemory(long freeMemory) {
      this.freeMemory = freeMemory;
      return this;
    }

    public double getCpuRate() {
      return cpuRate;
    }

    public MachineInfo setCpuRate(double cpuRate) {
      this.cpuRate = cpuRate;
      return this;
    }

    public String getJavaVersion() {
      return javaVersion;
    }

    public MachineInfo setJavaVersion(String javaVersion) {
      this.javaVersion = javaVersion;
      return this;
    }

    public String getOsName() {
      return osName;
    }

    public MachineInfo setOsName(String osName) {
      this.osName = osName;
      return this;
    }

    public long getJvmTotalMemoery() {
      return jvmTotalMemoery;
    }

    public MachineInfo setJvmTotalMemoery(long jvmTotalMemoery) {
      this.jvmTotalMemoery = jvmTotalMemoery;
      return this;
    }

    public long getJvmFreeMemory() {
      return jvmFreeMemory;
    }

    public MachineInfo setJvmFreeMemory(long jvmFreeMemory) {
      this.jvmFreeMemory = jvmFreeMemory;
      return this;
    }

    public double getProcessCpuRate() {
      return processCpuRate;
    }

    public MachineInfo setProcessCpuRate(double processCpuRate) {
      this.processCpuRate = processCpuRate;
      return this;
    }

    public List<MemoryDescInfo> getMemoryDescInfoList() {
      return new ArrayList<>(memoryDescInfoList);
    }

    public MachineInfo setMemoryDescInfoList(
        List<MemoryDescInfo> memoryDescInfoList) {
      this.memoryDescInfoList = new ArrayList<>(memoryDescInfoList);
      return this;
    }

    public int getDeadLockThreadCount() {
      return deadLockThreadCount;
    }

    public MachineInfo setDeadLockThreadCount(int deadLockThreadCount) {
      this.deadLockThreadCount = deadLockThreadCount;
      return this;
    }

    public List<DeadLockThreadInfo> getDeadLockThreadInfoList() {
      return new ArrayList<>(deadLockThreadInfoList);
    }

    public MachineInfo setDeadLockThreadInfoList(List<DeadLockThreadInfo> deadLockThreadInfoList) {
      this.deadLockThreadInfoList = new ArrayList<>(deadLockThreadInfoList);
      return this;
    }
  }

  public static class ConfigNodeInfo {

    /*node information*/
    private String codeVersion;
    private String p2pVersion;
    private int listenPort;
    private boolean discoverEnable;
    private int activeNodeSize;
    private int passiveNodeSize;
    private int sendNodeSize;
    private int maxConnectCount;
    private int sameIpMaxConnectCount;
    private int backupListenPort;
    private int backupMemberSize;
    private int backupPriority;
    private int dbVersion;
    private int minParticipationRate;
    private boolean supportConstant;
    private double minTimeRatio;
    private double maxTimeRatio;
    private long allowCreationOfContracts;
    private long allowAdaptiveEnergy;

    public String getCodeVersion() {
      return codeVersion;
    }

    public ConfigNodeInfo setCodeVersion(String codeVersion) {
      this.codeVersion = codeVersion;
      return this;
    }

    public String getP2pVersion() {
      return p2pVersion;
    }

    public ConfigNodeInfo setP2pVersion(String p2pVersion) {
      this.p2pVersion = p2pVersion;
      return this;
    }

    public int getListenPort() {
      return listenPort;
    }

    public ConfigNodeInfo setListenPort(int listenPort) {
      this.listenPort = listenPort;
      return this;
    }

    public boolean isDiscoverEnable() {
      return discoverEnable;
    }

    public ConfigNodeInfo setDiscoverEnable(boolean discoverEnable) {
      this.discoverEnable = discoverEnable;
      return this;
    }

    public int getActiveNodeSize() {
      return activeNodeSize;
    }

    public ConfigNodeInfo setActiveNodeSize(int activeNodeSize) {
      this.activeNodeSize = activeNodeSize;
      return this;
    }

    public int getPassiveNodeSize() {
      return passiveNodeSize;
    }

    public ConfigNodeInfo setPassiveNodeSize(int passiveNodeSize) {
      this.passiveNodeSize = passiveNodeSize;
      return this;
    }

    public int getSendNodeSize() {
      return sendNodeSize;
    }

    public ConfigNodeInfo setSendNodeSize(int sendNodeSize) {
      this.sendNodeSize = sendNodeSize;
      return this;
    }

    public int getMaxConnectCount() {
      return maxConnectCount;
    }

    public ConfigNodeInfo setMaxConnectCount(int maxConnectCount) {
      this.maxConnectCount = maxConnectCount;
      return this;
    }

    public int getSameIpMaxConnectCount() {
      return sameIpMaxConnectCount;
    }

    public ConfigNodeInfo setSameIpMaxConnectCount(int sameIpMaxConnectCount) {
      this.sameIpMaxConnectCount = sameIpMaxConnectCount;
      return this;
    }

    public int getBackupListenPort() {
      return backupListenPort;
    }

    public ConfigNodeInfo setBackupListenPort(int backupListenPort) {
      this.backupListenPort = backupListenPort;
      return this;
    }

    public int getBackupMemberSize() {
      return backupMemberSize;
    }

    public ConfigNodeInfo setBackupMemberSize(int backupMemberSize) {
      this.backupMemberSize = backupMemberSize;
      return this;
    }

    public int getBackupPriority() {
      return backupPriority;
    }

    public ConfigNodeInfo setBackupPriority(int backupPriority) {
      this.backupPriority = backupPriority;
      return this;
    }

    public int getDbVersion() {
      return dbVersion;
    }

    public ConfigNodeInfo setDbVersion(int dbVersion) {
      this.dbVersion = dbVersion;
      return this;
    }

    public int getMinParticipationRate() {
      return minParticipationRate;
    }

    public ConfigNodeInfo setMinParticipationRate(int minParticipationRate) {
      this.minParticipationRate = minParticipationRate;
      return this;
    }

    public boolean isSupportConstant() {
      return supportConstant;
    }

    public ConfigNodeInfo setSupportConstant(boolean supportConstant) {
      this.supportConstant = supportConstant;
      return this;
    }

    public double getMinTimeRatio() {
      return minTimeRatio;
    }

    public ConfigNodeInfo setMinTimeRatio(double minTimeRatio) {
      this.minTimeRatio = minTimeRatio;
      return this;
    }

    public double getMaxTimeRatio() {
      return maxTimeRatio;
    }

    public ConfigNodeInfo setMaxTimeRatio(double maxTimeRatio) {
      this.maxTimeRatio = maxTimeRatio;
      return this;
    }

    public long getAllowCreationOfContracts() {
      return allowCreationOfContracts;
    }

    public ConfigNodeInfo setAllowCreationOfContracts(long allowCreationOfContracts) {
      this.allowCreationOfContracts = allowCreationOfContracts;
      return this;
    }

    public long getAllowAdaptiveEnergy() {
      return allowAdaptiveEnergy;
    }

    public ConfigNodeInfo setAllowAdaptiveEnergy(long allowAdaptiveEnergy) {
      this.allowAdaptiveEnergy = allowAdaptiveEnergy;
      return this;
    }

  }

  public long getBeginSyncNum() {
    return beginSyncNum;
  }

  public NodeInfo setBeginSyncNum(long beginSyncNum) {
    this.beginSyncNum = beginSyncNum;
    return this;
  }

  public String getBlock() {
    return block;
  }

  public NodeInfo setBlock(String block) {
    this.block = block;
    return this;
  }

  public String getSolidityBlock() {
    return solidityBlock;
  }

  public NodeInfo setSolidityBlock(String solidityBlock) {
    this.solidityBlock = solidityBlock;
    return this;
  }

  public int getCurrentConnectCount() {
    return currentConnectCount;
  }

  public NodeInfo setCurrentConnectCount(int currentConnectCount) {
    this.currentConnectCount = currentConnectCount;
    return this;
  }

  public int getActiveConnectCount() {
    return activeConnectCount;
  }

  public NodeInfo setActiveConnectCount(int activeConnectCount) {
    this.activeConnectCount = activeConnectCount;
    return this;
  }

  public int getPassiveConnectCount() {
    return passiveConnectCount;
  }

  public NodeInfo setPassiveConnectCount(int passiveConnectCount) {
    this.passiveConnectCount = passiveConnectCount;
    return this;
  }

  public long getTotalFlow() {
    return totalFlow;
  }

  public NodeInfo setTotalFlow(long totalFlow) {
    this.totalFlow = totalFlow;
    return this;
  }

  public List<PeerInfo> getPeerList() {
    return new ArrayList<>(peerList);
  }

  public NodeInfo setPeerList(List<PeerInfo> peerList) {
    this.peerList = new ArrayList<>(peerList);
    return this;
  }

  public ConfigNodeInfo getConfigNodeInfo() {
    return configNodeInfo;
  }

  public NodeInfo setConfigNodeInfo(ConfigNodeInfo configNodeInfo) {
    this.configNodeInfo = configNodeInfo;
    return this;
  }

  public MachineInfo getMachineInfo() {
    return machineInfo;
  }

  public NodeInfo setMachineInfo(MachineInfo machineInfo) {
    this.machineInfo = machineInfo;
    return this;
  }

  public Map<String, String> getCheatWitnessInfoMap() {
    return cheatWitnessInfoMap;
  }

  public NodeInfo setCheatWitnessInfoMap(
      Map<String, String> cheatWitnessInfoMap) {
    this.cheatWitnessInfoMap = cheatWitnessInfoMap;
    return this;
  }

  public Protocol.NodeInfo transferToProtoEntity() {
    Protocol.NodeInfo.Builder builder = Protocol.NodeInfo.newBuilder();
    builder.setBeginSyncNum(getBeginSyncNum());
    builder.setBlock(getBlock());
    builder.setSolidityBlock(getSolidityBlock());
    builder.setCurrentConnectCount(getCurrentConnectCount());
    builder.setActiveConnectCount(getActiveConnectCount());
    builder.setPassiveConnectCount(getPassiveConnectCount());
    builder.setTotalFlow(getTotalFlow());
    builder.putAllCheatWitnessInfoMap(getCheatWitnessInfoMap());
    for (PeerInfo peerInfo : getPeerList()) {
      Protocol.NodeInfo.PeerInfo.Builder peerInfoBuilder = Protocol.NodeInfo.PeerInfo.newBuilder();
      peerInfoBuilder.setLastSyncBlock(peerInfo.getLastSyncBlock());
      peerInfoBuilder.setRemainNum(peerInfo.getRemainNum());
      peerInfoBuilder.setLastBlockUpdateTime(peerInfo.getLastBlockUpdateTime());
      peerInfoBuilder.setSyncFlag(peerInfo.isSyncFlag());
      peerInfoBuilder.setHeadBlockTimeWeBothHave(peerInfo.getHeadBlockTimeWeBothHave());
      peerInfoBuilder.setNeedSyncFromPeer(peerInfo.isSyncFlag());
      peerInfoBuilder.setNeedSyncFromUs(peerInfo.isNeedSyncFromUs());
      peerInfoBuilder.setHost(peerInfo.getHost());
      peerInfoBuilder.setPort(peerInfo.getPort());
      peerInfoBuilder.setNodeId(peerInfo.getNodeId());
      peerInfoBuilder.setConnectTime(peerInfo.getConnectTime());
      peerInfoBuilder.setAvgLatency(peerInfo.getAvgLatency());
      peerInfoBuilder.setSyncToFetchSize(peerInfo.getSyncToFetchSize());
      peerInfoBuilder.setSyncToFetchSizePeekNum(peerInfo.getSyncToFetchSizePeekNum());
      peerInfoBuilder.setSyncBlockRequestedSize(peerInfo.getSyncBlockRequestedSize());
      peerInfoBuilder.setUnFetchSynNum(peerInfo.getUnFetchSynNum());
      peerInfoBuilder.setBlockInPorcSize(peerInfo.getBlockInPorcSize());
      peerInfoBuilder.setHeadBlockWeBothHave(peerInfo.getHeadBlockWeBothHave());
      peerInfoBuilder.setIsActive(peerInfo.isActive());
      peerInfoBuilder.setScore(peerInfo.getScore());
      peerInfoBuilder.setNodeCount(peerInfo.getNodeCount());
      peerInfoBuilder.setInFlow(peerInfo.getInFlow());
      peerInfoBuilder.setDisconnectTimes(peerInfo.getDisconnectTimes());
      peerInfoBuilder.setLocalDisconnectReason(peerInfo.getLocalDisconnectReason());
      peerInfoBuilder.setRemoteDisconnectReason(peerInfo.getRemoteDisconnectReason());
      builder.addPeerInfoList(peerInfoBuilder.build());
    }
    ConfigNodeInfo configNodeInfo = getConfigNodeInfo();
    if (configNodeInfo != null) {
      Protocol.NodeInfo.ConfigNodeInfo.Builder configBuilder = Protocol.NodeInfo.ConfigNodeInfo
          .newBuilder();
      configBuilder.setCodeVersion(configNodeInfo.getCodeVersion());
      configBuilder.setP2PVersion(configNodeInfo.getP2pVersion());
      configBuilder.setListenPort(configNodeInfo.getListenPort());
      configBuilder.setDiscoverEnable(configNodeInfo.isDiscoverEnable());
      configBuilder.setActiveNodeSize(configNodeInfo.getActiveNodeSize());
      configBuilder.setPassiveNodeSize(configNodeInfo.getPassiveNodeSize());
      configBuilder.setSendNodeSize(configNodeInfo.getSendNodeSize());
      configBuilder.setMaxConnectCount(configNodeInfo.getMaxConnectCount());
      configBuilder.setSameIpMaxConnectCount(configNodeInfo.getSameIpMaxConnectCount());
      configBuilder.setBackupListenPort(configNodeInfo.getBackupListenPort());
      configBuilder.setBackupMemberSize(configNodeInfo.getBackupMemberSize());
      configBuilder.setBackupPriority(configNodeInfo.getBackupPriority());
      configBuilder.setDbVersion(configNodeInfo.getDbVersion());
      configBuilder.setMinParticipationRate(configNodeInfo.getMinParticipationRate());
      configBuilder.setSupportConstant(configNodeInfo.isSupportConstant());
      configBuilder.setMinTimeRatio(configNodeInfo.getMinTimeRatio());
      configBuilder.setMaxTimeRatio(configNodeInfo.getMaxTimeRatio());
      configBuilder.setAllowCreationOfContracts(configNodeInfo.getAllowCreationOfContracts());
      configBuilder.setAllowAdaptiveEnergy(configNodeInfo.getAllowAdaptiveEnergy());
      builder.setConfigNodeInfo(configBuilder.build());
    }
    MachineInfo machineInfo = getMachineInfo();
    if (machineInfo != null) {
      Protocol.NodeInfo.MachineInfo.Builder machineBuilder = Protocol.NodeInfo.MachineInfo
          .newBuilder();
      machineBuilder.setThreadCount(machineInfo.getThreadCount());
      machineBuilder.setDeadLockThreadCount(machineInfo.getDeadLockThreadCount());
      machineBuilder.setCpuCount(machineInfo.getCpuCount());
      machineBuilder.setTotalMemory(machineInfo.getTotalMemory());
      machineBuilder.setFreeMemory(machineInfo.getFreeMemory());
      machineBuilder.setCpuRate(machineInfo.getCpuRate());
      machineBuilder.setJavaVersion(machineInfo.getJavaVersion());
      machineBuilder.setOsName(machineInfo.getOsName());
      machineBuilder.setJvmTotalMemoery(machineInfo.getJvmTotalMemoery());
      machineBuilder.setJvmFreeMemory(machineInfo.getJvmFreeMemory());
      machineBuilder.setProcessCpuRate(machineInfo.getProcessCpuRate());
      for (MemoryDescInfo memoryDescInfo : machineInfo.getMemoryDescInfoList()) {
        Protocol.NodeInfo.MachineInfo.MemoryDescInfo.Builder descBuilder = Protocol.NodeInfo.MachineInfo.MemoryDescInfo
            .newBuilder();
        descBuilder.setName(memoryDescInfo.getName());
        descBuilder.setInitSize(memoryDescInfo.getInitSize());
        descBuilder.setUseSize(memoryDescInfo.getUseSize());
        descBuilder.setMaxSize(memoryDescInfo.getMaxSize());
        descBuilder.setUseRate(memoryDescInfo.getUseRate());
        machineBuilder.addMemoryDescInfoList(descBuilder.build());
      }
      for (DeadLockThreadInfo deadLockThreadInfo : machineInfo.getDeadLockThreadInfoList()) {
        Protocol.NodeInfo.MachineInfo.DeadLockThreadInfo.Builder deadBuilder = Protocol.NodeInfo.MachineInfo.DeadLockThreadInfo
            .newBuilder();
        deadBuilder.setName(deadLockThreadInfo.getName());
        deadBuilder.setLockName(deadLockThreadInfo.getLockName());
        deadBuilder.setLockOwner(deadLockThreadInfo.getLockOwner());
        deadBuilder.setState(deadLockThreadInfo.getState());
        deadBuilder.setBlockTime(deadLockThreadInfo.getBlockTime());
        deadBuilder.setWaitTime(deadLockThreadInfo.getWaitTime());
        deadBuilder.setStackTrace(deadLockThreadInfo.getStackTrace());
        machineBuilder.addDeadLockThreadInfoList(deadBuilder.build());
      }
      builder.setMachineInfo(machineBuilder.build());
    }

    return builder.build();
  }
}
