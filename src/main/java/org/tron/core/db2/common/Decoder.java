package org.tron.core.db2.common;

public interface Decoder<T> {
  T decode(byte[] bytes);
}
