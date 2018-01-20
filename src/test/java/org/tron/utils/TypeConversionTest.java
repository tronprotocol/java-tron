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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tron.common.utils.TypeConversion.bytesToHexString;
import static org.tron.common.utils.TypeConversion.bytesToLong;
import static org.tron.common.utils.TypeConversion.hexStringToBytes;
import static org.tron.common.utils.TypeConversion.longToBytes;

import java.util.Arrays;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TypeConversionTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testLongToBytes() {
    byte[] result = longToBytes(123L);
    assertTrue(Arrays.equals(new byte[]{0, 0, 0, 0, 0, 0, 0, 123}, result));
    logger.info("long 123 to bytes is: {}", result);
  }

  @Test
  public void testBytesToLong() {
    long result = bytesToLong(new byte[]{0, 0, 0, 0, 0, 0, 0, 124});
    assertEquals(124L, result);
    logger.info("bytes 124 to long is: {}", result);
  }

  @Test
  public void testBytesToHexString() {
    String result = bytesToHexString(new byte[]{0, 0, 0, 0, 0, 0, 0, 125});
    assertEquals("000000000000007d", result);
    logger.info("bytes 125 to hex string is: {}", result);
  }

  @Test
  public void testHexStringToBytes() {
    byte[] result = hexStringToBytes("7f");
    assertTrue(Arrays.equals(new byte[]{127}, result));
    logger.info("hex string 7f to bytes is: {}", result);
  }
}
