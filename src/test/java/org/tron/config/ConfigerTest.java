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
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.InitialWitness;


public class ConfigerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testGeClientTestEcKey() {
    ECKey key = ECKey.fromPrivate(
        Hex.decode("1cd5a70741c6e583d2dd3c5f17231e608eb1e52437210d948c5085e141c2d830"));

    logger.info("address = {}", ByteArray.toHexString(key.getAddress()));

    assertEquals("125b6c87b3d67114b3873977888c34582f27bbb0",
        ByteArray.toHexString(key.getAddress()));
  }

  @Test
  public void testInitialWitness() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.NORMAL_CONF));
    InitialWitness initialWitness = Args.getInstance().getInitialWitness();
    assertEquals("http://tron.org", initialWitness.getLocalWitness().getUrl());
    assertEquals(3, initialWitness.getActiveWitnessList().size());
    assertEquals("0x01", initialWitness.getActiveWitnessList().get(0).getPublicKey());
  }

}
