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

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TxInputCapsule;

@Slf4j
public class TxInputCapsuleTest {
  @Test
  public void testTxOutputCapsule() {
    byte[] txId = ByteArray
        .fromHexString("2c0937534dd1b3832d05d865e8e6f2bf23218300b33a992740d45ccab7d4f519");
    long vout = 12345L;
    byte[] signature = ByteArray
        .fromHexString("ded9c2181fd7ea468a7a7b1475defe90bb0fc0ca8d0f2096b0617465cea6568c");
    byte[] pubkey = ByteArray
        .fromHexString("a0c9d5524c055381fe8b1950e0c3b09d252add57a7aec061ae258aa03ee25822");

    TxInputCapsule txInputCapsule = new TxInputCapsule(txId, vout, signature, pubkey);

    Assert
        .assertArrayEquals(txId, txInputCapsule.getTxInput().getRawData().getTxID().toByteArray());
    Assert.assertEquals(vout, txInputCapsule.getTxInput().getRawData().getVout());
    Assert.assertArrayEquals(signature, txInputCapsule.getTxInput().getSignature().toByteArray());
    Assert.assertArrayEquals(pubkey,
        txInputCapsule.getTxInput().getRawData().getPubKey().toByteArray());
    Assert.assertTrue(txInputCapsule.validate());
  }
}

