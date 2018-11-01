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

package org.tron.core.capsule;

import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Sha256Hash;


@Slf4j
public class StorageRowCapsule implements ProtoCapsule<byte[]> {
  @Getter
  private byte[] rowValue;
  @Setter
  @Getter
  private byte[] rowKey;

  @Getter
  private boolean dirty = false;

  public StorageRowCapsule(StorageRowCapsule rowCapsule) {
    this.rowKey = rowCapsule.getRowKey().clone();
    this.rowValue = rowCapsule.getRowValue().clone();
    this.dirty = rowCapsule.isDirty();
  }

  public StorageRowCapsule(byte[] rowKey, byte[] rowValue) {
    this.rowKey = rowKey;
    this.rowValue = rowValue;
    markDirty();
  }

  public StorageRowCapsule(byte[] rowValue) {
    this.rowValue = rowValue;
  }

  private void markDirty() {
    dirty = true;
  }

  public Sha256Hash getHash() {
    return Sha256Hash.of(this.rowValue);
  }

  public DataWord getValue() {
    return new DataWord(this.rowValue);
  }

  public void setValue(DataWord value) {
    this.rowValue = value.getData();
    markDirty();
  }

  @Override
  public byte[] getData() {
    return this.rowValue;
  }

  @Override
  public byte[] getInstance() {
    return this.rowValue;
  }

  @Override
  public String toString() {
    return Arrays.toString(rowValue);
  }
}
