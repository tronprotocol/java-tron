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

/**
 * This exception is thrown when there is an issue retrieving or decoding values from {@link
 * MerkleStorage}.
 */
public class MerkleTrieException extends RuntimeException {

  private Bytes32 hash;
  private Bytes location;

  public MerkleTrieException(final String message) {
    super(message);
  }

  public MerkleTrieException(final String message, final Bytes32 hash, final Bytes location) {
    super(message);
    this.hash = hash;
    this.location = location;
  }

  public MerkleTrieException(final String message, final Exception cause) {
    super(message, cause);
  }

  public Bytes32 getHash() {
    return hash;
  }

  public Bytes getLocation() {
    return location;
  }
}
