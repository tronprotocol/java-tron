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

/** Base exception class for problems encountered in the domain for storage. */
public class StorageException extends RuntimeException {

  /**
   * Constructs a new storage exception with the specified cause.
   *
   * @param cause saved for later retrieval by the {@link #getCause()} method). (A {@code null}
   *     value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public StorageException(final Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new storage exception with the specified detail message and cause.
   *
   * @param message the detail that may be retrieved later by Throwable.getMessage().
   * @param cause saved for later retrieval by the {@link #getCause()} method). (A {@code null}
   *     value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public StorageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new storage exception with the specified detail message.
   *
   * @param message the detail that may be retrieved later by Throwable.getMessage().
   */
  public StorageException(final String message) {
    super(message);
  }
}
