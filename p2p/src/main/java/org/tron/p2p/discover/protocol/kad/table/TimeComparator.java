package org.tron.p2p.discover.protocol.kad.table;

import java.util.Comparator;

public class TimeComparator implements Comparator<NodeEntry> {
  @Override
  public int compare(NodeEntry e1, NodeEntry e2) {
    long t1 = e1.getModified();
    long t2 = e2.getModified();

    if (t1 < t2) {
      return 1;
    } else if (t1 > t2) {
      return -1;
    } else {
      return 0;
    }
  }
}
