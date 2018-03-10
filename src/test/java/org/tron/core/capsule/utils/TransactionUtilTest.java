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

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.TXInput;
import org.tron.protos.Protocol.TXInput.raw;
import org.tron.protos.Protocol.TXOutput;
import org.tron.protos.Protocol.Transaction;

public class TransactionUtilTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  public TransactionUtil transactionUtil;

  String pubKey = "328ea6d24659dec48adea1aced9a136e5ebdf40258db30d1b1d97ed2b74be34e";
  int value = 100;

  Transaction.Builder trx = Transaction.newBuilder().setRawData(
      Transaction.raw.newBuilder().addVout(TXOutput.newBuilder().setValue(value))
          .addVin(TXInput.newBuilder().setRawData(
              raw.newBuilder().setPubKey(ByteString.copyFrom(ByteArray.fromHexString(pubKey)))))
          .build());

  /*
   * unit test for correct parameters
   */
  @Test
  public void testNewGenesisTransaction() {
    Transaction transaction = transactionUtil.newGenesisTransaction(pubKey, value);

    Assert.assertEquals(trx.build().getRawData().getVout(0).getValue(),
        transaction.getRawData().getVout(0).getValue());
  }

  @Test
  public void testGetSender() {
    byte[] pubKey = ByteArray
        .fromHexString("328ea6d24659dec48adea1aced9a136e5ebdf40258db30d1b1d97ed2b74be34e");

    Transaction transaction = trx.build();
    byte[] bytes = TransactionUtil.getSender(transaction);

    byte[] expectBytes = ECKey.computeAddress(pubKey);

    Assert.assertEquals("is not Expect", ByteArray.toHexString(expectBytes),
        ByteArray.toHexString(bytes));
  }

}
