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

package org.tron.trie;

import static org.tron.crypto.Hash.sha3;
import static org.tron.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import org.tron.storage.SourceInter;

public class SecureTrie extends TrieImpl {

  public SecureTrie(byte[] root) {
    super(root);
  }

  public SecureTrie(SourceInter<byte[], byte[]> cache) {
    super(cache, null);
  }

  public SecureTrie(SourceInter<byte[], byte[]> cache, byte[] root) {
    super(cache, root);
  }

  @Override
  public byte[] getData(byte[] key) {
    return super.getData(sha3(key));
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    super.putData(sha3(key), value);
  }

  @Override
  public void deleteData(byte[] key) {
    putData(key, EMPTY_BYTE_ARRAY);
  }
}
