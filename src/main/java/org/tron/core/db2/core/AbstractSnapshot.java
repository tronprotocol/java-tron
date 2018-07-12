package org.tron.core.db2.core;

import lombok.Getter;
import lombok.Setter;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.DB;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
