package org.tron.core.db2.core;

import java.util.Iterator;
import java.util.Map.Entry;

public class SnapshotNode extends AbstractSnapshot<byte[], byte[]> {

  public SnapshotNode(Snapshot snapshot) {
    solidity = snapshot;
  }

  public void setSolidity(Snapshot snapshot) {
    solidity = snapshot;
  }

  @Override
  public byte[] get(byte[] key) {
    return new byte[0];
  }

  @Override
  public void put(byte[] key, byte[] value) {

  }

  @Override
  public void remove(byte[] key) {

  }

  @Override
  public void merge(Snapshot from) {

  }

  @Override
  public Snapshot retreat() {
    return null;
  }

  @Override
  public Snapshot getRoot() {
    return null;
  }

  @Override
  public void close() {

  }

  @Override
  public void reset() {

  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return null;
  }

  @Override
  public void resetSolidity() {
    solidity = solidity.getRoot();
  }

  @Override
  public void updateSolidity() {
    solidity = solidity.getNext();
  }
}
