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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.StorageRow;


@Slf4j
public class StorageRowCapsule implements ProtoCapsule<StorageRow> {

  private StorageRow instance;

  @Getter
  private boolean dirty = false;

  private void markDirty() {
    dirty = true;
  }

  private StorageRowCapsule() {
    instance = StorageRow.newBuilder().build();
  }

  public StorageRowCapsule(byte[] key, byte[] value) {
    instance = StorageRow.newBuilder().setKey(ByteString.copyFrom(key))
        .setValue(ByteString.copyFrom(value)).build();
    markDirty();
  }

  public StorageRowCapsule(byte[] code) {
    try {
      this.instance = StorageRow.parseFrom(code);
    } catch (InvalidProtocolBufferException e) {
    }
  }

  public StorageRowCapsule(StorageRow cache) {
    this.instance = cache;
  }

  public Sha256Hash getHash() {
    byte[] storageBytes = this.instance.toByteArray();
    return Sha256Hash.of(storageBytes);
  }


  public DataWord getValue() {
    return new DataWord(this.instance.getValue().toByteArray());
  }

  public byte[] getKey() {
    return this.instance.getKey().toByteArray();
  }

  public void setValue(DataWord value) {
    this.instance = this.instance.toBuilder().setValue(ByteString.copyFrom(value.getData()))
        .build();
    markDirty();
  }

  @Override
  public byte[] getData() {
    return this.instance.toByteArray();
  }

  @Override
  public StorageRow getInstance() {
    return this.instance;
  }

  @Override
  public String toString() {
    return this.instance.toString();
  }
}
