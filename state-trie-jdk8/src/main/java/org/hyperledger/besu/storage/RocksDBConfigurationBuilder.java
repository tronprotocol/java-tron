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

import java.nio.file.Path;

public class RocksDBConfigurationBuilder {
  public static final int DEFAULT_MAX_OPEN_FILES = -1;
  public static final long DEFAULT_CACHE_CAPACITY = 1 * 1024 * 1024 * 1024L;
  public static final long DEFAULT_WRITE_BUFFER_SIZE = 256 * 1024 * 1024L;
  public static final int DEFAULT_MAX_BACKGROUND_COMPACTIONS = Runtime.getRuntime().availableProcessors();
  public static final int DEFAULT_BACKGROUND_THREAD_COUNT = 4;
  public static final boolean DEFAULT_IS_HIGH_SPEC = false;
  private Path databaseDir;
  private String label = "blockchain";
  private int maxOpenFiles = DEFAULT_MAX_OPEN_FILES;
  private long cacheCapacity = DEFAULT_CACHE_CAPACITY;
  private long writeBufferSize = DEFAULT_WRITE_BUFFER_SIZE;
  private int maxBackgroundCompactions = DEFAULT_MAX_BACKGROUND_COMPACTIONS;
  private int backgroundThreadCount = DEFAULT_BACKGROUND_THREAD_COUNT;
  private boolean isHighSpec = DEFAULT_IS_HIGH_SPEC;

  public RocksDBConfigurationBuilder databaseDir(final Path databaseDir) {
    this.databaseDir = databaseDir;
    return this;
  }

  public RocksDBConfigurationBuilder maxOpenFiles(final int maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  public RocksDBConfigurationBuilder label(final String label) {
    this.label = label;
    return this;
  }

  public RocksDBConfigurationBuilder cacheCapacity(final long cacheCapacity) {
    this.cacheCapacity = cacheCapacity;
    return this;
  }

  public RocksDBConfigurationBuilder writeBufferSize(final long writeBufferSize) {
    this.writeBufferSize = writeBufferSize;
    return this;
  }

  public RocksDBConfigurationBuilder maxBackgroundCompactions(final int maxBackgroundCompactions) {
    this.maxBackgroundCompactions = maxBackgroundCompactions;
    return this;
  }

  public RocksDBConfigurationBuilder backgroundThreadCount(final int backgroundThreadCount) {
    this.backgroundThreadCount = backgroundThreadCount;
    return this;
  }

  public RocksDBConfigurationBuilder isHighSpec(final boolean isHighSpec) {
    this.isHighSpec = isHighSpec;
    return this;
  }

  public static RocksDBConfigurationBuilder from(final RocksDBFactoryConfiguration configuration) {
    return new RocksDBConfigurationBuilder()
        .backgroundThreadCount(configuration.getBackgroundThreadCount())
        .cacheCapacity(configuration.getCacheCapacity())
        .writeBufferSize(configuration.getWriteBufferSize())
        .maxBackgroundCompactions(configuration.getMaxBackgroundCompactions())
        .maxOpenFiles(configuration.getMaxOpenFiles())
        .isHighSpec(configuration.isHighSpec());
  }

  public RocksDBConfiguration build() {
    return new RocksDBConfiguration(
        databaseDir,
        maxOpenFiles,
        maxBackgroundCompactions,
        backgroundThreadCount,
        cacheCapacity,
        writeBufferSize,
        label,
        isHighSpec);
  }
}
