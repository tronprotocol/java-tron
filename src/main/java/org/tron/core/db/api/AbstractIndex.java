package org.tron.core.db.api;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.persistence.Persistence;
import org.tron.core.db.api.Index.Iface;

public abstract class AbstractIndex<T> extends ConcurrentIndexedCollection<T> implements Iface {

  public AbstractIndex(Persistence<T, ? extends Comparable> persistence) {
    super(persistence);
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }
}
