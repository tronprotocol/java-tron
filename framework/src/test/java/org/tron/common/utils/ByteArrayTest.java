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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

@Slf4j
public class ByteArrayTest {

  @Test
  public void testToHexString() {
    //logger.info("Byte: byte 16 to hex string = {}", ByteArray.toHexString(new byte[]{16}));
    assertEquals("byte to hex string is wrong", "10", ByteArray.toHexString(new byte[]{16}));
  }

  @Test
  public void long2Bytes() {
    long a = 0x123456;
    byte[] bb = ByteArray.fromLong(a);
    System.out.println(bb[6]);
    System.out.println(bb[7]);
  }

  @Test
  public void testHexStringToByte() {
    //logger.info("Byte: hex string 0x11 to byte = {}", ByteArray.fromHexString("0x11"));
    byte[] expectedfirst = new byte[]{17};
    byte[] actualfirst = ByteArray.fromHexString("0x11");
    assertArrayEquals(expectedfirst, actualfirst);
    //logger.info("Byte: hex string 10 to byte = {}", ByteArray.fromHexString("10"));
    byte[] expectedsecond = new byte[]{16};
    byte[] actualsecond = ByteArray.fromHexString("10");
    assertArrayEquals(expectedsecond, actualsecond);
    //logger.info("Byte: hex string 1 to byte = {}", ByteArray.fromHexString("1"));
    byte[] expectedthird = new byte[]{1};
    byte[] actualthird = ByteArray.fromHexString("1");
    assertArrayEquals(expectedthird, actualthird);
  }

  @Test
  public void testToLong() {
    //logger.info("Byte: byte 13 to long = {}", ByteArray.toLong(new byte[]{13}));
    assertEquals("byte to long is wrong", 13L, ByteArray.toLong(new byte[]{13}));

  }

  @Test
  public void testFromLong() {
    //logger.info("Byte: long 127L to byte = {}", ByteArray.fromLong(127L));
    byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 127};
    byte[] actual = ByteArray.fromLong(127L);
    assertArrayEquals(expected, actual);

  }

  @Test
  public void test2ToHexString() {
    //byte[] bs = new byte[]{};
    //logger.info("utils.ByteArray.toHexString: {}", ByteArray.toHexString(bs));
    //logger.info("Hex.toHexString: {}", Hex.toHexString(bs));
    byte[] bss = new byte[]{8, 9, 12, 13, 14, 15, 16};
    assertEquals("ByteArray.toHexString is not equals Hex.toHexString", ByteArray.toHexString(bss),
        Hex.toHexString(bss));
  }
}
