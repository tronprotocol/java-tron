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

/** A transaction that can atomically commit a sequence of operations to a key-value store. */
@Unstable
public interface KeyValueStorageTransaction {

  /**
   * Associates the specified value with the specified key.
   *
   * <p>If a previously value had been store against the given key, the old value is replaced by the
   * given value.
   *
   * @param key the given value is to be associated with.
   * @param value associated with the specified key.
   */
  void put(byte[] key, byte[] value);

  /**
   * When the given key is present, the key and mapped value will be removed from storage.
   *
   * @param key the key and mapped value that will be removed.
   */
  void remove(byte[] key);

  /**
   * Performs an atomic commit of all the operations queued in the transaction.
   *
   * @throws StorageException problem was encountered preventing the commit
   */
  void commit() throws StorageException;

  /** Reset the transaction to a state prior to any operations being queued. */
  void rollback();
}
