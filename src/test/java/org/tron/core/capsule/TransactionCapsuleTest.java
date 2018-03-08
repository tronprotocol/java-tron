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

package org.tron.core.capsule;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  protected TransactionCapsule transactionCapsule;

  @Test
  public void testCheckBalance() {

    Assert.assertTrue(transactionCapsule
        .checkBalance(new byte[]{1, 12, -1, 23}, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4", 1, 2));

    Assert.assertFalse(transactionCapsule
        .checkBalance(new byte[]{1, 12, -1, 23},
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 1, 2));

    Assert.assertFalse(transactionCapsule.checkBalance(new byte[]{1, 12, -1, 23}, "", 3, 2));

  }

}
