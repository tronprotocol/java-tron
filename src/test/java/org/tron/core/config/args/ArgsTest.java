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

package org.tron.core.config.args;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.config.Configuration;

public class ArgsTest {

  @After
  public void destroy() {
    Args.clearParam();
  }

  @Test
  public void get() {
    Args.setParam(new String[]{}, Configuration.getByPath("config-junit.conf"));

    Args args = Args.getInstance();

    Assert.assertEquals("61ea9502165977c7b2be2be25d3030c21b7b33a4aeb0b13ac578001104bef721",
        args.getPrivateKey());

    Assert.assertEquals("database-test", args.getStorage().getDirectory());

    Assert.assertEquals(7080, args.getOverlay().getPort());

    Assert.assertEquals(7, args.getSeedNode().getIpList().size());

    GenesisBlock genesisBlock = args.getGenesisBlock();

    Assert.assertEquals(3, genesisBlock.getAssets().size());

    Assert.assertEquals(4, genesisBlock.getWitnesses().size());

    Assert.assertEquals("0", genesisBlock.getTimestamp());

    Assert.assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000",
        genesisBlock.getParentHash());

    Assert.assertEquals(
        Lists.newArrayList("00f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62"),
        args.getLocalWitnesses().getPrivateKeys());

    Assert.assertEquals(5000, args.getBlockInterval());
  }
}
