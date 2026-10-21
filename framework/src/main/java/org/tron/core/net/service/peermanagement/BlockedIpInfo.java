package org.tron.core.net.service.peermanagement;

import lombok.Getter;

/**
 * Immutable manual blocklist record shared by persistence and Admin RPC responses.
 */
@Getter
public final class BlockedIpInfo {

  private final String ip;
  private final long blockedAtMillis;

  public BlockedIpInfo(String ip, long blockedAtMillis) {
    this.ip = ip;
    this.blockedAtMillis = blockedAtMillis;
  }
}
