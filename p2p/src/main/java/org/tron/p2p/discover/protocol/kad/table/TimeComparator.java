package org.tron.p2p.discover.protocol.kad.table;

import java.util.Comparator;

public class TimeComparator implements Comparator<NodeEntry> {
  @Override
  public int compare(NodeEntry e1, NodeEntry e2) {
    return Long.compare(e1.getModified(), e2.getModified());
  }
}
