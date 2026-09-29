/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.setting;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;

public class RocksDbSettingsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void shouldKeepNativeBlockTableDefaults() throws Exception {
    Path database = temporaryFolder.newFolder("rocksdb").toPath();

    try (Options options = RocksDbSettings.getOptionsByDbName("test")) {
      try (RocksDB ignored = RocksDB.open(options, database.toString())) {
        // Opening the DB materializes the table factory and persists its native settings.
      }
    }

    Path optionsFile;
    try (Stream<Path> files = Files.list(database)) {
      optionsFile = files
          .filter(path -> path.getFileName().toString().startsWith("OPTIONS-"))
          .max(Comparator.comparing(path -> path.getFileName().toString()))
          .orElseThrow(() -> new AssertionError("RocksDB OPTIONS file not found"));
    }
    String nativeOptions = new String(Files.readAllBytes(optionsFile), StandardCharsets.UTF_8);

    assertTrue(nativeOptions.contains("block_size=4096"));
    assertTrue(nativeOptions.contains("pin_l0_filter_and_index_blocks_in_cache=false"));
    assertTrue(nativeOptions.contains("filter_policy=nullptr"));
  }
}
