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


package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.SpendableOutputs;
import org.tron.protos.Protocol.TXOutput;
import org.tron.protos.Protocol.TXOutputs;


public class UtxoStore extends TronDatabase {

  public static final Logger logger = LoggerFactory.getLogger("UTXOStore");

  private UtxoStore(String dbName) {
    super(dbName);
  }


  private static UtxoStore instance;

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static UtxoStore create(String dbName) {
    if (instance == null) {
      synchronized (UtxoStore.class) {
        if (instance == null) {
          instance = new UtxoStore(dbName);
        }
      }
    }
    return instance;
  }


  public void reSet() {
    this.dbSource.resetDb();
  }

  public byte[] find(byte[] key) {
    return dbSource.getData(key);
  }


  public Set<byte[]> getKeys() {
    return dbSource.allKeys();
  }

  /**
   * save  utxo.
   */
  public void saveUtxo(byte[] utxoKey, byte[] utxoData) {
    dbSource.putData(utxoKey, utxoData);
  }

  /**
   * Find spendable outputs.
   */
  public SpendableOutputs findSpendableOutputs(byte[] pubKeyHash, long amount) {
    SpendableOutputs spendableOutputs = new SpendableOutputs();
    HashMap<String, long[]> unspentOutputs = new HashMap<>();
    long accumulated = 0L;

    Set<byte[]> keySet = getDbSource().allKeys();

    for (byte[] key : keySet) {
      byte[] txOutputsData = getDbSource().getData(key);
      try {
        TXOutputs txOutputs = TXOutputs.parseFrom(txOutputsData);

        int len = txOutputs.getOutputsCount();

        for (int i = 0; i < len; i++) {
          TXOutput txOutput = txOutputs.getOutputs(i);
          if (ByteArray.toHexString(ECKey.computeAddress(pubKeyHash))
              .equals(ByteArray.toHexString(txOutput.getPubKeyHash().toByteArray()))
              && accumulated < amount) {
            accumulated += txOutput.getValue();

            long[] v = unspentOutputs.get(ByteArray.toHexString(key));

            if (v == null) {
              v = new long[0];
            }

            unspentOutputs.put(ByteArray.toHexString(key), ArrayUtils.add(v, i));
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

  /**
   * Find related UTXOs.
   */
  public ArrayList<TXOutput> findUtxo(byte[] address) {
    ArrayList<TXOutput> utxos = new ArrayList<>();

    Set<byte[]> keySet = getDbSource().allKeys();

    for (byte[] key : keySet) {
      byte[] txData = getDbSource().getData(key);
      try {
        TXOutputs txOutputs = TXOutputs.parseFrom(txData);
        for (TXOutput txOutput : txOutputs.getOutputsList()) {
          if (ByteArray.toHexString(ECKey.computeAddress(address))
              .equals(ByteArray.toHexString(txOutput.getPubKeyHash().toByteArray()))) {
            utxos.add(txOutput);
          }
        }
      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
      }
    }

    return utxos;
  }

  public void close() {
    dbSource.closeDB();
  }

  @Override
  public void put(byte[] key, Object item) {

  }

  @Override
  public void delete(byte[] key) {

  }

  @Override
  public Object get(byte[] key) {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }
}
