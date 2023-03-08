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

import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.RocksDBConfiguration;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RocksDBKeyValueStorageTest extends AbstractKeyValueStorageTest {

  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  @Override
  protected KeyValueStorage createStore() throws Exception {
    return new RocksDBKeyValueStorage(config());
  }

  private RocksDBConfiguration config() throws Exception {
    return new RocksDBConfigurationBuilder().databaseDir(folder.newFolder().toPath()).build();
  }
}
