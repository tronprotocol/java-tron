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

package org.tron.core.capsule.utils;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocal.TXOutput;

public class TxOutputUtilTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testNewTxOutput() {
    long value = 123456L;
    String address = "1921681012";
    logger.info("newTxOutput(value, address)={}", TxOutputUtil.newTxOutput(value, address));
    TXOutput txOutput = TxOutputUtil.newTxOutput(value, address);
    logger.info("value={}", txOutput.getValue());
    logger.info("address={}", ByteArray.toHexString(txOutput.getPubKeyHash().toByteArray()));
    Assert.assertEquals(value, txOutput.getValue());
    Assert.assertEquals(address, ByteArray.toHexString(txOutput.getPubKeyHash().toByteArray()));

    long value1 = 98765L;
    String address1 = "192168101";
    String address2 = "0" + address1;
    logger.info("newTxOutput(value1, address1)={}", TxOutputUtil.newTxOutput(value1, address1));
    TXOutput txOutput1 = TxOutputUtil.newTxOutput(value1, address1);
    logger.info("value={}", txOutput1.getValue());
    logger.info("address={}", ByteArray.toHexString(txOutput1.getPubKeyHash().toByteArray()));
    Assert.assertEquals(value1, txOutput1.getValue());
    Assert.assertEquals(address2,
        ByteArray.toHexString(txOutput1.getPubKeyHash().toByteArray()));

    long value3 = 9852448L;
    String address3 = "0x1921681011";
    String address4 = "1921681011";
    logger.info("TxOutputUtil.newTxOutput(value3, address3)={}",
        TxOutputUtil.newTxOutput(value3, address3));
    TXOutput txOutput3 = TxOutputUtil.newTxOutput(value3, address3);
    logger.info("value={}", txOutput3.getValue());
    logger.info("address={}", ByteArray.toHexString(txOutput3.getPubKeyHash().toByteArray()));
    Assert.assertEquals(value3, txOutput3.getValue());
    Assert.assertEquals(address4, ByteArray.toHexString(txOutput3.getPubKeyHash().toByteArray()));

    long value5 = 67549L;
    String address5 = null;
    logger.info("TxOutputUtil.newTxOutput(value5, address5)={}",
        TxOutputUtil.newTxOutput(value5, address5));
    TXOutput txOutput5 = TxOutputUtil.newTxOutput(value5, address5);
    logger.info("value={}", txOutput5.getValue());
    logger.info("address={}", ByteArray.toHexString(txOutput5.getPubKeyHash().toByteArray()));
    Assert.assertEquals(value5, txOutput5.getValue());
    Assert.assertEquals("", ByteArray.toHexString(txOutput5.getPubKeyHash().toByteArray()));

  }

}
