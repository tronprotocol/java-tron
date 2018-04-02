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

import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.TXOutput;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class TransactionUtil {

  public static Transaction newGenesisTransaction(String key, long value) {
    return new TransactionCapsule(key, value).getInstance();
  }

  /**
   * Determine whether the transaction is a coin-base transaction.
   *
   * @param transaction {@link Transaction} transaction.
   * @return boolean true for coinbase, false for not coinbase.
   */
  /*public static boolean isCoinbaseTransaction(Transaction transaction) {
    Transaction.raw rawData = transaction.getRawData();
    return rawData.getVinList().size() == 1
        && rawData.getVin(0).getRawData().getTxID().size() == 0
        && rawData.getVin(0).getRawData().getVout() == -1;
  }*/
  private static boolean checkTxOutUnSpent(TXOutput prevOut) {
    return true;//todo :check prevOut is unspent
  }

  /**
   * checkBalance.
   */
  private static boolean checkBalance(long totalBalance, long totalSpent) {
    return totalBalance == totalSpent;
  }

  /**
   * Get sender.
   */
 /* public static byte[] getSender(Transaction tx) {
    byte[] pubKey = tx.getRawData().getVin(0).getRawData().getPubKey().toByteArray();
    return ECKey.computeAddress(pubKey);
  } */

}
