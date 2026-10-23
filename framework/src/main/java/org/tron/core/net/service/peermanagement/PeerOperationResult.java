package org.tron.core.net.service.peermanagement;

import lombok.Getter;

@Getter
public class PeerOperationResult {

  private final boolean success;
  private final boolean changed;
  private final int disconnectedCount;
  // Empty on success; describes why the operation failed otherwise.
  private final String errorMessage;

  public PeerOperationResult(boolean success, boolean changed, int disconnectedCount,
      String errorMessage) {
    this.success = success;
    this.changed = changed;
    this.disconnectedCount = disconnectedCount;
    this.errorMessage = errorMessage;
  }
}
