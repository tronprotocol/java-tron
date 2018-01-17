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

import static org.tron.utils.TypeConversion.bytesToHexString;
import static org.tron.utils.TypeConversion.bytesToLong;
import static org.tron.utils.TypeConversion.hexStringToBytes;
import static org.tron.utils.TypeConversion.longToBytes;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TypeConversionTest {
  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testLongToBytes() {
    byte[] byteArrayExpected = new byte[] {0, 0, 0, 0, 0, 0, 0, 123};
    byte[] byteArrayActual = longToBytes(123L);

    Assert.assertArrayEquals(byteArrayExpected, byteArrayActual);
    logger.info("long 123 to bytes is: {}", byteArrayActual);
  }

  @Test
  public void testBytesToLong() {
    long longExpected = 124L;
    long longActual = bytesToLong(new byte[] {0, 0, 0, 0, 0, 0, 0, 124});

    Assert.assertEquals(longExpected, longActual);
    logger.info("bytes 124 to long is: {}", longActual);
  }

  @Test
  public void testBytesToHexString() {
    String stringExpected = "000000000000007d";
    String stringActual = bytesToHexString(new byte[] {0, 0, 0, 0, 0, 0, 0, 125});

    Assert.assertEquals(stringExpected, stringActual);
    logger.info("bytes 125 to hex string is: {}", stringActual);
  }

  @Test
  public void testHexStringToBytes() {
    byte[] byteArrayExpected = new byte[] {127};
    byte[] byteArrayActual = hexStringToBytes("7f");

    Assert.assertArrayEquals(byteArrayExpected, byteArrayActual);
    logger.info("hex string 7f to bytes is: {}", byteArrayActual);
  }
}
