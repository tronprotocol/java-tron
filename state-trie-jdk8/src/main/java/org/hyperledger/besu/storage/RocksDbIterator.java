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
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkState;

public class RocksDbIterator implements Iterator<Pair<byte[], byte[]>>, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(RocksDbIterator.class);

  private final RocksIterator rocksIterator;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private RocksDbIterator(final RocksIterator rocksIterator) {
    this.rocksIterator = rocksIterator;
  }

  public static RocksDbIterator create(final RocksIterator rocksIterator) {
    return new RocksDbIterator(rocksIterator);
  }

  @Override
  public boolean hasNext() {
    assertOpen();
    return rocksIterator.isValid();
  }

  @Override
  public Pair<byte[], byte[]> next() {
    assertOpen();
    try {
      rocksIterator.status();
    } catch (final RocksDBException e) {
      LOG.error(
          String.format("%s encountered a problem while iterating.", getClass().getSimpleName()),
          e);
    }
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final byte[] key = rocksIterator.key();
    final byte[] value = rocksIterator.value();
    rocksIterator.next();
    return Pair.of(key, value);
  }

  public byte[] nextKey() {
    assertOpen();
    try {
      rocksIterator.status();
    } catch (final RocksDBException e) {
      LOG.error(
          String.format("%s encountered a problem while iterating.", getClass().getSimpleName()),
          e);
    }
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final byte[] key = rocksIterator.key();
    rocksIterator.next();
    return key;
  }

  public Stream<Pair<byte[], byte[]>> toStream() {
    assertOpen();
    final Spliterator<Pair<byte[], byte[]>> spliterator =
        Spliterators.spliteratorUnknownSize(
            this,
            Spliterator.IMMUTABLE
                | Spliterator.DISTINCT
                | Spliterator.NONNULL
                | Spliterator.ORDERED
                | Spliterator.SORTED);

    return StreamSupport.stream(spliterator, false).onClose(this::close);
  }

  public Stream<byte[]> toStreamKeys() {
    assertOpen();
    final Spliterator<byte[]> spliterator =
        Spliterators.spliteratorUnknownSize(
            new Iterator<byte[]>() {
              @Override
              public boolean hasNext() {
                return RocksDbIterator.this.hasNext();
              }

              @Override
              public byte[] next() {
                return RocksDbIterator.this.nextKey();
              }
            },
            Spliterator.IMMUTABLE
                | Spliterator.DISTINCT
                | Spliterator.NONNULL
                | Spliterator.ORDERED
                | Spliterator.SORTED);

    return StreamSupport.stream(spliterator, false).onClose(this::close);
  }

  private void assertOpen() {
    checkState(
        !closed.get(),
        String.format("Attempt to read from a closed %s", getClass().getSimpleName()));
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      rocksIterator.close();
    }
  }
}
