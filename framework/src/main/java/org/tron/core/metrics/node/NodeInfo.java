package org.tron.core.metrics.node;

public class NodeInfo {

  private String ip;
  private int nodeType;
  private String version;
  private int backupStatus;


  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public int getNodeType() {
    return nodeType;
  }

  public void setNodeType(int nodeType) {
    this.nodeType = nodeType;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public int getBackupStatus() {
    return backupStatus;
  }

  public void setBackupStatus(int backupStatus) {
    this.backupStatus = backupStatus;
  }
}
