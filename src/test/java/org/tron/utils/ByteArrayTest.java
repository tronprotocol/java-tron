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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.ByteArray;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ByteArrayTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testToHexString() {
    assertEquals("10", ByteArray.toHexString(new byte[]{16}));
    logger.info("Byte: byte 16 to hex string = {}", ByteArray.toHexString(new byte[]{16}));

  }

  @Test
  public void testHexStringToByte() {
    assertArrayEquals(new byte[]{17}, ByteArray.fromHexString("0x11"));
    logger.info("Byte: hex string 0x11 to byte = {}", ByteArray.fromHexString("0x11"));
    assertArrayEquals(new byte[]{16}, ByteArray.fromHexString("10"));
    logger.info("Byte: hex string 10 to byte = {}", ByteArray.fromHexString("10"));
    assertArrayEquals(new byte[]{1}, ByteArray.fromHexString("1"));
    logger.info("Byte: hex string 1 to byte = {}", ByteArray.fromHexString("1"));
  }

  @Test
  public void testToLong() {
    assertEquals(13L, ByteArray.toLong(new byte[]{13}));
    logger.info("Byte: byte 13 to long = {}", ByteArray.toLong(new byte[]{13}));
  }

  @Test
  public void testFromLong() {
    assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 127}, ByteArray.fromLong(127L));
    logger.info("Byte: long 127L to byte = {}", ByteArray.fromLong(127L));
  }

  @Test
  public void testToInt() {
    assertEquals(17291729, ByteArray.toInt(new byte[]{1, 7, -39, -47}));
    logger.info("Int: {}", ByteArray.toInt(new byte[]{1, 7, -39, -47}));
  }

  @Test
  public void testFromInt() {
    assertArrayEquals(new byte[]{1, 7, -39, -47}, ByteArray.fromInt(17291729));
    logger.info("Byte: {}", ByteArray.fromInt(17291729));
  }

  @Test
  public void test2ToHexString() {
    byte[] bs = new byte[]{8, 12, 16, 21};
    assertEquals("080c1015", ByteArray.toHexString(bs));
    assertEquals("080c1015", Hex.toHexString(bs));
    logger.info("utils.ByteArray.toHexString: {}", ByteArray.toHexString(bs));
    logger.info("Hex.toHexString: {}", Hex.toHexString(bs));
  }
}
