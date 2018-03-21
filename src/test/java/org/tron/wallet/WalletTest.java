/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.wallet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;

@Slf4j
public class WalletTest {
  @Test
  public void testWallet() {
    Wallet wallet = new Wallet();
    Wallet wallet2 = new Wallet();
    logger.info("wallet address = {}", ByteArray.toHexString(wallet
        .getAddress()));
    logger.info("wallet2 address = {}", ByteArray.toHexString(wallet2
            .getAddress()));
    assertFalse(wallet.getAddress().equals(wallet2.getAddress()));
  }

  @Test
  public void testGetAddress() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    Wallet wallet = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
            .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet
            .getAddress()));
    assertArrayEquals(wallet.getAddress(), ecKey.getAddress());
  }

  @Test
  public void testGetEcKey() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    Wallet wallet = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
            .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet
            .getAddress()));
    assertEquals("Wallet ECKey should match provided ECKey", wallet.getEcKey(), ecKey);
  }
}
