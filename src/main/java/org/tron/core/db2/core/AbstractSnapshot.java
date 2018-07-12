package org.tron.core.db2.snapshot;

import lombok.Getter;
import lombok.Setter;
import org.tron.core.db2.common.DB;

public abstract class AbstractSnapshot<K, V> implements Snapshot {
  @Getter
  protected DB<K, V> db;
  @Getter
  @Setter
  protected Snapshot previous;

  @Override
  public Snapshot advance() {
    return new SnapshotImpl(this);
  }

  @Override
  public Snapshot retreat() {
    return previous;
  }
}
