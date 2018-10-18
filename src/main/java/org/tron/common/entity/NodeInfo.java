package org.tron.common.entity;

import java.util.List;

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
  private List<PeerInfo> peerList;

  /*node config information*/
  private ConfigNodeInfo configNodeInfo;
  /*machine information*/
  private MachineInfo machineInfo;

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
    private List<MemoryDescInfo> memoryDescInfoList;
    private List<DeadLockThreadInfo> deadLockThreadInfoList;

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
      return memoryDescInfoList;
    }

    public MachineInfo setMemoryDescInfoList(
        List<MemoryDescInfo> memoryDescInfoList) {
      this.memoryDescInfoList = memoryDescInfoList;
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
      return deadLockThreadInfoList;
    }

    public MachineInfo setDeadLockThreadInfoList(
        List<DeadLockThreadInfo> deadLockThreadInfoList) {
      this.deadLockThreadInfoList = deadLockThreadInfoList;
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
    return peerList;
  }

  public NodeInfo setPeerList(List<PeerInfo> peerList) {
    this.peerList = peerList;
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
}
