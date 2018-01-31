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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocal.TXOutput;
import org.tron.protos.Protocal.TXOutputs;


public class UTXOSet {

  private static final Logger logger = LoggerFactory.getLogger("UTXOSet");

  private Blockchain blockchain;
  private LevelDbDataSourceImpl txDB;

  @Inject
  public UTXOSet(@Named("transaction") LevelDbDataSourceImpl txDb, Blockchain blockchain) {
    this.txDB = txDb;
    this.blockchain = blockchain;
  }

  public Blockchain getBlockchain() {
    return blockchain;
  }


  public void reindex() {
    logger.info("reindex");

    txDB.resetDB();

    HashMap<String, TXOutputs> utxo = blockchain.findUtxo();

    Set<Map.Entry<String, TXOutputs>> entrySet = utxo.entrySet();

    for (Map.Entry<String, TXOutputs> entry : entrySet) {
      String key = entry.getKey();
      TXOutputs value = entry.getValue();

      for (TXOutput ignored : value.getOutputsList()) {
        txDB.putData(ByteArray.fromHexString(key), value.toByteArray());
      }
    }
  }

  public SpendableOutputs findSpendableOutputs(byte[] pubKeyHash, long amount) {
    SpendableOutputs spendableOutputs = new SpendableOutputs();
    HashMap<String, long[]> unspentOutputs = new HashMap<>();
    long accumulated = 0L;

    Set<byte[]> keySet = txDB.allKeys();

    for (byte[] key : keySet) {
      byte[] txOutputsData = txDB.getData(key);
      try {
        TXOutputs txOutputs = TXOutputs.parseFrom(txOutputsData);

        int len = txOutputs.getOutputsCount();

        for (int i = 0; i < len; i++) {
          TXOutput txOutput = txOutputs.getOutputs(i);
          if (ByteArray.toHexString(ECKey.computeAddress(pubKeyHash))
              .equals(ByteArray.toHexString(txOutput
                  .getPubKeyHash()
                  .toByteArray())) && accumulated < amount) {
            accumulated += txOutput.getValue();

            long[] v = unspentOutputs.get(ByteArray.toHexString(key));

            if (v == null) {
              v = new long[0];
            }

            long[] tmp = Arrays.copyOf(v, v.length + 1);
            tmp[tmp.length - 1] = i;

            unspentOutputs.put(ByteArray.toHexString(key), tmp);
          }
        }
      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
      }
    }

    spendableOutputs.setAmount(accumulated);
    spendableOutputs.setUnspentOutputs(unspentOutputs);

    return spendableOutputs;
  }

  public ArrayList<TXOutput> findUTXO(byte[] pubKeyHash) {
    ArrayList<TXOutput> utxos = new ArrayList<>();

    Set<byte[]> keySet = txDB.allKeys();
    for (byte[] key : keySet) {
      byte[] txData = txDB.getData(key);

      try {
        TXOutputs txOutputs = TXOutputs.parseFrom(txData);
        for (TXOutput txOutput : txOutputs.getOutputsList()) {
          if (ByteArray.toHexString(ECKey.computeAddress(pubKeyHash))
              .equals(ByteArray.toHexString(txOutput
                  .getPubKeyHash()
                  .toByteArray()))) {
            utxos.add(txOutput);
          }
        }
      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
      }
    }

    return utxos;
  }
}
