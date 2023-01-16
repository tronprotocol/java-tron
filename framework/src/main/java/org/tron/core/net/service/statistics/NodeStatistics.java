package org.tron.core.net.service.statistics;

import lombok.Getter;
import org.tron.protos.Protocol;

public class NodeStatistics {
  @Getter
  private Protocol.ReasonCode remoteDisconnectReason = null;
  @Getter
  private Protocol.ReasonCode localDisconnectReason = null;
  @Getter
  private int disconnectTimes = 0;
  private long start = System.currentTimeMillis();

  public Protocol.ReasonCode getDisconnectReason() {
    if (localDisconnectReason != null) {
      return localDisconnectReason;
    }
    if (remoteDisconnectReason != null) {
      return remoteDisconnectReason;
    }
    return Protocol.ReasonCode.UNKNOWN;
  }

  public void nodeDisconnectedRemote(Protocol.ReasonCode reason) {
    remoteDisconnectReason = reason;
    notifyDisconnect();
  }

  public void nodeDisconnectedLocal(Protocol.ReasonCode reason) {
    localDisconnectReason = reason;
    notifyDisconnect();
  }

  private void notifyDisconnect() {
    disconnectTimes++;
  }

  @Override
  public String toString() {
    return new StringBuilder()
            .append("time:").append(System.currentTimeMillis() - start)
            .append(", disconnectTimes:").append(disconnectTimes)
            .append(", localReason:").append(localDisconnectReason)
            .append(", remoteReason:").append(remoteDisconnectReason).toString();
  }

}
