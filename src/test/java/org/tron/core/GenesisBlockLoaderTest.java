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

package org.tron.core;

import com.alibaba.fastjson.JSON;
import com.google.common.io.ByteStreams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

public class GenesisBlockLoaderTest {
  @Test
  public void testGenesisBlockLoader() {
    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream("genesis-test.json");
      String json = new String(ByteStreams.toByteArray(is));

      GenesisBlockLoader genesisBlockLoader = JSON.parseObject(json, GenesisBlockLoader.class);

      System.out.println(genesisBlockLoader);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
