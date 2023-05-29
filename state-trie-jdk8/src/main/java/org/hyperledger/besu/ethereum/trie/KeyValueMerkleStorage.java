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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.KeyValueStorageTransaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeyValueMerkleStorage implements MerkleStorage {

  protected final KeyValueStorage keyValueStorage;
  protected final Map<Bytes32, Bytes> pendingUpdates = new HashMap<>();

  public KeyValueMerkleStorage(final KeyValueStorage keyValueStorage) {
    this.keyValueStorage = keyValueStorage;
  }

  @Override
  public Optional<Bytes> get(final Bytes location, final Bytes32 hash) {
    return pendingUpdates.containsKey(hash)
        ? Optional.of(pendingUpdates.get(hash))
        : keyValueStorage.get(hash.toArrayUnsafe()).map(Bytes::wrap);
  }

  @Override
  public void put(final Bytes location, final Bytes32 hash, final Bytes value) {
    pendingUpdates.put(hash, value);
  }

  @Override
  public void commit() {
    if (pendingUpdates.size() == 0) {
      // Nothing to do
      return;
    }
    final KeyValueStorageTransaction kvTx = keyValueStorage.startTransaction();
    for (final Map.Entry<Bytes32, Bytes> entry : pendingUpdates.entrySet()) {
      kvTx.put(entry.getKey().toArrayUnsafe(), entry.getValue().toArrayUnsafe());
    }
    kvTx.commit();

    pendingUpdates.clear();
  }

  @Override
  public void rollback() {
    pendingUpdates.clear();
  }
}
