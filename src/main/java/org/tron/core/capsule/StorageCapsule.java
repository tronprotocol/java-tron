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
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.StorageItem;


/**
 * @author Guo Yonggang
 * @since 28.04.2018
 */
@Slf4j
public class StorageCapsule implements ProtoCapsule<StorageItem> {

  private StorageItem storage;

  public StorageCapsule(byte[] code) {
    try {
      this.storage = StorageItem.parseFrom(code);
    } catch (InvalidProtocolBufferException e) {
      //
    }
  }

  public StorageCapsule(StorageItem cache) {
    this.storage = cache;
  }

  public Sha256Hash getHash() {
    byte[] storageBytes = this.storage.toByteArray();
    return Sha256Hash.of(storageBytes);
  }

  public Sha256Hash getRawHash() {
    return Sha256Hash.of(this.storage.toByteArray());
  }

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }

  public DataWord get(DataWord key) {
    if (!this.storage.containsItems(key.toUTF8String())) {
      return null;
    }

    DataWord value = new DataWord(this.storage.getItemsMap().get(key.toUTF8String()).toByteArray());
    return value;
  }

  public void put(DataWord key, DataWord value) {
    this.storage = this.storage.toBuilder().
            putItems(key.toUTF8String(), ByteString.copyFrom(value.getData())).build();
  }

  @Override
  public byte[] getData() {
    return this.storage.toByteArray();
  }

  @Override
  public StorageItem getInstance() {
    return this.storage;
  }

  @Override
  public String toString() {
    return this.storage.toString();
  }
}
