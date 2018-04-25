package org.tron.core.db.api.index;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.persistence.Persistence;
import org.tron.core.db.api.index.Index.Iface;

public abstract class AbstractIndex<T> extends ConcurrentIndexedCollection<T> implements Iface {

  public AbstractIndex() {
    super();
  }

  public AbstractIndex(Persistence<T, ? extends Comparable> persistence) {
    super(persistence);
  }

  public boolean update(T t) {
    return add(t);
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }
}
