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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.SpendableOutputs;
import org.tron.protos.Protocol.TXOutput;
import org.tron.protos.Protocol.TXOutputs;

@Slf4j
public class UtxoStore extends TronDatabase {
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

    for (byte[] key : getDbSource().allKeys()) {
      try {
        TXOutputs txOutputs = TXOutputs.parseFrom(getDbSource().getData(key));
        String keyToHexString = ByteArray.toHexString(key);

        for (int i = 0, len = txOutputs.getOutputsCount(); i < len; i++) {
          TXOutput txOutput = txOutputs.getOutputs(i);
          if (ByteArray.toHexString(ECKey.computeAddress(pubKeyHash))
                  .equals(ByteArray.toHexString(txOutput.getPubKeyHash().toByteArray()))
                  && accumulated < amount) {

            accumulated += txOutput.getValue();
            long[] v = ArrayUtils.nullToEmpty(unspentOutputs.get(keyToHexString));
            unspentOutputs.put(keyToHexString, ArrayUtils.add(v, i));
          }
        }
      } catch (InvalidProtocolBufferException e) {
        logger.debug(e.getMessage(), e);
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
    return getDbSource().allKeys().stream()
            .map(key -> {
              try {
                return TXOutputs.parseFrom(getDbSource().getData(key));
              } catch (InvalidProtocolBufferException e) {
                logger.debug(e.getMessage(), e);
                return null;
              }
            })
            .filter(Objects::nonNull)
            .map(TXOutputs::getOutputsList)
            .flatMap(List::stream)
            .filter(txOutput -> ByteArray.toHexString(ECKey.computeAddress(address))
                    .equals(ByteArray.toHexString(txOutput.getPubKeyHash().toByteArray())))
            .collect(Collectors.toCollection(ArrayList::new));
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