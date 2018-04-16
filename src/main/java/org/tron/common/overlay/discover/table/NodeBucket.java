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
        }else {
            e.touch();
        }

        return null;
    }

    private NodeEntry getLastSeen() {
        List<NodeEntry> sorted = nodes;
        sorted.sort(new TimeComparator());
        return sorted.get(0);
    }

    public synchronized void dropNode(NodeEntry entry) {
        nodes.stream().
                filter(e -> e.getId().equals(entry.getId()))
                .findFirst()
                .ifPresent(e -> nodes.remove(e));
    }

    public int getNodesCount() {
        return nodes.size();
    }

    public List<NodeEntry> getNodes() {
//        List<NodeEntry> nodes = new ArrayList<>();
//        for (NodeEntry e : this.nodes) {
//            nodes.add(e);
//        }
        return nodes;
    }
}
