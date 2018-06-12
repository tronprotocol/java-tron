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

import org.tron.common.overlay.discover.node.Node;

/**
 * Created by kest on 5/25/15.
 */
public class NodeEntry {

  private byte[] ownerId;
  Node node;
  private String entryId;
  private int distance;
  private long modified;

  public NodeEntry(Node n) {
    this.node = n;
    this.ownerId = n.getId();
    entryId = n.getHost();
    distance = distance(ownerId, n.getId());
    touch();
  }

  public NodeEntry(byte[] ownerId, Node n) {
    this.node = n;
    this.ownerId = ownerId;
    entryId = n.getHost();
    distance = distance(ownerId, n.getId());
    touch();
  }

  public void touch() {
    modified = System.currentTimeMillis();
  }

  public int getDistance() {
    return distance;
  }

  public String getId() {
    return entryId;
  }

  public Node getNode() {
    return node;
  }

  public long getModified() {
    return modified;
  }

  @Override
  public boolean equals(Object o) {
    boolean ret = false;

    if (o != null && this.getClass() == o.getClass()) {
      NodeEntry e = (NodeEntry) o;
      ret = this.getId().equals(e.getId());
    }

    return ret;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  public static int distance(byte[] ownerId, byte[] targetId) {
//        byte[] h1 = keccak(targetId);
//        byte[] h2 = keccak(ownerId);
    byte[] h1 = targetId;
    byte[] h2 = ownerId;

    byte[] hash = new byte[Math.min(h1.length, h2.length)];

    for (int i = 0; i < hash.length; i++) {
      hash[i] = (byte) (((int) h1[i]) ^ ((int) h2[i]));
    }

    int d = KademliaOptions.BINS;

    for (byte b : hash) {
      if (b == 0) {
        d -= 8;
      } else {
        int count = 0;
        for (int i = 7; i >= 0; i--) {
          boolean a = (b & (1 << i)) == 0;
          if (a) {
            count++;
          } else {
            break;
          }
        }

        d -= count;

        break;
      }
    }
    return d;
  }
}
