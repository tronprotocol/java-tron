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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;


public class ConfigerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testGeClientTestEcKey() {
    final ECKey key = ECKey.fromPrivate(
        Hex.decode("1cd5a70741c6e583d2dd3c5f17231e608eb1e52437210d948c5085e141c2d830"));

    logger.info("address = {}", ByteArray.toHexString(key.getAddress()));

    assertEquals("125b6c87b3d67114b3873977888c34582f27bbb0",
        ByteArray.toHexString(key.getAddress()));
  }

}
