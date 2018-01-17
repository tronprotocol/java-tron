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

package org.tron.utils;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class ByteArrayTest {
  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testHexToString() {
    String hexString1 = ByteArray.toHexString(new byte[] {'a'});
    String hexString2 = ByteArray.toHexString(new byte[] {16});

    Assert.assertEquals(hexString1, "61");
    Assert.assertEquals(hexString2, "10");

    logger.info("Byte: byte a to hex string = {}", hexString1);
    logger.info("Byte: byte 16 to hex string = {}", hexString2);
  }

  @Test
  public void test2HexToString() {
    byte[] byteArrayExpected = new byte[] {16};
    String stringActual1 = ByteArray.toHexString(byteArrayExpected);
    String stringActual2 = Hex.toHexString(byteArrayExpected);

    Assert.assertEquals(stringActual1, stringActual2);

    logger.info("utils.ByteArray.toHexString: {}", stringActual1);
    logger.info("Hex.toHexString: {}", stringActual2);
  }

  @Test
  public void testHexStringToByte() {
    byte[] byteArrayExpected1 = new byte[] {17};
    byte[] byteArrayExpected2 = new byte[] {16};
    byte[] byteArrayExpected3 = new byte[] {1};

    byte[] byteArrayActual1 = ByteArray
            .fromHexString("0x11");
    byte[] byteArrayActual2 = ByteArray
            .fromHexString("10");
    byte[] byteArrayActual3 = ByteArray
            .fromHexString("1");

    Assert.assertArrayEquals(byteArrayExpected1, byteArrayActual1);
    Assert.assertArrayEquals(byteArrayExpected2, byteArrayActual2);
    Assert.assertArrayEquals(byteArrayExpected3, byteArrayActual3);

    logger.info("Byte: hex string 0x11 to byte = {}", byteArrayActual1);
    logger.info("Byte: hex string 10 to byte = {}", byteArrayActual2);
    logger.info("Byte: hex string 1 to byte = {}", byteArrayActual3);
  }

  @Test
  public void testByteToLong() {
    long longExpected = 13;

    long longActual = ByteArray.toLong(new
          byte[]{13});

    Assert.assertEquals(longExpected, longActual);

    logger.info("Byte: byte 13 to long = {}", longActual);
  }

  @Test
  public void testLongToByte() {
    byte[] byteArrayExpected = new byte[] {0, 0, 0, 0, 0, 0, 0, 127};
    byte[] byteArrayActual = ByteArray.fromLong(127L);

    Assert.assertArrayEquals(byteArrayExpected, byteArrayActual);

    logger.info("Byte: long 127L to byte = {}", byteArrayActual);
  }
}
