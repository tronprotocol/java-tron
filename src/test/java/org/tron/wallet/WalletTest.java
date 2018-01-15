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

package org.tron.wallet;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.crypto.ECKey;
import org.tron.utils.ByteArray;
import org.tron.utils.Utils;
import org.tron.wallet.Wallet;

public class WalletTest {
  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testWallet() {
    Wallet wallet = new Wallet();
    Wallet wallet2 = new Wallet();
    logger.info("wallet address = {}", ByteArray.toHexString(wallet
        .getAddress()));
    assertFalse(wallet.getAddress().equals(wallet2.getAddress()));
  }

  @Test
  public void testGetAddress() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    Wallet wallet = new Wallet(ecKey);
    assertArrayEquals(wallet.getAddress(), ecKey.getAddress());
  }

  @Test
  public void testGetECKey() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    Wallet wallet = new Wallet(ecKey);
    assertEquals("Wallet ECKey should match provided ECKey", wallet.getEcKey(), ecKey);
  }
}
