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

package org.tron.core.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.tron.common.utils.PublicMethod.getRandomPrivateKey;
import static org.tron.common.utils.client.utils.AbiUtil.generateOccupationConstantPrivateKey;
import static org.tron.core.Constant.ADD_PRE_FIX_STRING_MAINNET;

import com.typesafe.config.Config;
import java.lang.reflect.Field;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;

@Slf4j
public class ConfigurationTest {

  @Before
  public void resetSingleton()
      throws SecurityException, NoSuchFieldException, IllegalArgumentException,
      IllegalAccessException {
    Field instance = Configuration.class.getDeclaredField("config");
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  public void testGetEcKey() {
    ECKey key = ECKey.fromPrivate(Hex.decode(generateOccupationConstantPrivateKey()));
    assertNotNull(key);
    assertEquals("2e988a386a799f506693793c6a5af6b54dfaabfb",
        ByteArray.toHexString(key.getAddress()).substring(2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenNullPathGetShouldThrowIllegalArgumentException() {
    Configuration.getByFileName(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenEmptyPathGetShouldThrowIllegalArgumentException() {
    Configuration.getByFileName(StringUtils.EMPTY, StringUtils.EMPTY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getShouldNotFindConfiguration() {
    Config config = Configuration.getByFileName("notExistingPath", "notExistingPath");
    assertFalse(config.hasPath("storage"));
    assertFalse(config.hasPath("overlay"));
    assertFalse(config.hasPath("seed.node"));
    assertFalse(config.hasPath("genesis.block"));
  }

  @Test
  public void getShouldReturnConfiguration() {
    Config config = Configuration.getByFileName(Constant.TEST_CONF, Constant.TEST_CONF);
    assertTrue(config.hasPath("storage"));
    assertTrue(config.hasPath("seed.node"));
    assertTrue(config.hasPath("genesis.block"));
  }

  @Test
  public void getConfigurationWhenOnlyConfFileName() {
    URL res = getClass().getClassLoader().getResource(Constant.TEST_CONF);
    Config config = Configuration.getByFileName("", res.getPath());
    assertTrue(config.hasPath("storage"));
    assertTrue(config.hasPath("seed.node"));
    assertTrue(config.hasPath("genesis.block"));
  }
}
