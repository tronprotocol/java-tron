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

package org.tron.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.TxInputCapsule;

public class TxInputCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testNewTXInput() {
    logger.info("test new TXInput: {}", TxInputCapsule.newTxInput(new
        byte[]{}, 1, new byte[]{}, new byte[]{}));
  }

  @Test
  public void testToPrintString() {
    logger.info("test to print string: {}", TxInputCapsule.toPrintString
        (TxInputCapsule.newTxInput(new byte[]{}, 1, new byte[]{}, new
            byte[]{})));
  }
}
