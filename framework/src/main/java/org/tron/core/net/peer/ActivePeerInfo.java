package org.tron.core.net.peer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Immutable peer statistics shared by peer logging and Admin RPC responses.
 * Adding a getter changes the Admin response schema and must not expose sensitive state.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ActivePeerInfo {

  private final String remoteAddress;
  private final long connectSeconds;
  private final long averageLatencyMillis;
  private final long lastKnownBlockNum;
  private final boolean needSyncFromPeer;
  private final boolean needSyncFromUs;
  private final int syncToFetchSize;
  private final long syncToFetchSizePeekNum;
  private final int syncBlockRequestedSize;
  private final long remainNum;
  private final long syncChainRequestedMillis;
  private final long inactiveSeconds;
  private final int syncBlockInProcessSize;
}
