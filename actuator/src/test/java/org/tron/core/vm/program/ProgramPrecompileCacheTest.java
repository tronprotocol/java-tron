package org.tron.core.vm.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.commons.collections4.map.LRUMap;
import org.junit.After;
import org.junit.Test;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Repository;

public class ProgramPrecompileCacheTest {

  @After
  public void tearDown() {
    VMConfig.clearLocalSnapshot();
  }

  @Test
  public void cacheCanRecompileAcrossProposalActivationAndRollback() {
    for (boolean enabled : new boolean[]{false, true, false, true}) {
      setResourceOptimization(enabled);
      ProgramPrecompile compiled = program().getProgramPrecompile();
      assertTrue(compiled.hasJumpDest(0));
      assertTrue(compiled.hasJumpDest(3));
      assertFalse(compiled.hasJumpDest(1));
      assertFalse(compiled.hasJumpDest(2));
      assertFalse(compiled.hasJumpDest(4));
      assertSame(compiled, program().getProgramPrecompile());
    }
  }

  @Test
  public void lruEvictsEntriesWithOldHashesWithoutExceedingCapacity() {
    LRUMap<Key, Integer> cache = new LRUMap<>(4);
    for (boolean enabled : new boolean[]{false, true, false, true}) {
      setResourceOptimization(enabled);
      // Replace every entry after each switch, including entries from the previous hash mode.
      for (int value = 0; value < 16; value++) {
        cache.put(cacheKey(value), value);
        assertEquals(Integer.valueOf(value), cache.get(cacheKey(value)));
        assertTrue(cache.size() <= 4);
      }
      assertEquals(4, cache.size());
      for (Map.Entry<Key, Integer> entry : cache.entrySet()) {
        assertEquals(entry.getValue(), cache.get(entry.getKey(), false));
      }
    }
  }

  private static Program program() {
    byte[] address = new byte[21];
    address[0] = 0x41;
    address[20] = 0x5a;
    ProgramInvoke invoke = mock(ProgramInvoke.class);
    when(invoke.getContractAddress()).thenReturn(new DataWord(address));
    when(invoke.getDeposit()).thenReturn(mock(Repository.class));
    return new Program(new byte[]{0x5b, 0x60, 0x5b, 0x5b, 0}, address, invoke,
        mock(InternalTransaction.class));
  }

  private static Key cacheKey(int value) {
    byte[] bytes = new byte[53];
    bytes[52] = (byte) value;
    return Key.create(bytes);
  }

  private static void setResourceOptimization(boolean enabled) {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = enabled;
    VMConfig.setLocalSnapshot(snapshot);
  }
}
