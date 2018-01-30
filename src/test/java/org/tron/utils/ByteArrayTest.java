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

public class ByteArrayTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testToHexString() {
    logger.info("Byte: byte 16 to hex string = {}", ByteArray.toHexString
        (new byte[]{16}));
  }

  @Test
  public void testHexStringToByte() {
    logger.info("Byte: hex string 0x11 to byte = {}", ByteArray
        .fromHexString("0x11"));
    logger.info("Byte: hex string 10 to byte = {}", ByteArray
        .fromHexString("10"));
    logger.info("Byte: hex string 1 to byte = {}", ByteArray
        .fromHexString("1"));
  }

  @Test
  public void testToLong() {
    logger.info("Byte: byte 13 to long = {}", ByteArray.toLong(new
        byte[]{13}));
  }

  @Test
  public void testFromLong() {
    logger.info("Byte: long 127L to byte = {}", ByteArray.fromLong(127L));
  }

  @Test
  public void test2ToHexString() {
    byte[] bs = new byte[]{};

    logger.info("utils.ByteArray.toHexString: {}", ByteArray.toHexString
        (bs));
    logger.info("Hex.toHexString: {}", Hex.toHexString(bs));
  }
}
