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

import java.util.HashMap;
import java.util.Map;

public class GenesisBlockLoader {
  private long timestamp;
  private byte[] txTrieRoot;
  private byte[] parentHash;
  private byte[] hash;
  private byte[] nonce;
  private byte[] difficulty;
  private long number;
  private Map<String, Integer> transaction = new HashMap<>();

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public byte[] getTxTrieRoot() {
    return txTrieRoot;
  }

  public void setTxTrieRoot(byte[] txTrieRoot) {
    this.txTrieRoot = txTrieRoot;
  }

  public byte[] getParentHash() {
    return parentHash;
  }

  public void setParentHash(byte[] parentHash) {
    this.parentHash = parentHash;
  }

  public byte[] getHash() {
    return hash;
  }

  public void setHash(byte[] hash) {
    this.hash = hash;
  }

  public byte[] getNonce() {
    return nonce;
  }

  public void setNonce(byte[] nonce) {
    this.nonce = nonce;
  }

  public byte[] getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(byte[] difficulty) {
    this.difficulty = difficulty;
  }

  public long getNumber() {
    return number;
  }

  public void setNumber(long number) {
    this.number = number;
  }

  public Map<String, Integer> getTransaction() {
    return transaction;
  }

  public void setTransaction(Map<String, Integer> transaction) {
    this.transaction = transaction;
  }
}
