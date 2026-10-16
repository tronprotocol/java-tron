package org.tron.core.net.service.peermanagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.tron.core.net.peer.ActivePeerInfo;

@Getter
public class ActivePeerListResult {

  private final int allCount;
  private final int activeCount;
  private final int passiveCount;
  private final int validCount;
  private final List<ActivePeerInfo> peers;

  public ActivePeerListResult(int allCount, int activeCount, int passiveCount, int validCount,
      List<ActivePeerInfo> peers) {
    this.allCount = allCount;
    this.activeCount = activeCount;
    this.passiveCount = passiveCount;
    this.validCount = validCount;
    this.peers = Collections.unmodifiableList(new ArrayList<>(peers));
  }
}
