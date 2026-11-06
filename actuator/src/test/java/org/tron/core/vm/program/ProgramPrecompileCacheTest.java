package org.tron.core.vm.program;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.apache.commons.collections4.map.LRUMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteUtil;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Repository;

public class ProgramPrecompileCacheTest {

  private static final byte[] CODE = {0x5b, 0x60, 0x5b, 0x5b, 0};

  private LRUMap<Key, ProgramPrecompile> cache;
  private ProgramInvoke invoke;
  private InternalTransaction internalTransaction;
  private boolean hashChangesAcrossModes;

  @Before
  public void setUp() throws ReflectiveOperationException {
    cache = sharedCache();
    cache.clear();
    assertTrue("The fixture needs room for both hash modes", cache.maxSize() >= 2);
    invoke = mock(ProgramInvoke.class);
    when(invoke.getContractAddress()).thenReturn(new DataWord(address(0)));
    when(invoke.getDeposit()).thenReturn(mock(Repository.class));
    internalTransaction = mock(InternalTransaction.class);

    setResourceOptimization(false);
    int legacyHash = cacheKey(0).hashCode();
    setResourceOptimization(true);
    hashChangesAcrossModes = legacyHash != cacheKey(0).hashCode();
    setResourceOptimization(false);
  }

  @After
  public void tearDown() {
    if (cache != null) {
      cache.clear();
    }
    VMConfig.clearLocalSnapshot();
  }

  @Test
  public void activationKeepsAnalysisCorrectAndBoundsDuplicateKeys() {
    ProgramPrecompile legacy = program(0).getProgramPrecompile();
    assertSame(legacy, program(0).getProgramPrecompile());
    assertEquals(1, cache.size());

    setResourceOptimization(true);
    ProgramPrecompile optimized = program(0).getProgramPrecompile();

    assertSame(optimized, program(0).getProgramPrecompile());
    assertFixtureEntries(legacy, optimized);
  }

  @Test
  public void rollbackAndReactivationReuseTheirExistingAnalyses() {
    ProgramPrecompile legacy = program(0).getProgramPrecompile();
    setResourceOptimization(true);
    ProgramPrecompile optimized = program(0).getProgramPrecompile();

    for (int round = 0; round < 3; round++) {
      setResourceOptimization(false);
      assertSame(legacy, program(0).getProgramPrecompile());
      setResourceOptimization(true);
      assertSame(optimized, program(0).getProgramPrecompile());
      assertFixtureEntries(legacy, optimized);
    }
  }

  @Test
  public void evictionRecompilesAfterRollbackAndPreservesRecentEntries() {
    ProgramPrecompile legacy = program(0).getProgramPrecompile();
    setResourceOptimization(true);
    program(0).getProgramPrecompile();
    ProgramPrecompile mostRecent = null;
    for (int value = 1; value <= cache.maxSize(); value++) {
      mostRecent = program(value).getProgramPrecompile();
      assertTrue(cache.size() <= cache.maxSize());
    }
    assertEquals(cache.maxSize(), cache.size());
    assertFalse(cache.containsKey(cacheKey(0)));
    setResourceOptimization(false);
    assertFalse(cache.containsKey(cacheKey(0)));

    ProgramPrecompile recompiled = program(0).getProgramPrecompile();

    assertNotSame(legacy, recompiled);
    assertAnalysis(recompiled);
    assertSame(recompiled, program(0).getProgramPrecompile());
    assertEquals(cache.maxSize(), cache.size());
    setResourceOptimization(true);
    assertSame(mostRecent, program(cache.maxSize()).getProgramPrecompile());
    assertEquals(cache.maxSize(), cache.size());
  }

  @Test
  public void alternatingModesKeepTheSharedCacheWithinCapacity() {
    for (boolean enabled : new boolean[]{false, true, false, true, false}) {
      setResourceOptimization(enabled);
      // Each wave exceeds capacity, including entries whose stored hashes use the other mode.
      for (int value = 0; value <= cache.maxSize(); value++) {
        ProgramPrecompile compiled = program(value).getProgramPrecompile();
        assertAnalysis(compiled);
        assertSame(compiled, program(value).getProgramPrecompile());
        assertTrue(cache.size() <= cache.maxSize());
      }
      assertEquals(cache.maxSize(), cache.size());
    }
  }

  @Test
  public void constantCallsBypassTheSharedCacheAcrossModes() {
    ProgramPrecompile legacy = program(0).getProgramPrecompile();
    when(invoke.isConstantCall()).thenReturn(true);
    for (boolean enabled : new boolean[]{false, true, false, true}) {
      setResourceOptimization(enabled);
      Program frame = program(0);
      ProgramPrecompile compiled = frame.getProgramPrecompile();
      assertAnalysis(compiled);
      assertNotSame(legacy, compiled);
      assertSame(compiled, frame.getProgramPrecompile());
      assertNotSame(compiled, program(0).getProgramPrecompile());
      assertEquals(1, cache.size());
    }
    when(invoke.isConstantCall()).thenReturn(false);
    setResourceOptimization(false);
    assertSame(legacy, program(0).getProgramPrecompile());
  }

  private void assertFixtureEntries(ProgramPrecompile legacy, ProgramPrecompile optimized) {
    assertAnalysis(legacy);
    assertAnalysis(optimized);
    // A randomized optimized hash may equal the legacy hash; reuse is valid in that case.
    if (hashChangesAcrossModes) {
      assertNotSame(legacy, optimized);
      assertEquals(2, cache.size());
    } else {
      assertSame(legacy, optimized);
      assertEquals(1, cache.size());
    }
    for (Key key : cache.keySet()) {
      assertArrayEquals(cacheKey(0).getData(), key.getData());
    }
  }

  private static void assertAnalysis(ProgramPrecompile compiled) {
    assertTrue(compiled.hasJumpDest(0));
    assertTrue(compiled.hasJumpDest(3));
    assertFalse(compiled.hasJumpDest(1));
    assertFalse(compiled.hasJumpDest(2));
    assertFalse(compiled.hasJumpDest(4));
    assertFalse(compiled.hasJumpDest(-1));
    assertFalse(compiled.hasJumpDest(CODE.length));
  }

  private Program program(int value) {
    return new Program(CODE, address(value), invoke, internalTransaction);
  }

  private static byte[] address(int value) {
    byte[] address = new byte[21];
    address[0] = 0x41;
    for (int i = 0; i < Integer.BYTES; i++) {
      address[address.length - 1 - i] = (byte) (value >>> (8 * i));
    }
    return address;
  }

  private static Key cacheKey(int value) {
    return Key.create(ByteUtil.merge(address(value), Hash.sha3(CODE)));
  }

  @SuppressWarnings("unchecked")
  private static LRUMap<Key, ProgramPrecompile> sharedCache() throws ReflectiveOperationException {
    Field field = Program.class.getDeclaredField("programPrecompileLRUMap");
    field.setAccessible(true);
    return (LRUMap<Key, ProgramPrecompile>) field.get(null);
  }

  private static void setResourceOptimization(boolean enabled) {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = enabled;
    VMConfig.setLocalSnapshot(snapshot);
  }
}
