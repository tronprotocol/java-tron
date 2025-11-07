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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.SecureRandom;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.exception.TronError;
import org.tron.core.exception.TronError.ErrCode;

public class LocalWitnessTest {

  private final LocalWitnesses localWitness = new LocalWitnesses();
  private static final String PRIVATE_KEY = PublicMethod.getRandomPrivateKey();

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setLocalWitness() {
    localWitness
        .setPrivateKeys(
            Lists.newArrayList(
                PRIVATE_KEY));
  }

  @AfterClass
  public static void clear() {
    Args.clearParam();
  }

  @Test
  public void whenSetNullPrivateKey() {
    localWitness.setPrivateKeys(null);
    Assert.assertNotNull(localWitness.getPrivateKey());
    Assert.assertNotNull(localWitness.getPublicKey());
  }

  @Test(expected = TronError.class)
  public void whenSetEmptyPrivateKey() {
    localWitness.setPrivateKeys(Lists.newArrayList(""));
    fail("private key must be 64-bits hex string");
  }

  @Test(expected = TronError.class)
  public void whenSetBadFormatPrivateKey() {
    localWitness.setPrivateKeys(Lists.newArrayList("a111"));
    fail("private key must be 64-bits hex string");
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
  public void testValidPrivateKey() {
    LocalWitnesses localWitnesses = new LocalWitnesses();

    try {
      localWitnesses.addPrivateKeys(PRIVATE_KEY);
      Assert.assertEquals(1, localWitnesses.getPrivateKeys().size());
      Assert.assertEquals(PRIVATE_KEY, localWitnesses.getPrivateKeys().get(0));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testValidPrivateKeyWithPrefix() {
    LocalWitnesses localWitnesses = new LocalWitnesses();

    try {
      localWitnesses.addPrivateKeys("0x" + PRIVATE_KEY);
      Assert.assertEquals(1, localWitnesses.getPrivateKeys().size());
      Assert.assertEquals("0x" + PRIVATE_KEY, localWitnesses.getPrivateKeys().get(0));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testInvalidPrivateKey() {
    LocalWitnesses localWitnesses = new LocalWitnesses();
    String expectedMessage = "private key must be 64 hex string";
    assertTronError(localWitnesses, null, expectedMessage);
    assertTronError(localWitnesses, "", expectedMessage);
    assertTronError(localWitnesses, "  ", expectedMessage);
    assertTronError(localWitnesses, "11111", expectedMessage);
    String expectedMessage2 = "private key must be hex string";
    SecureRandom secureRandom = new SecureRandom();
    byte[] keyBytes = new byte[31];
    secureRandom.nextBytes(keyBytes);
    final String privateKey = ByteArray.toHexString(keyBytes) + "  ";
    assertTronError(localWitnesses, privateKey, expectedMessage2);
    final String privateKey2 = "xy" + ByteArray.toHexString(keyBytes);
    assertTronError(localWitnesses, privateKey2, expectedMessage2);
  }

  private void assertTronError(LocalWitnesses localWitnesses, String privateKey,
      String expectedMessage) {
    TronError thrown = assertThrows(TronError.class,
        () -> localWitnesses.addPrivateKeys(privateKey));
    assertEquals(ErrCode.WITNESS_INIT, thrown.getErrCode());
    assertTrue(thrown.getMessage().contains(expectedMessage));
  }

  @Test
  public void testHexStringFormat() {
    Assert.assertTrue(StringUtil.isHexadecimal("0123456789abcdefABCDEF"));
    Assert.assertFalse(StringUtil.isHexadecimal(null));
    Assert.assertFalse(StringUtil.isHexadecimal(""));
    Assert.assertFalse(StringUtil.isHexadecimal("abc"));
    Assert.assertFalse(StringUtil.isHexadecimal(" "));
    Assert.assertFalse(StringUtil.isHexadecimal("123xyz"));
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
    localWitnesses.initWitnessAccountAddress(new byte[0], true);
    Assert.assertNotNull(localWitnesses1.getPublicKey());

    LocalWitnesses localWitnesses2 = new LocalWitnesses();
    Assert.assertNull(localWitnesses2.getPrivateKey());
    Assert.assertNull(localWitnesses2.getPublicKey());
    localWitnesses2.initWitnessAccountAddress(null, true);
    LocalWitnesses localWitnesses3 = new LocalWitnesses();
    Assert.assertNull(localWitnesses3.getWitnessAccountAddress());
  }

  @Test
  public void testLocalWitnessConfig() throws IOException {
    Args.setParam(
        new String[]{"--output-directory", temporaryFolder.newFolder().toString(), "-w", "--debug"},
        "config-localtest.conf");
    LocalWitnesses witness = Args.getLocalWitnesses();
    Assert.assertNotNull(witness.getPrivateKey());
    Assert.assertNotNull(witness.getWitnessAccountAddress());
  }

  @Test
  public void testNullLocalWitnessConfig() throws IOException {
    Args.setParam(
        new String[]{"--output-directory", temporaryFolder.newFolder().toString(), "--debug"},
        Constant.TEST_CONF);
    LocalWitnesses witness = Args.getLocalWitnesses();
    Assert.assertNull(witness.getPrivateKey());
    Assert.assertNull(witness.getWitnessAccountAddress());
  }
}
