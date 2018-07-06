package org.tron.core.db2.snapshot;

import org.tron.core.db2.common.DB;

public abstract class AbstractSnapshot<K, V> implements Snapshot {
  protected DB<K, V> db;
  protected Snapshot previous;
}
