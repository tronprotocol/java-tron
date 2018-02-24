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

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.utils.TransactionUtil;
import org.tron.protos.Protocal.Transaction;

public class TransactionCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testNewCoinbaseTransaction() {
    Transaction coinbaseTransaction = TransactionUtil
        .newCoinbaseTransaction("12", "", 0);

    logger.info("test new coinbase transaction: {}", coinbaseTransaction);
  }

  @Test
  public void testGetHash() {
    Transaction coinbaseTransaction = TransactionUtil
        .newCoinbaseTransaction("12", "", 0);

    logger.info("test getData hash: {}",
        ByteArray.toHexString(TransactionUtil.getHash(coinbaseTransaction)));
  }

  @Test
  public void testToPrintString() {
    Transaction coinbaseTransaction = TransactionUtil
        .newCoinbaseTransaction("12", "", 0);

    logger.info("test to print string: {}", TransactionUtil
        .toPrintString(coinbaseTransaction));
  }

  @Test
  public void testIsCoinbaseTransaction() {
    Transaction coinbaseTransaction = TransactionUtil
        .newCoinbaseTransaction("12", "", 0);

    logger.info("test is coinbase transaction: {}", TransactionUtil
        .isCoinbaseTransaction(coinbaseTransaction));
  }

  @Test
  public void testParseTransaction() {
    String transactionData =
        "12650a202dbb0466bb1bc2f4b1432e62307160084c14eeab2b093f11969db06c07f3012f2241041701702"
            + "2a990673f2291d73a45621dc4bc754e3313f5a9cea1421b9ea0133d92a3a029c1be7d947b709195"
            + "ea02370e05712cea4a699edb6efe8fedfe18eb4fcb1a1808011214fd0f3c8ab4877f0fd96cd156b"
            + "0ad42ea7aa82c311a1808091214fd0f3c8ab4877f0fd96cd156b0ad42ea7aa82c31";
    try {
      Transaction transaction = Transaction.parseFrom(ByteArray.fromHexString(transactionData));
      System.out.println();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }
}
