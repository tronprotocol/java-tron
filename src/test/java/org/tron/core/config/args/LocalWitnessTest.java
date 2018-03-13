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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalWitnessTest {

  private LocalWitness localWitness = new LocalWitness();

  @Before
  public void setLocalWitness() {
    localWitness
        .setPrivateKey("00f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62");
  }

  @Test
  public void whenSetNullPrivateKey() {
    localWitness.setPrivateKey(null);
  }

  @Test
  public void whenSetEmptyPrivateKey() {
    localWitness.setPrivateKey("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetBadFormatPrivateKey() {
    localWitness.setPrivateKey("a111");
  }

  @Test
  public void whenSetPrefixPrivateKey() {
    localWitness
        .setPrivateKey("0x00f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62");
    localWitness
        .setPrivateKey("0X00f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62");
  }

  @Test
  public void getPrivateKey() {
    Assert.assertEquals("00f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62",
        localWitness.getPrivateKey());
  }
}
