package org.tron.common.entity;

import java.util.List;

public class NodeInfo {

  /*block information*/
  private long blockNum;
  private long solidityNum;
  private long beginSyncNum;
  private String blockHash;
  private String solidityHash;

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

  /*connect information*/
  private int currentConnectCount;
  private int activeConnectCount;
  private int passiveConnectCount;
  private long totalFlow;
  private List<PeerInfo> peerList;

  /*machine information*/
  private int threadCount;
  private int cpuCount;
  private long totalMemory;
  private long freeMemory;
  private double cpuRate;
  private String javaVersion;
  private String osName;
  private long jvmTotalMemoery;
  private long jvmFreeMemory;
  private double processCpuRate;

  public long getBlockNum() {
    return blockNum;
  }

  public NodeInfo setBlockNum(long blockNum) {
    this.blockNum = blockNum;
    return this;
  }

  public long getSolidityNum() {
    return solidityNum;
  }

  public NodeInfo setSolidityNum(long solidityNum) {
    this.solidityNum = solidityNum;
    return this;
  }

  public long getBeginSyncNum() {
    return beginSyncNum;
  }

  public NodeInfo setBeginSyncNum(long beginSyncNum) {
    this.beginSyncNum = beginSyncNum;
    return this;
  }

  public String getSolidityHash() {
    return solidityHash;
  }

  public NodeInfo setSolidityHash(String solidityHash) {
    this.solidityHash = solidityHash;
    return this;
  }

  public String getBlockHash() {
    return blockHash;
  }

  public NodeInfo setBlockHash(String blockHash) {
    this.blockHash = blockHash;
    return this;
  }

  public String getCodeVersion() {
    return codeVersion;
  }

  public NodeInfo setCodeVersion(String codeVersion) {
    this.codeVersion = codeVersion;
    return this;
  }

  public String getP2pVersion() {
    return p2pVersion;
  }

  public NodeInfo setP2pVersion(String p2pVersion) {
    this.p2pVersion = p2pVersion;
    return this;
  }

  public int getListenPort() {
    return listenPort;
  }

  public NodeInfo setListenPort(int listenPort) {
    this.listenPort = listenPort;
    return this;
  }

  public boolean isDiscoverEnable() {
    return discoverEnable;
  }

  public NodeInfo setDiscoverEnable(boolean discoverEnable) {
    this.discoverEnable = discoverEnable;
    return this;
  }

  public int getActiveNodeSize() {
    return activeNodeSize;
  }

  public NodeInfo setActiveNodeSize(int activeNodeSize) {
    this.activeNodeSize = activeNodeSize;
    return this;
  }

  public int getPassiveNodeSize() {
    return passiveNodeSize;
  }

  public NodeInfo setPassiveNodeSize(int passiveNodeSize) {
    this.passiveNodeSize = passiveNodeSize;
    return this;
  }

  public int getSendNodeSize() {
    return sendNodeSize;
  }

  public NodeInfo setSendNodeSize(int sendNodeSize) {
    this.sendNodeSize = sendNodeSize;
    return this;
  }

  public int getMaxConnectCount() {
    return maxConnectCount;
  }

  public NodeInfo setMaxConnectCount(int maxConnectCount) {
    this.maxConnectCount = maxConnectCount;
    return this;
  }

  public int getSameIpMaxConnectCount() {
    return sameIpMaxConnectCount;
  }

  public NodeInfo setSameIpMaxConnectCount(int sameIpMaxConnectCount) {
    this.sameIpMaxConnectCount = sameIpMaxConnectCount;
    return this;
  }

  public int getBackupListenPort() {
    return backupListenPort;
  }

  public NodeInfo setBackupListenPort(int backupListenPort) {
    this.backupListenPort = backupListenPort;
    return this;
  }

  public int getBackupMemberSize() {
    return backupMemberSize;
  }

  public NodeInfo setBackupMemberSize(int backupMemberSize) {
    this.backupMemberSize = backupMemberSize;
    return this;
  }

  public int getBackupPriority() {
    return backupPriority;
  }

  public NodeInfo setBackupPriority(int backupPriority) {
    this.backupPriority = backupPriority;
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

  /*machine information*/

  public int getThreadCount() {
    return threadCount;
  }

  public NodeInfo setThreadCount(int threadCount) {
    this.threadCount = threadCount;
    return this;
  }

  public int getCpuCount() {
    return cpuCount;
  }

  public NodeInfo setCpuCount(int cpuCount) {
    this.cpuCount = cpuCount;
    return this;
  }

  public long getTotalMemory() {
    return totalMemory;
  }

  public NodeInfo setTotalMemory(long totalMemory) {
    this.totalMemory = totalMemory;
    return this;
  }

  public long getFreeMemory() {
    return freeMemory;
  }

  public NodeInfo setFreeMemory(long freeMemory) {
    this.freeMemory = freeMemory;
    return this;
  }

  public double getCpuRate() {
    return cpuRate;
  }

  public NodeInfo setCpuRate(double cpuRate) {
    this.cpuRate = cpuRate;
    return this;
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public NodeInfo setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
    return this;
  }

  public String getOsName() {
    return osName;
  }

  public NodeInfo setOsName(String osName) {
    this.osName = osName;
    return this;
  }

  public long getJvmTotalMemoery() {
    return jvmTotalMemoery;
  }

  public NodeInfo setJvmTotalMemoery(long jvmTotalMemoery) {
    this.jvmTotalMemoery = jvmTotalMemoery;
    return this;
  }

  public long getJvmFreeMemory() {
    return jvmFreeMemory;
  }

  public NodeInfo setJvmFreeMemory(long jvmFreeMemory) {
    this.jvmFreeMemory = jvmFreeMemory;
    return this;
  }

  public double getProcessCpuRate() {
    return processCpuRate;
  }

  public NodeInfo setProcessCpuRate(double processCpuRate) {
    this.processCpuRate = processCpuRate;
    return this;
  }
}
