package org.tron.p2p.discover.protocol.kad.table;

import org.tron.p2p.discover.Node;

public class NodeEntry {
  private Node node;
  private String entryId;
  private int distance;
  private long modified;

  public NodeEntry(byte[] ownerId, Node n) {
    this.node = n;
    entryId = n.getHostKey();
    distance = distance(ownerId, n.getId());
    touch();
  }

  public static int distance(byte[] ownerId, byte[] targetId) {
    byte[] h1 = targetId;
    byte[] h2 = ownerId;

    byte[] hash = new byte[Math.min(h1.length, h2.length)];

    for (int i = 0; i < hash.length; i++) {
      hash[i] = (byte) (h1[i] ^ h2[i]);
    }

    int d = KademliaOptions.BINS;

    for (byte b : hash) {
      if (b == 0) {
        d -= 8;
      } else {
        int count = 0;
        for (int i = 7; i >= 0; i--) {
          boolean a = ((b & 0xff) & (1 << i)) == 0;
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
    return this.entryId.hashCode();
  }
}
