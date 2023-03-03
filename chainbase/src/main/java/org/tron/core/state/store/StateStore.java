package org.tron.core.state.store;

import org.tron.core.state.WorldStateQueryInstance;

public interface StateStore {

  void init(WorldStateQueryInstance worldStateQueryInstance);

  boolean isInit();

  default void throwIfError() {
    throw new UnsupportedOperationException();
  }

  default void throwIfNotInit() {
    if (!isInit()) {
      throw new IllegalStateException();
    }
  }
}
