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

package org.tron.config;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.Configer;

import static org.junit.Assert.assertEquals;

public class ConfigerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testGeClientTestEcKey() {
    ECKey key = Configer.getMyKey();

    //logger.info("address = {}", ByteArray.toHexString(key.getAddress()));

    assertEquals("125b6c87b3d67114b3873977888c34582f27bbb0", ByteArray.toHexString(key.getAddress()));
  }
}
