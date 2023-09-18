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
package org.hyperledger.besu.ethereum.trie;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.KeyValueStorageTransaction;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public abstract class AbstractKeyValueStorageTest {

  protected abstract KeyValueStorage createStore() throws Exception;

  @Test
  public void twoStoresAreIndependent() throws Exception {
    final KeyValueStorage store1 = createStore();
    final KeyValueStorage store2 = createStore();

    final KeyValueStorageTransaction tx = store1.startTransaction();
    final byte[] key = bytesFromHexString("0001");
    final byte[] value = bytesFromHexString("0FFF");

    tx.put(key, value);
    tx.commit();

    final Optional<byte[]> result = store2.get(key);
    assertThat(result).isEmpty();
  }

  @Test
  public void put() throws Exception {
    final KeyValueStorage store = createStore();
    final byte[] key = bytesFromHexString("0F");
    final byte[] firstValue = bytesFromHexString("0ABC");
    final byte[] secondValue = bytesFromHexString("0DEF");

    KeyValueStorageTransaction tx = store.startTransaction();
    tx.put(key, firstValue);
    tx.commit();
    assertThat(store.get(key)).contains(firstValue);

    tx = store.startTransaction();
    tx.put(key, secondValue);
    tx.commit();
    assertThat(store.get(key)).contains(secondValue);
  }

  @Test
  public void streamKeys() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    final List<byte[]> keys =
        Stream.of("0F", "10", "11", "12")
            .map(this::bytesFromHexString)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        Collections::unmodifiableList
                ));
    keys.forEach(key -> tx.put(key, bytesFromHexString("0ABC")));
    tx.commit();
    Set<byte[]> set = store.stream().map(Pair::getKey).collect(Collectors.collectingAndThen(
            Collectors.toSet(),
            Collections::unmodifiableSet));

    assertThat(set)
            .containsExactlyInAnyOrder(keys.toArray(new byte[][] {}));
  }

  @Test
  public void getAllKeysThat() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.put(bytesFromHexString("0F"), bytesFromHexString("0ABC"));
    tx.put(bytesFromHexString("10"), bytesFromHexString("0ABC"));
    tx.put(bytesFromHexString("11"), bytesFromHexString("0ABC"));
    tx.put(bytesFromHexString("12"), bytesFromHexString("0ABC"));
    tx.commit();
    Set<byte[]> keys = store.getAllKeysThat(bv -> Bytes.wrap(bv).toString().contains("1"));
    assertThat(keys.size()).isEqualTo(3);
    assertThat(keys)
        .containsExactlyInAnyOrder(
            bytesFromHexString("10"), bytesFromHexString("11"), bytesFromHexString("12"));
  }

  @Test
  public void containsKey() throws Exception {
    final KeyValueStorage store = createStore();
    final byte[] key = bytesFromHexString("ABCD");
    final byte[] value = bytesFromHexString("DEFF");

    assertThat(store.containsKey(key)).isFalse();

    final KeyValueStorageTransaction transaction = store.startTransaction();
    transaction.put(key, value);
    transaction.commit();

    assertThat(store.containsKey(key)).isTrue();
  }

  @Test
  public void removeExisting() throws Exception {
    final KeyValueStorage store = createStore();
    final byte[] key = bytesFromHexString("0F");
    final byte[] value = bytesFromHexString("0ABC");

    KeyValueStorageTransaction tx = store.startTransaction();
    tx.put(key, value);
    tx.commit();

    tx = store.startTransaction();
    tx.remove(key);
    tx.commit();
    assertThat(store.get(key)).isEmpty();
  }

  @Test
  public void removeExistingSameTransaction() throws Exception {
    final KeyValueStorage store = createStore();
    final byte[] key = bytesFromHexString("0F");
    final byte[] value = bytesFromHexString("0ABC");

    KeyValueStorageTransaction tx = store.startTransaction();
    tx.put(key, value);
    tx.remove(key);
    tx.commit();
    assertThat(store.get(key)).isEmpty();
  }

  @Test
  public void removeNonExistent() throws Exception {
    final KeyValueStorage store = createStore();
    final byte[] key = bytesFromHexString("0F");

    KeyValueStorageTransaction tx = store.startTransaction();
    tx.remove(key);
    tx.commit();
    assertThat(store.get(key)).isEmpty();
  }

  @Test
  public void concurrentUpdate() throws Exception {
    final int keyCount = 1000;
    final KeyValueStorage store = createStore();

    final CountDownLatch finishedLatch = new CountDownLatch(2);
    final Function<byte[], Thread> updater =
        (value) ->
            new Thread(
                () -> {
                  try {
                    for (int i = 0; i < keyCount; i++) {
                      KeyValueStorageTransaction tx = store.startTransaction();
                      tx.put(Bytes.minimalBytes(i).toArrayUnsafe(), value);
                      tx.commit();
                    }
                  } finally {
                    finishedLatch.countDown();
                  }
                });

    // Run 2 concurrent transactions that write a bunch of values to the same keys
    final byte[] a = Bytes.of(10).toArrayUnsafe();
    final byte[] b = Bytes.of(20).toArrayUnsafe();
    updater.apply(a).start();
    updater.apply(b).start();

    finishedLatch.await();

    for (int i = 0; i < keyCount; i++) {
      final byte[] key = Bytes.minimalBytes(i).toArrayUnsafe();
      final byte[] actual = store.get(key).get();
      assertThat(Arrays.equals(actual, a) || Arrays.equals(actual, b)).isTrue();
    }

    store.close();
  }

  @Test
  public void transactionCommit() throws Exception {
    final KeyValueStorage store = createStore();
    // Add some values
    KeyValueStorageTransaction tx = store.startTransaction();
    tx.put(bytesOf(1), bytesOf(1));
    tx.put(bytesOf(2), bytesOf(2));
    tx.put(bytesOf(3), bytesOf(3));
    tx.commit();

    // Start transaction that adds, modifies, and removes some values
    tx = store.startTransaction();
    tx.put(bytesOf(2), bytesOf(3));
    tx.put(bytesOf(2), bytesOf(4));
    tx.remove(bytesOf(3));
    tx.put(bytesOf(4), bytesOf(8));

    // Check values before committing have not changed
    assertThat(store.get(bytesOf(1))).contains(bytesOf(1));
    assertThat(store.get(bytesOf(2))).contains(bytesOf(2));
    assertThat(store.get(bytesOf(3))).contains(bytesOf(3));
    assertThat(store.get(bytesOf(4))).isEmpty();

    tx.commit();

    // Check that values have been updated after commit
    assertThat(store.get(bytesOf(1))).contains(bytesOf(1));
    assertThat(store.get(bytesOf(2))).contains(bytesOf(4));
    assertThat(store.get(bytesOf(3))).isEmpty();
    assertThat(store.get(bytesOf(4))).contains(bytesOf(8));
  }

  @Test
  public void transactionRollback() throws Exception {
    final KeyValueStorage store = createStore();
    // Add some values
    KeyValueStorageTransaction tx = store.startTransaction();
    tx.put(bytesOf(1), bytesOf(1));
    tx.put(bytesOf(2), bytesOf(2));
    tx.put(bytesOf(3), bytesOf(3));
    tx.commit();

    // Start transaction that adds, modifies, and removes some values
    tx = store.startTransaction();
    tx.put(bytesOf(2), bytesOf(3));
    tx.put(bytesOf(2), bytesOf(4));
    tx.remove(bytesOf(3));
    tx.put(bytesOf(4), bytesOf(8));

    // Check values before committing have not changed
    assertThat(store.get(bytesOf(1))).contains(bytesOf(1));
    assertThat(store.get(bytesOf(2))).contains(bytesOf(2));
    assertThat(store.get(bytesOf(3))).contains(bytesOf(3));
    assertThat(store.get(bytesOf(4))).isEmpty();

    tx.rollback();

    // Check that values have not changed after rollback
    assertThat(store.get(bytesOf(1))).contains(bytesOf(1));
    assertThat(store.get(bytesOf(2))).contains(bytesOf(2));
    assertThat(store.get(bytesOf(3))).contains(bytesOf(3));
    assertThat(store.get(bytesOf(4))).isEmpty();
  }

  @Test
  public void transactionCommitEmpty() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.commit();
  }

  @Test
  public void transactionRollbackEmpty() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.rollback();
  }

  @Test(expected = IllegalStateException.class)
  public void transactionPutAfterCommit() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.commit();
    tx.put(bytesOf(1), bytesOf(1));
  }

  @Test(expected = IllegalStateException.class)
  public void transactionRemoveAfterCommit() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.commit();
    tx.remove(bytesOf(1));
  }

  @Test(expected = IllegalStateException.class)
  public void transactionPutAfterRollback() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.rollback();
    tx.put(bytesOf(1), bytesOf(1));
  }

  @Test(expected = IllegalStateException.class)
  public void transactionRemoveAfterRollback() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.rollback();
    tx.remove(bytesOf(1));
  }

  @Test(expected = IllegalStateException.class)
  public void transactionCommitAfterRollback() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.rollback();
    tx.commit();
  }

  @Test(expected = IllegalStateException.class)
  public void transactionCommitTwice() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.commit();
    tx.commit();
  }

  @Test(expected = IllegalStateException.class)
  public void transactionRollbackAfterCommit() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.commit();
    tx.rollback();
  }

  @Test(expected = IllegalStateException.class)
  public void transactionRollbackTwice() throws Exception {
    final KeyValueStorage store = createStore();
    final KeyValueStorageTransaction tx = store.startTransaction();
    tx.rollback();
    tx.rollback();
  }

  @Test
  public void twoTransactions() throws Exception {
    final KeyValueStorage store = createStore();

    final KeyValueStorageTransaction tx1 = store.startTransaction();
    final KeyValueStorageTransaction tx2 = store.startTransaction();

    tx1.put(bytesOf(1), bytesOf(1));
    tx2.put(bytesOf(2), bytesOf(2));

    tx1.commit();
    tx2.commit();

    assertThat(store.get(bytesOf(1))).contains(bytesOf(1));
    assertThat(store.get(bytesOf(2))).contains(bytesOf(2));
  }

  @Test
  public void transactionIsolation() throws Exception {
    final int keyCount = 1000;
    final KeyValueStorage store = createStore();

    final CountDownLatch finishedLatch = new CountDownLatch(2);
    final Function<byte[], Thread> txRunner =
        (value) ->
            new Thread(
                () -> {
                  final KeyValueStorageTransaction tx = store.startTransaction();
                  for (int i = 0; i < keyCount; i++) {
                    tx.put(Bytes.minimalBytes(i).toArrayUnsafe(), value);
                  }
                  try {
                    tx.commit();
                  } finally {
                    finishedLatch.countDown();
                  }
                });

    // Run 2 concurrent transactions that write a bunch of values to the same keys
    final byte[] a = bytesOf(10);
    final byte[] b = bytesOf(20);
    txRunner.apply(a).start();
    txRunner.apply(b).start();

    finishedLatch.await();

    // Check that transaction results are isolated (not interleaved)
    final List<byte[]> finalValues = new ArrayList<>(keyCount);
    for (int i = 0; i < keyCount; i++) {
      final byte[] key = Bytes.minimalBytes(i).toArrayUnsafe();
      finalValues.add(store.get(key).get());
    }

    // Expecting the same value for all entries
    final byte[] expected = finalValues.get(0);
    for (final byte[] actual : finalValues) {
      assertThat(actual).containsExactly(expected);
    }

    assertThat(Arrays.equals(expected, a) || Arrays.equals(expected, b)).isTrue();

    store.close();
  }

  /*
   * Used to mimic the wrapping with Bytes performed in Besu
   */
  protected byte[] bytesFromHexString(final String hex) {
    return Bytes.fromHexString(hex).toArrayUnsafe();
  }

  protected byte[] bytesOf(final int... bytes) {
    return Bytes.of(bytes).toArrayUnsafe();
  }
}
