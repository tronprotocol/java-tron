/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.storage;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryKeyValueStorage implements KeyValueStorage {

  private final Map<Bytes, byte[]> hashValueStore;
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  public InMemoryKeyValueStorage() {
    this(new HashMap<>());
  }

  protected InMemoryKeyValueStorage(final Map<Bytes, byte[]> hashValueStore) {
    this.hashValueStore = hashValueStore;
  }

  @Override
  public void clear() {
    final Lock lock = rwLock.writeLock();
    lock.lock();
    try {
      hashValueStore.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean containsKey(final byte[] key) throws StorageException {
    return get(key).isPresent();
  }

  @Override
  public Optional<byte[]> get(final byte[] key) throws StorageException {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      return Optional.ofNullable(hashValueStore.get(Bytes.wrap(key)));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Set<byte[]> getAllKeysThat(final Predicate<byte[]> returnCondition) {
    return stream()
        .filter(pair -> returnCondition.test(pair.getKey()))
        .map(Pair::getKey)
            .collect(Collectors.collectingAndThen(
                    Collectors.toSet(),
                    Collections::unmodifiableSet
            ));
  }

  @Override
  public Set<byte[]> getAllValuesFromKeysThat(final Predicate<byte[]> returnCondition) {
    return stream()
        .filter(pair -> returnCondition.test(pair.getKey()))
        .map(Pair::getValue)
            .collect(Collectors.collectingAndThen(
                    Collectors.toSet(),
                    Collections::unmodifiableSet
            ));
  }

  @Override
  public Stream<Pair<byte[], byte[]>> stream() {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      return ImmutableSet.copyOf(hashValueStore.entrySet()).stream()
          .map(bytesEntry -> Pair.of(bytesEntry.getKey().toArrayUnsafe(), bytesEntry.getValue()));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Stream<byte[]> streamKeys() {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      return ImmutableSet.copyOf(hashValueStore.entrySet()).stream()
          .map(bytesEntry -> bytesEntry.getKey().toArrayUnsafe());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean tryDelete(final byte[] key) {
    final Lock lock = rwLock.writeLock();
    if (lock.tryLock()) {
      try {
        hashValueStore.remove(Bytes.wrap(key));
      } finally {
        lock.unlock();
      }
      return true;
    }
    return false;
  }

  @Override
  public void close() {}

  @Override
  public KeyValueStorageTransaction startTransaction() {
    return new KeyValueStorageTransactionTransitionValidatorDecorator(new InMemoryTransaction());
  }

  public Set<Bytes> keySet() {
    return  ImmutableSet.copyOf((hashValueStore.keySet()));
  }

  private class InMemoryTransaction implements KeyValueStorageTransaction {

    private Map<Bytes, byte[]> updatedValues = new HashMap<>();
    private Set<Bytes> removedKeys = new HashSet<>();

    @Override
    public void put(final byte[] key, final byte[] value) {
      updatedValues.put(Bytes.wrap(key), value);
      removedKeys.remove(Bytes.wrap(key));
    }

    @Override
    public void remove(final byte[] key) {
      removedKeys.add(Bytes.wrap(key));
      updatedValues.remove(Bytes.wrap(key));
    }

    @Override
    public void commit() throws StorageException {
      final Lock lock = rwLock.writeLock();
      lock.lock();
      try {
        hashValueStore.putAll(updatedValues);
        removedKeys.forEach(hashValueStore::remove);
        updatedValues = null;
        removedKeys = null;
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void rollback() {
      updatedValues.clear();
      removedKeys.clear();
    }
  }

  public void dump(final PrintStream ps) {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      hashValueStore.forEach(
          (k, v) -> ps.printf("  %s : %s%n", k.toHexString(), Bytes.wrap(v).toHexString()));
    } finally {
      lock.unlock();
    }
  }
}
