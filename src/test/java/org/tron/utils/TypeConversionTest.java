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

import static org.tron.utils.TypeConversion.bytesToLong;
import static org.tron.utils.TypeConversion.longToBytes;
import static org.tron.utils.TypeConversion.bytesToHexString;
import static org.tron.utils.TypeConversion.hexStringToBytes;


public class TypeConversionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("Test");

    @Test
    public void testLongToBytes() {
        byte[] result = longToBytes(123L);
        LOGGER.info("long 123 to bytes is: {}", result);
    }

    @Test
    public void testBytesToLong() {
        long result = bytesToLong(new byte[]{0, 0, 0, 0, 0, 0, 0, 124});
        LOGGER.info("bytes 124 to long is: {}", result);
    }

    @Test
    public void testBytesToHexString() {
        String result = bytesToHexString(new byte[]{0, 0, 0, 0, 0, 0, 0, 125});
        LOGGER.info("bytes 125 to hex string is: {}", result);
    }

    @Test
    public void testHexStringToBytes() {
        byte[] result = hexStringToBytes("7f");
        LOGGER.info("hex string 7f to bytes is: {}", result);
    }
}
