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
import org.tron.core.capsule.TxOutputCapsule;

@Slf4j
public class TxOutputCapsuleTest {
  @Test
  public void testTxOutputCapsule() {
    long value = 123456L;
    String address = "3450dde5007c67a50ec2e09489fa53ec1ff59c61e7ddea9638645e6e5f62e5f5";
    TxOutputCapsule txOutputCapsule = new TxOutputCapsule(value, address);

    Assert.assertEquals(value, txOutputCapsule.getTxOutput().getValue());
    Assert.assertEquals(address,
        ByteArray.toHexString(txOutputCapsule.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule.validate());

    long value3 = 9852448L;
    String address3 = "0xfd1a5decba973b0d31e84e7d8f4a5b10d33ab37ce6533f1ff5a9db2d9db8ef";
    String address4 = "fd1a5decba973b0d31e84e7d8f4a5b10d33ab37ce6533f1ff5a9db2d9db8ef";
    TxOutputCapsule txOutputCapsule2 = new TxOutputCapsule(value3, address3);

    Assert.assertEquals(value3, txOutputCapsule2.getTxOutput().getValue());
    Assert.assertEquals(address4,
        ByteArray.toHexString(txOutputCapsule2.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule2.validate());

    long value5 = 67549L;
    String address5 = null;
    TxOutputCapsule txOutputCapsule3 = new TxOutputCapsule(value5, address5);

    Assert.assertEquals(value5, txOutputCapsule3.getTxOutput().getValue());
    Assert.assertEquals("",
        ByteArray.toHexString(txOutputCapsule3.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputCapsule3.validate());

  }

}

