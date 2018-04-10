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

package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.tron.common.utils.TypeConversion.bytesToHexString;
import static org.tron.common.utils.TypeConversion.bytesToLong;
import static org.tron.common.utils.TypeConversion.hexStringToBytes;
import static org.tron.common.utils.TypeConversion.longToBytes;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TypeConversionTest {
  @Test
  public void testLongToBytes() {
    byte[] result = longToBytes(123L);
    //logger.info("long 123 to bytes is: {}", result);
    byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 123};
    assertArrayEquals(expected, result);

  }

  @Test
  public void testBytesToLong() {
    long result = bytesToLong(new byte[]{0, 0, 0, 0, 0, 0, 0, 124});
    //logger.info("bytes 124 to long is: {}", result);
    assertEquals(124L, result);

  }

  @Test
  public void testBytesToHexString() {
    String result = bytesToHexString(new byte[]{0, 0, 0, 0, 0, 0, 0, 125});
    //logger.info("bytes 125 to hex string is: {}", result);
    assertEquals("000000000000007d", result);
  }

  @Test
  public void testHexStringToBytes() {
    byte[] result = hexStringToBytes("7f");
    //logger.info("hex string 7f to bytes is: {}", result);
    byte[] expected = new byte[]{127};
    assertArrayEquals(expected, result);

  }
}
