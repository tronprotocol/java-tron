package org.tron.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
