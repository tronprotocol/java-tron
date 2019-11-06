/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.overlay.discover.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kest on 5/25/15.
 */
public class NodeBucket {

  private final int depth;
  private List<NodeEntry> nodes = new ArrayList<>();

  NodeBucket(int depth) {
    this.depth = depth;
  }

  public int getDepth() {
    return depth;
  }

  public synchronized NodeEntry addNode(NodeEntry e) {
    if (!nodes.contains(e)) {
      if (nodes.size() >= KademliaOptions.BUCKET_SIZE) {
        return getLastSeen();
      } else {
        nodes.add(e);
      }
    }

    return null;
  }

  private NodeEntry getLastSeen() {
    List<NodeEntry> sorted = nodes;
    Collections.sort(sorted, new TimeComparator());
    return sorted.get(0);
  }

  public synchronized void dropNode(NodeEntry entry) {
    for (NodeEntry e : nodes) {
      if (e.getId().equals(entry.getId())) {
        nodes.remove(e);
        break;
      }
    }
  }

  public int getNodesCount() {
    return nodes.size();
  }

  public List<NodeEntry> getNodes() {
    return nodes;
  }
}
