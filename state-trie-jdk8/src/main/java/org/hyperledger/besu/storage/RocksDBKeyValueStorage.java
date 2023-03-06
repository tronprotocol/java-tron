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

import org.apache.commons.lang3.tuple.Pair;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RocksDBKeyValueStorage implements KeyValueStorage {

  static {
    loadNativeLibrary();
  }

  private static final Logger LOG = LoggerFactory.getLogger(RocksDBKeyValueStorage.class);

  private final Options options;
  private final OptimisticTransactionDB db;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final WriteOptions tryDeleteOptions =
          new WriteOptions().setNoSlowdown(true).setIgnoreMissingColumnFamilies(true);

  public RocksDBKeyValueStorage(final RocksDBConfiguration configuration) {

    try {
      final Statistics stats = new Statistics();
      options =
              new Options()
                      .setCreateIfMissing(true)
                      .setMaxOpenFiles(configuration.getMaxOpenFiles())
                      .setTableFormatConfig(createBlockBasedTableConfig(configuration))
                      .setMaxBackgroundCompactions(configuration.getMaxBackgroundCompactions())
                      .setStatistics(stats);
      options.getEnv().setBackgroundThreads(configuration.getBackgroundThreadCount());

      db = OptimisticTransactionDB.open(options, configuration.getDatabaseDir().toString());
    } catch (final RocksDBException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void clear() throws StorageException {
    try (final RocksIterator rocksIterator = db.newIterator()) {
      rocksIterator.seekToFirst();
      if (rocksIterator.isValid()) {
        final byte[] firstKey = rocksIterator.key();
        rocksIterator.seekToLast();
        if (rocksIterator.isValid()) {
          final byte[] lastKey = rocksIterator.key();
          db.deleteRange(firstKey, lastKey);
          db.delete(lastKey);
        }
      }
    } catch (final RocksDBException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public boolean containsKey(final byte[] key) throws StorageException {
    return get(key).isPresent();
  }

  @Override
  public Optional<byte[]> get(final byte[] key) throws StorageException {
    throwIfClosed();

    try {
      return Optional.ofNullable(db.get(key));
    } catch (final RocksDBException e) {
      throw new StorageException(e);
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
  public Stream<Pair<byte[], byte[]>> stream() {
    final RocksIterator rocksIterator = db.newIterator();
    rocksIterator.seekToFirst();
    return RocksDbIterator.create(rocksIterator).toStream();
  }

  @Override
  public Stream<byte[]> streamKeys() {
    final RocksIterator rocksIterator = db.newIterator();
    rocksIterator.seekToFirst();
    return RocksDbIterator.create(rocksIterator).toStreamKeys();
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
  public boolean tryDelete(final byte[] key) {
    try {
      db.delete(tryDeleteOptions, key);
      return true;
    } catch (RocksDBException e) {
      if (e.getStatus().getCode() == Status.Code.Incomplete) {
        return false;
      } else {
        throw new StorageException(e);
      }
    }
  }

  @Override
  public KeyValueStorageTransaction startTransaction() throws StorageException {
    throwIfClosed();
    final WriteOptions options = new WriteOptions();
    options.setIgnoreMissingColumnFamilies(true);
    return new KeyValueStorageTransactionTransitionValidatorDecorator(
            new RocksDBTransaction(db.beginTransaction(options), options));
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      tryDeleteOptions.close();
      options.close();
      db.close();
    }
  }

  private BlockBasedTableConfig createBlockBasedTableConfig(final RocksDBConfiguration config) {
    final LRUCache cache = new LRUCache(config.getCacheCapacity());
    return new BlockBasedTableConfig().setBlockCache(cache);
  }

  private void throwIfClosed() {
    if (closed.get()) {
      LOG.error("Attempting to use a closed RocksDBKeyValueStorage");
      throw new IllegalStateException("Storage has been closed");
    }
  }

  private static void loadNativeLibrary() {
    try {
      RocksDB.loadLibrary();
    } catch (final ExceptionInInitializerError e) {
      if (e.getCause() instanceof UnsupportedOperationException) {
        LOG.info("Unable to load RocksDB library", e);
        throw new InvalidConfigurationException(
                "Unsupported platform detected. On Windows, ensure you have 64bit Java installed.");
      } else {
        throw e;
      }
    }
  }
}
