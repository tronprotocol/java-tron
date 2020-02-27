package org.tron.core.metrics.node;

public class NodeInfo {
  private String ip;
  private int nodeType;
  private int status;
  private String version;
  private int backupStatus;

  public int getBackupStatus() {
    return backupStatus;
  }

  public void setBackupStatus(int backupStatus) {
    this.backupStatus = backupStatus;
  }

  public String getIp() {
    return this.ip;
  }

  public NodeInfo setIp(String ip) {
    this.ip = ip;
    return this;
  }

  public int getNodeType() {
    return this.nodeType;
  }


  public NodeInfo setNodeType(int nodeType) {
    this.nodeType = nodeType;
    return this;
  }

  public int getStatus() {
    return this.status;
  }

  public NodeInfo setStatus(int status) {
    this.status = status;
    return this;
  }

  public String getVersion() {
    return this.version;
  }

  public NodeInfo setVersion(String version) {
    this.version = version;
    return this;
  }

}
