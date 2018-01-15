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

package org.tron.storage;


public interface MemSizeEstimator<E> {

  /**
   * byte[] type size estimator
   */
  MemSizeEstimator<byte[]> ByteArrayEstimator = new MemSizeEstimator<byte[]>() {
    @Override
    public long estimateSize(byte[] bytes) {
      return bytes == null ? 0 : bytes.length + 4; // 4 - compressed ref size
    }
  };

  long estimateSize(E e);
}
