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
package org.hyperledger.besu.crypto;

import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

/** Various utilities for providing hashes (digests) of arbitrary data. */
public abstract class Hash {
  private Hash() {}

  public static final String KECCAK256_ALG = "KECCAK-256";

  private static final Supplier<MessageDigest> KECCAK256_SUPPLIER =
      Suppliers.memoize(() -> messageDigest(KECCAK256_ALG));

  private static MessageDigest messageDigest(final String algorithm) {
    try {
      return MessageDigestFactory.create(algorithm);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param digestSupplier the digest supplier to use
   * @return A digest.
   */
  private static byte[] digestUsingAlgorithm(
      final Bytes input, final Supplier<MessageDigest> digestSupplier) {
    try {
      final MessageDigest digest = (MessageDigest) digestSupplier.get().clone();
      input.update(digest);
      return digest.digest();
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * Digest using keccak-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 keccak256(final Bytes input) {
    return Bytes32.wrap(digestUsingAlgorithm(input, KECCAK256_SUPPLIER));
  }
}
