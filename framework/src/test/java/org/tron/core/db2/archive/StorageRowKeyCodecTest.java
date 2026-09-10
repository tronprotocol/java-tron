package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.store.StorageRowKeyCodec;
import org.tron.core.vm.program.Storage;

public class StorageRowKeyCodecTest {

  private static final byte[] ADDRESS = Hex.decode(
      "410102030405060708090a0b0c0d0e0f1011121314");
  private static final byte[] SLOT = Hex.decode(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
  private static final byte[] TRANSACTION_HASH = Hex.decode(
      "f0e0d0c0b0a090807060504030201000112233445566778899aabbccddeeff00");

  @Test
  public void matchesLegacyNormalVersionAndCreate2GoldenVectors() {
    assertArrayEquals(Hex.decode(
            "20ca5ae32eacb5480afc3d6566816bdb101112131415161718191a1b1c1d1e1f"),
        StorageRowKeyCodec.physicalKey(ADDRESS, SLOT, 0, null));
    assertArrayEquals(Hex.decode(
            "20ca5ae32eacb5480afc3d6566816bdbdea5e526567e92b0321816a4e895bd2d"),
        StorageRowKeyCodec.physicalKey(ADDRESS, SLOT, 1, null));
    assertArrayEquals(Hex.decode(
            "9397a7a785754542ff19d0968c0f92d4101112131415161718191a1b1c1d1e1f"),
        StorageRowKeyCodec.physicalKey(ADDRESS, SLOT, 0, TRANSACTION_HASH));
    assertArrayEquals(Hex.decode(
            "89ab580a96974c01d754858e702bc237101112131415161718191a1b1c1d1e1f"),
        StorageRowKeyCodec.physicalKey(ADDRESS, SLOT, 0, new byte[32]));
  }

  @Test
  public void latestVmStorageUsesTheSharedCodec() {
    Storage storage = new Storage(ADDRESS, null, key -> null);
    storage.setContractVersion(1);
    storage.generateAddrHash(TRANSACTION_HASH);
    DataWord slot = new DataWord(SLOT);
    storage.put(slot, new DataWord(1));

    StorageRowCapsule row = storage.getRowCache().get(slot);
    assertArrayEquals(StorageRowKeyCodec.physicalKey(ADDRESS, SLOT, 1, TRANSACTION_HASH),
        row.getRowKey());
  }

  @Test
  public void copiesOutputsAndRejectsInvalidComponentLengths() {
    byte[] address = Arrays.copyOf(ADDRESS, ADDRESS.length);
    byte[] slot = Arrays.copyOf(SLOT, SLOT.length);
    byte[] transactionHash = Arrays.copyOf(TRANSACTION_HASH, TRANSACTION_HASH.length);
    byte[] first = StorageRowKeyCodec.physicalKey(address, slot, 1, transactionHash);
    first[0] ^= 1;

    assertArrayEquals(ADDRESS, address);
    assertArrayEquals(SLOT, slot);
    assertArrayEquals(TRANSACTION_HASH, transactionHash);
    assertArrayEquals(Hex.decode(
            "9397a7a785754542ff19d0968c0f92d4dea5e526567e92b0321816a4e895bd2d"),
        StorageRowKeyCodec.physicalKey(address, slot, 1, transactionHash));
    assertEquals(StorageRowKeyCodec.KEY_BYTES,
        StorageRowKeyCodec.physicalKey(address, slot, 0, null).length);
    assertThrows(IllegalArgumentException.class,
        () -> StorageRowKeyCodec.physicalKey(address, new byte[31], 0, null));
    assertThrows(IllegalArgumentException.class,
        () -> StorageRowKeyCodec.physicalKeyFromAddressHash(new byte[31], slot, 0));
  }
}
