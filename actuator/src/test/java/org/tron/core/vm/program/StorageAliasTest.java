package org.tron.core.vm.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ForkController;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StorageRowStore;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program.OutOfTimeException;

public class StorageAliasTest {

  private ForkController controller;
  private MockedStatic<ForkController> fork;
  private StorageRowStore store;
  private Storage storage;
  private final DataWord slot = new DataWord(1);
  private final DataWord alias = aliasedSlot(1);

  @Before
  public void setUp() {
    controller = mock(ForkController.class);
    fork = mockStatic(ForkController.class);
    fork.when(ForkController::instance).thenReturn(controller);
    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_3)).thenReturn(true);
    VMConfig.setLocalSnapshot(new VMConfig.Snapshot());
    store = mock(StorageRowStore.class);
    storage = createStorage(0, 0);
  }

  @After
  public void tearDown() {
    VMConfig.clearLocalSnapshot();
    fork.close();
  }

  @Test
  public void repeatedWritesIncludingZeroDoNotAlias() {
    storage.put(slot, new DataWord(5));
    storage.put(slot.clone(), DataWord.ZERO());
    storage.put(slot.clone(), new DataWord(7));
    assertEquals(new DataWord(7), storage.getValue(slot));
    assertThrows(OutOfTimeException.class, () -> storage.put(alias, new DataWord(8)));
  }

  @Test
  public void readOfAliasThrowsAndSameSlotReadWriteStillWorks() {
    when(store.get(argThat(key -> key != null && key.length == 32))).thenAnswer(
        invocation -> new StorageRowCapsule(new DataWord(5).getData()));
    assertEquals(new DataWord(5), storage.getValue(slot));
    assertEquals(new DataWord(5), storage.getValue(slot.clone()));
    assertThrows(OutOfTimeException.class, () -> storage.getValue(alias));
    storage.put(slot, DataWord.ZERO());
    assertEquals(DataWord.ZERO(), storage.getValue(slot));
    assertThrows(OutOfTimeException.class, () -> storage.put(alias, new DataWord(8)));
  }

  @Test
  public void readOfAliasAfterWriteThrows() {
    when(store.get(argThat(key -> key != null && key.length == 32))).thenAnswer(
        invocation -> new StorageRowCapsule(new DataWord(5).getData()));
    storage.put(slot, new DataWord(9));
    assertThrows(OutOfTimeException.class, () -> storage.getValue(alias));
    assertEquals(new DataWord(9), storage.getValue(slot));
  }

  @Test
  public void aliasedReadsRemainAllowedBeforeFork() {
    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_3)).thenReturn(false);
    Storage before = createStorage(0, 0);
    when(store.get(any(byte[].class))).thenAnswer(
        invocation -> new StorageRowCapsule(new DataWord(5).getData()));
    assertEquals(new DataWord(5), before.getValue(slot));
    assertEquals(new DataWord(5), before.getValue(alias));
    before.put(alias, new DataWord(8));
    assertEquals(new DataWord(8), before.getValue(alias));
  }

  @Test
  public void forkFlagIsCapturedOnceAtConstruction() {
    clearInvocations(controller);
    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_3)).thenReturn(false);
    Storage before = createStorage(0, 0);
    before.put(slot, new DataWord(5));

    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_3)).thenReturn(true);
    before.put(alias, new DataWord(8));
    assertEquals(new DataWord(8), before.getValue(alias));
    Storage child = new Storage(before);
    child.put(slot, new DataWord(1));
    child.put(alias, new DataWord(2));
    verify(controller, times(1)).pass(ForkBlockVersionEnum.VERSION_4_8_2_3);
    verify(controller, never()).pass(ForkBlockVersionEnum.VERSION_4_8_2_4);

    Storage after = createStorage(0, 0);
    after.put(alias, new DataWord(8));
    assertThrows(OutOfTimeException.class, () -> after.put(slot, new DataWord(7)));
    verify(controller, times(2)).pass(ForkBlockVersionEnum.VERSION_4_8_2_3);
    verify(controller, never()).pass(ForkBlockVersionEnum.VERSION_4_8_2_4);

    clearInvocations(controller);
    Storage optimized = optimizedStorage();
    optimized.put(slot, new DataWord(1));
    optimized.put(alias, new DataWord(2));
    assertEquals(new DataWord(2), optimized.getValue(alias));
    new Storage(optimized).put(slot, new DataWord(3));
    verify(controller, times(1)).pass(ForkBlockVersionEnum.VERSION_4_8_2_3);
    verify(controller, never()).pass(ForkBlockVersionEnum.VERSION_4_8_2_4);
  }

  @Test
  public void aliasedWritesRemainAllowedBeforeFork() {
    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_3)).thenReturn(false);
    Storage before = createStorage(0, 0);
    before.put(slot, new DataWord(5));
    before.put(alias, new DataWord(8));
    assertEquals(new DataWord(5), before.getValue(slot));
    assertEquals(new DataWord(8), before.getValue(alias));
  }

  @Test
  public void copyInheritsWrittenRowsAndAllowsRepeatedWrites() {
    storage.put(slot, new DataWord(5));
    Storage child = new Storage(storage);
    assertThrows(OutOfTimeException.class, () -> child.put(alias, new DataWord(8)));
    child.put(slot.clone(), new DataWord(7));
    assertEquals(new DataWord(7), child.getValue(slot));
    assertEquals(new DataWord(5), storage.getValue(slot));
  }

  @Test
  public void discardedChildDoesNotLeakWrittenRows() {
    Storage child = new Storage(storage);
    child.put(slot, new DataWord(5));
    storage.put(alias, new DataWord(8));
    assertEquals(new DataWord(8), storage.getValue(alias));
    assertEquals(new DataWord(5), child.getValue(slot));
  }

  @Test
  public void versionOneUsesHashedLegacyRows() {
    storage.setContractVersion(1);
    storage.put(slot, new DataWord(5));
    storage.put(alias, new DataWord(8));
    assertFalse(Arrays.equals(storage.getRowCache().get(slot).getRowKey(),
        storage.getRowCache().get(alias).getRowKey()));
    assertEquals(new DataWord(5), storage.getValue(slot));
    assertEquals(new DataWord(8), storage.getValue(alias));
  }

  @Test
  public void migratedNewKeysDoNotConflict() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(new DataWord(4).getData()));
    assertEquals(new DataWord(4), optimized.getValue(slot));
    assertEquals(new DataWord(4), optimized.getValue(alias));
    optimized.put(slot, new DataWord(5));
    optimized.put(alias, new DataWord(8));
    assertEquals(new DataWord(5), optimized.getValue(slot));
    assertEquals(new DataWord(8), optimized.getValue(alias));
  }

  @Test
  public void optimizedWritesDoNotTimeout() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    optimized.put(slot, new DataWord(5));
    optimized.put(alias, new DataWord(8));
    assertEquals(48, optimized.getRowCache().get(slot).getRowKey().length);
    assertEquals(48, optimized.getRowCache().get(alias).getRowKey().length);
    assertEquals(new DataWord(5), optimized.getValue(slot));
    assertEquals(new DataWord(8), optimized.getValue(alias));
    assertThrows(OutOfTimeException.class, () -> optimized.getValue(anotherAlias()));
  }

  @Test
  public void oldKeyIsReturnedOnlyToTheFirstCachedSlot() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    assertThrows(OutOfTimeException.class, () -> optimized.getValue(alias));
    assertEquals(new DataWord(7), optimized.getValue(slot.clone()));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void newKeyWinsOverACachedOldKeyCollision() {
    Storage optimized = optimizedStorage();
    boolean[] serveNewKey = {true};
    when(store.get(any(byte[].class))).thenAnswer(invocation -> {
      byte[] key = invocation.getArgument(0);
      if (serveNewKey[0] && key.length == 48) {
        return new StorageRowCapsule(new DataWord(4).getData());
      }
      if (key.length == 32) {
        return new StorageRowCapsule(new DataWord(7).getData());
      }
      return null;
    });
    assertEquals(new DataWord(4), optimized.getValue(slot));
    serveNewKey[0] = false;
    assertThrows(OutOfTimeException.class, () -> optimized.getValue(alias));
    serveNewKey[0] = true;
    assertEquals(new DataWord(4), optimized.getValue(alias));
  }

  @Test
  public void optimizedCommitDeletesLegacyKey() {
    Storage optimized = optimizedStorage();
    optimized.put(slot, new DataWord(5));
    optimized.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48), any());
    verify(store).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void readOfOldKeyDoesNotMigrateOnCommit() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void proposal99SecondReadIsEmptyAndPureReadMigrates() {
    Storage proposal99 = createStorage(1, 0);
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), proposal99.getValue(slot));
    assertNull(proposal99.getValue(alias));
    proposal99.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48),
        argThat(row -> new DataWord(row.getValue()).longValue() == 7L));
    verify(store).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void proposal99ChildCopyDoesNotTimeout() {
    Storage parent = createStorage(1, 0);
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), parent.getValue(slot));
    Storage child = new Storage(parent);
    assertNull(child.getValue(alias));
    child.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48), any());
    verify(store).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void readOfMissingKeyDoesNotWriteAndRepeatReadSkipsStore() {
    Storage optimized = optimizedStorage();
    assertNull(optimized.getValue(slot));
    assertNull(optimized.getValue(slot));
    optimized.commit();
    verify(store, times(2)).get(any());
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void readOfNewKeyDoesNotRewrite() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(new DataWord(4).getData()));
    assertEquals(new DataWord(4), optimized.getValue(slot));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void writeAfterReadKeepsWrittenValueAndDeletesOldKey() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    optimized.put(slot, new DataWord(9));
    optimized.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48),
        argThat(row -> new DataWord(row.getValue()).longValue() == 9L));
    verify(store).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void readThenWriteOfExistingNewKeyDoesNotDeleteOld() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(new DataWord(4).getData()));
    assertEquals(new DataWord(4), optimized.getValue(slot));
    optimized.put(slot, new DataWord(9));
    optimized.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48),
        argThat(row -> new DataWord(row.getValue()).longValue() == 9L));
    verify(store, never()).delete(any());
  }

  @Test
  public void writeThenCacheHitDeletesOldKey() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(new DataWord(4).getData()));
    optimized.put(slot, new DataWord(9));
    assertEquals(new DataWord(9), optimized.getValue(slot));
    optimized.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48), any());
    verify(store).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void storedZeroOnNewKeyReadsAsEmptyAndSkipsOldKey() {
    Storage optimized = optimizedStorage();
    when(store.get(any(byte[].class))).thenAnswer(invocation -> {
      byte[] key = invocation.getArgument(0);
      if (key.length == 48) {
        return new StorageRowCapsule(DataWord.ZERO().getData());
      }
      if (key.length == 32) {
        return new StorageRowCapsule(new DataWord(7).getData());
      }
      return null;
    });
    assertNull(optimized.getValue(slot));
    assertNull(optimized.getValue(slot));
    Storage child = new Storage(optimized);
    assertNull(child.getValue(slot));
    verify(store, times(1)).get(argThat(key -> key != null && key.length == 48));
    verify(store, never()).get(argThat(key -> key != null && key.length == 32));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void writeAfterStoredZeroHitsCacheAndDoesNotDeleteOldKey() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(DataWord.ZERO().getData()));
    assertNull(optimized.getValue(slot));
    optimized.put(slot, new DataWord(9));
    assertEquals(new DataWord(9), optimized.getValue(slot));
    optimized.commit();
    verify(store, times(1)).get(any());
    verify(store).put(argThat(key -> key != null && key.length == 48),
        argThat(row -> new DataWord(row.getValue()).longValue() == 9L));
    verify(store, never()).delete(any());
  }

  @Test
  public void putZeroOverStoredZeroStaysInCache() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(DataWord.ZERO().getData()));
    assertNull(optimized.getValue(slot));
    optimized.put(slot, DataWord.ZERO());
    assertEquals(DataWord.ZERO(), optimized.getValue(slot));
    assertEquals(DataWord.ZERO(), optimized.getValue(slot.clone()));
    verify(store, times(1)).get(any());
  }

  @Test
  public void collidingReadTimesOutAndDoesNotMigrate() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    OutOfTimeException ex = assertThrows(OutOfTimeException.class,
        () -> optimized.getValue(alias));
    assertEquals("CPU timeout for storage check", ex.getMessage());
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void pureReadDoesNotMigrateAndLegacyValueStays() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(alias));
    assertEquals(new DataWord(7), optimized.getValue(alias.clone()));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
    Storage later = optimizedStorage();
    assertEquals(new DataWord(7), later.getValue(slot));
  }

  @Test
  public void unrelatedWriteDoesNotMigrateAPureRead() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    optimized.put(new DataWord(2), new DataWord(3));
    optimized.commit();
    verify(store, times(1)).put(argThat(key -> key != null && key.length == 48), any());
    verify(store, times(1)).delete(argThat(key -> key != null && key.length == 32));
    Storage later = optimizedStorage();
    assertEquals(new DataWord(7), later.getValue(slot));
  }

  @Test
  public void writeAfterReadStillMigrates() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    optimized.put(slot, new DataWord(9));
    optimized.commit();
    verify(store).put(argThat(key -> key != null && key.length == 48),
        argThat(row -> new DataWord(row.getValue()).longValue() == 9L));
    verify(store).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void directPutDoesNotThrowAndStillDeletesOldKey() {
    Storage optimized = optimizedStorage();
    optimized.put(slot, new DataWord(5));
    optimized.put(alias, new DataWord(8));
    optimized.commit();
    verify(store, times(2)).put(argThat(key -> key != null && key.length == 48), any());
    verify(store, times(2)).delete(argThat(key -> key != null && key.length == 32));
  }

  @Test
  public void readAfterAliasPutTimesOutWhileLegacyRowExists() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    optimized.put(slot, new DataWord(5));
    OutOfTimeException ex = assertThrows(OutOfTimeException.class,
        () -> optimized.getValue(alias));
    assertEquals("CPU timeout for storage check", ex.getMessage());
  }

  @Test
  public void aliasReadOfAbsentLegacyRowStaysEmpty() {
    Storage optimized = optimizedStorage();
    optimized.put(slot, new DataWord(5));
    assertNull(optimized.getValue(alias));
  }

  @Test
  public void aliasReadAfterMigrationDoesNotThrow() {
    Storage optimized = optimizedStorage();
    int[] newKeyReads = {0};
    when(store.get(any(byte[].class))).thenAnswer(invocation -> {
      byte[] key = invocation.getArgument(0);
      if (key.length == 48 && newKeyReads[0]++ == 0) {
        return new StorageRowCapsule(new DataWord(4).getData());
      }
      return null;
    });
    assertEquals(new DataWord(4), optimized.getValue(slot));
    assertNull(optimized.getValue(alias));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void liveLegacyRowStillTimesOutAfterANewKeyHit() {
    Storage optimized = optimizedStorage();
    int[] newKeyReads = {0};
    when(store.get(any(byte[].class))).thenAnswer(invocation -> {
      byte[] key = invocation.getArgument(0);
      if (key.length == 48 && newKeyReads[0]++ == 0) {
        return new StorageRowCapsule(new DataWord(4).getData());
      }
      if (key.length == 32) {
        return new StorageRowCapsule(new DataWord(7).getData());
      }
      return null;
    });
    assertEquals(new DataWord(4), optimized.getValue(slot));
    assertThrows(OutOfTimeException.class, () -> optimized.getValue(alias));
  }

  @Test
  public void migratedNewKeysDoNotTimeout() {
    Storage optimized = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 48)))
        .thenReturn(new StorageRowCapsule(new DataWord(4).getData()));
    assertEquals(new DataWord(4), optimized.getValue(slot));
    assertEquals(new DataWord(4), optimized.getValue(alias));
    optimized.commit();
    verify(store, never()).delete(any());
  }

  @Test
  public void childCopyInheritsOwner() {
    Storage parent = optimizedStorage();
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), parent.getValue(slot));
    Storage child = new Storage(parent);
    assertThrows(OutOfTimeException.class, () -> child.getValue(alias));
    child.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  @Test
  public void versionOneAliasesDoNotTimeout() {
    Storage optimized = optimizedStorage();
    optimized.setContractVersion(1);
    when(store.get(argThat(key -> key != null && key.length == 32)))
        .thenReturn(new StorageRowCapsule(new DataWord(7).getData()));
    assertEquals(new DataWord(7), optimized.getValue(slot));
    assertEquals(new DataWord(7), optimized.getValue(alias));
    optimized.commit();
    verify(store, never()).put(any(), any());
    verify(store, never()).delete(any());
  }

  private Storage optimizedStorage() {
    return createStorage(0, 1);
  }

  private Storage createStorage(int proposal99, int proposal102) {
    DynamicPropertiesStore dynamicStore = mock(DynamicPropertiesStore.class);
    when(dynamicStore.allowTvmStorageOptimization()).thenReturn(proposal99 == 1 || proposal102 == 1);
    when(dynamicStore.allowOptimizeTvmStorageKey()).thenReturn(proposal102 == 1);
    return new Storage(new byte[21], store, dynamicStore);
  }

  private static DataWord aliasedSlot(int value) {
    byte[] bytes = new DataWord(value).getData().clone();
    bytes[15] = 1;
    return new DataWord(bytes);
  }

  private static DataWord anotherAlias() {
    byte[] bytes = new DataWord(1).getData().clone();
    bytes[14] = 1;
    return new DataWord(bytes);
  }
}
