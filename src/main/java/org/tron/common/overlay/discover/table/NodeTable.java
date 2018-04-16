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

import org.slf4j.LoggerFactory;
import org.tron.common.overlay.discover.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by kest on 5/25/15.
 */
public class NodeTable {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger("NodeTable");

    private final Node node;  // our node
    private transient NodeBucket[] buckets;
    private transient List<NodeEntry> nodes;
    private Map<Node, Node> evictedCandidates = new HashMap<>();
    private Map<Node, Date> expectedPongs = new HashMap<>();

    public NodeTable(Node n) {
        this.node = n;
        initialize();
    }

    public Node getNode() {
        return node;
    }

    public final void initialize() {
        nodes = new ArrayList<>();
        buckets = new NodeBucket[KademliaOptions.BINS];
        IntStream.range(0, KademliaOptions.BINS)
                .forEachOrdered(i -> buckets[i] = new NodeBucket(i));
    }

    public synchronized Node addNode(Node n) {
        NodeEntry e = new NodeEntry(node.getId(), n);
        NodeEntry lastSeen = buckets[getBucketId(e)].addNode(e);
        if (lastSeen != null) {
            return lastSeen.getNode();
        }
        if (!nodes.contains(e)) {
            nodes.add(e);
        }
        return null;
    }

    public synchronized void dropNode(Node n) {
        NodeEntry e = new NodeEntry(node.getId(), n);
        buckets[getBucketId(e)].dropNode(e);
        nodes.remove(e);
    }

    public synchronized boolean contains(Node n) {
        NodeEntry e = new NodeEntry(node.getId(), n);
        return Arrays.stream(buckets)
                .anyMatch(b -> b.getNodes().contains(e));
    }

    public synchronized void touchNode(Node n) {
        NodeEntry e = new NodeEntry(node.getId(), n);
        Arrays.stream(buckets)
                .filter(b -> b.getNodes().contains(e))
                .findFirst()
                .ifPresent(b -> b.getNodes().get(b.getNodes().indexOf(e)).touch());
    }

    public int getBucketsCount() {
        return (int) Arrays.stream(buckets)
                .filter(b -> b.getNodesCount() > 0)
                .count();
    }

    public synchronized NodeBucket[] getBuckets() {
        return buckets;
    }

    public int getBucketId(NodeEntry e) {
        int id = e.getDistance() - 1;
        return id < 0 ? 0 : id;
    }

    public synchronized int getNodesCount() {
        return nodes.size();
    }

    public synchronized List<NodeEntry> getAllNodes() {
        return Arrays.stream(buckets)
                .flatMap(b -> b.getNodes().stream())
                .filter(e -> !e.getNode().equals(node))
                .collect(Collectors.toList());
    }

    public synchronized List<Node> getClosestNodes(byte[] targetId) {
        List<NodeEntry> closestEntries = getAllNodes();
        closestEntries.sort(new DistanceComparator(targetId));
        if (closestEntries.size() > KademliaOptions.BUCKET_SIZE) {
            closestEntries = closestEntries.subList(0, KademliaOptions.BUCKET_SIZE);
        }
        return closestEntries.stream()
                .filter(e -> !e.getNode().isDiscoveryNode())
                .map(NodeEntry::getNode)
                .collect(Collectors.toList());
    }
}
