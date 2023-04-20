package org.tron.core.state.store;


public interface StateStore {

  default void throwIfError() {
    throw new UnsupportedOperationException();
  }
}
