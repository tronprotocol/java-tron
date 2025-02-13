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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;

public class LocalWitnessTest {

  private final LocalWitnesses localWitness = new LocalWitnesses();
  private static final String PRIVATE_KEY = PublicMethod.getRandomPrivateKey();

  @Before
  public void setLocalWitness() {
    localWitness
        .setPrivateKeys(
            Lists.newArrayList(
                    PRIVATE_KEY));
  }

  @Test
  public void whenSetNullPrivateKey() {
    localWitness.setPrivateKeys(null);
    Assert.assertNotNull(localWitness.getPrivateKey());
    Assert.assertNotNull(localWitness.getPublicKey());
  }

  @Test
  public void whenSetEmptyPrivateKey() {
    localWitness.setPrivateKeys(Lists.newArrayList(""));
    Assert.assertNotNull(localWitness.getPrivateKey());
    Assert.assertNotNull(localWitness.getPublicKey());
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetBadFormatPrivateKey() {
    localWitness.setPrivateKeys(Lists.newArrayList("a111"));
  }

  @Test
  public void whenSetPrefixPrivateKey() {
    localWitness
        .setPrivateKeys(Lists
            .newArrayList("0x" + PRIVATE_KEY));
    localWitness
        .setPrivateKeys(Lists
            .newArrayList("0X" + PRIVATE_KEY));
    Assert.assertNotNull(localWitness.getPrivateKey());
  }

  @Test
  public void getPrivateKey() {
    Assert.assertEquals(Lists
            .newArrayList(PRIVATE_KEY),
        localWitness.getPrivateKeys());
  }

  @Test
  public void testConstructor() {
    LocalWitnesses localWitnesses = new LocalWitnesses(PublicMethod.getRandomPrivateKey());
    LocalWitnesses localWitnesses1 =
        new LocalWitnesses(Lists.newArrayList(PublicMethod.getRandomPrivateKey()));
    localWitnesses.setWitnessAccountAddress(new byte[0]);
    Assert.assertNotNull(localWitnesses1.getPublicKey());

    LocalWitnesses localWitnesses2 = new LocalWitnesses();
    Assert.assertNull(localWitnesses2.getPrivateKey());
    Assert.assertNull(localWitnesses2.getPublicKey());
    localWitnesses2.initWitnessAccountAddress(true);
    LocalWitnesses localWitnesses3 = new LocalWitnesses();
    Assert.assertNotNull(localWitnesses3.getWitnessAccountAddress(true));
  }
}
