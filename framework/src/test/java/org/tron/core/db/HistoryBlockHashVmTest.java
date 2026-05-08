package org.tron.core.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.program.Program.IllegalOperationException;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;

/**
 * Real STATICCALL execution of the deployed TIP-2935 bytecode through the VM
 * trace path. Complements {@link HistoryBlockHashIntegrationTest}, which only
 * verifies that storage writes round-trip through {@code RepositoryImpl}.
 *
 * <p>Each test prepares storage and a {@code BlockCapsule} (which fixes the
 * EVM {@code block.number}), invokes the deployed contract via
 * {@code TvmTestUtils.triggerContract...}, and asserts the bytecode's
 * documented branches: normal return, bootstrap zero, three revert paths,
 * and PUSH0 not tripping {@code IllegalOperationException} under Shanghai.
 */
public class HistoryBlockHashVmTest extends BaseTest {

  static {
    Args.setParam(
        new String[]{"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);
  }

  private static final byte[] OWNER =
      Hex.decode("41abd4b9367799eaa3197fecb144eb71de1e049abc");
  private static final long FEE_LIMIT = 1_000_000_000L;

  @Before
  public void init() {
    // Some prior tests in the same Gradle JVM batch may flip ConfigLoader.disable
    // to true, which would freeze VMConfig at whatever it last held. Reset so
    // VMActuator picks up the DPS values we set below.
    ConfigLoader.disable = false;

    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    dps.saveAllowTvmConstantinople(1L);
    dps.saveAllowTvmTransferTrc10(1L);
    dps.saveAllowTvmSolidity059(1L);
    dps.saveAllowTvmIstanbul(1L);
    dps.saveAllowTvmLondon(1L);
    dps.saveAllowTvmShangHai(1L);
    dps.saveAllowTvmPrague(1L);

    AccountCapsule owner = new AccountCapsule(
        ByteString.copyFrom(OWNER), Protocol.AccountType.Normal);
    owner.setBalance(30_000_000_000_000L);
    chainBaseManager.getAccountStore().put(OWNER, owner);

    HistoryBlockHashUtil.deploy(dbManager);
  }

  @After
  public void cleanup() {
    // BaseTest shares the Spring context across @Test methods in this class,
    // so reset every store we touched.
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    dps.saveAllowTvmShangHai(0L);
    dps.saveAllowTvmPrague(0L);
    dps.saveBlockHashHistoryInstalled(0L);

    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;
    chainBaseManager.getCodeStore().delete(addr);
    chainBaseManager.getContractStore().delete(addr);
    chainBaseManager.getAccountStore().delete(addr);

    Storage storage = new Storage(addr, chainBaseManager.getStorageRowStore());
    for (long slot : new long[]{0L, 1L, 50L, 100L, 900L, 999L, 1000L}) {
      storage.put(new DataWord(slot), DataWord.ZERO());
    }
    storage.commit();
  }

  private void writeSlot(long slot, byte[] hash) {
    Storage storage = new Storage(
        HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS,
        chainBaseManager.getStorageRowStore());
    storage.put(new DataWord(slot), new DataWord(hash));
    storage.commit();
  }

  private BlockCapsule blockAt(long num) {
    BlockCapsule block = new BlockCapsule(
        num,
        Sha256Hash.wrap(new byte[32]),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));
    // Skip the cpu-limit-ratio path that reads {@code trx.getRet(0)}; the
    // bare TriggerSmartContract built by TvmTestUtils carries no Ret entry.
    block.generatedByMyself = true;
    return block;
  }

  private static byte[] uint256(long n) {
    return new DataWord(n).getData();
  }

  private TVMTestResult call(byte[] calldata, long currentBlockNum) throws Exception {
    return TvmTestUtils.triggerContractAndReturnTvmTestResult(
        OWNER,
        HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS,
        calldata,
        0L,
        FEE_LIMIT,
        dbManager,
        blockAt(currentBlockNum));
  }

  /**
   * Normal read: 32-byte calldata pointing at a block within the sliding
   * window whose slot has been populated. The bytecode SLOAD-and-RETURNs
   * the stored hash; this also exercises every PUSH0 in the read path.
   */
  @Test
  public void vmReturnsWrittenHashForBlockInWindow() throws Exception {
    long current = 1000L;
    long queried = current - 100L;
    byte[] hash = new byte[32];
    Arrays.fill(hash, (byte) 0xab);
    writeSlot(queried % HistoryBlockHashUtil.HISTORY_SERVE_WINDOW, hash);

    TVMTestResult result = call(uint256(queried), current);

    assertFalse("must not revert", result.getRuntime().getResult().isRevert());
    assertNull("must not throw", result.getRuntime().getResult().getException());
    byte[] hReturn = result.getRuntime().getResult().getHReturn();
    assertNotNull("must return data", hReturn);
    assertArrayEquals(hash, hReturn);
  }

  /**
   * Bootstrap behavior: when a slot has not been written yet (pre-activation
   * blocks within the sliding window, or fresh post-activation), the SLOAD
   * returns 0 and the contract returns {@code bytes32(0)} — never reverts.
   */
  @Test
  public void vmReturnsZeroForUnwrittenSlot() throws Exception {
    long current = 1000L;
    long queried = current - 50L;

    TVMTestResult result = call(uint256(queried), current);

    assertFalse("must not revert", result.getRuntime().getResult().isRevert());
    assertNull("must not throw", result.getRuntime().getResult().getException());
    byte[] hReturn = result.getRuntime().getResult().getHReturn();
    assertNotNull("must return data", hReturn);
    assertArrayEquals(new byte[32], hReturn);
  }

  /**
   * Out-of-range upper bound: querying the current block number (or any
   * future block) is not serviceable — the bytecode reverts via 5f5ffd.
   */
  @Test
  public void vmRevertsForFutureBlock() throws Exception {
    long current = 1000L;
    long queried = current;

    TVMTestResult result = call(uint256(queried), current);

    assertTrue("must revert for queried >= current",
        result.getRuntime().getResult().isRevert());
  }

  /**
   * Out-of-range lower bound: once {@code queried + 8191 < current}, the slot
   * has already been overwritten by a newer block in the ring buffer, so the
   * bytecode reverts rather than returning a stale hash.
   */
  @Test
  public void vmRevertsForBlockOutsideWindow() throws Exception {
    long current = 1000L + HistoryBlockHashUtil.HISTORY_SERVE_WINDOW + 1L;
    long queried = 1000L;

    TVMTestResult result = call(uint256(queried), current);

    assertTrue("must revert for queried + window < current",
        result.getRuntime().getResult().isRevert());
  }

  /**
   * Calldata length guard: anything other than 32 bytes — including the
   * 4-byte ABI selector shape Solidity callers might accidentally encode —
   * reverts immediately at the {@code 60203603604257} preamble.
   */
  @Test
  public void vmRevertsForBadCalldataLength() throws Exception {
    long current = 1000L;
    byte[] shortCalldata = new byte[]{0x01, 0x02, 0x03, 0x04};

    TVMTestResult result = call(shortCalldata, current);

    assertTrue("must revert for calldata.size != 32",
        result.getRuntime().getResult().isRevert());
  }

  /**
   * Shanghai gate: the bytecode contains four PUSH0 (0x5f) opcodes on the
   * read path. With {@code ALLOW_TVM_SHANGHAI=1}, a normal call must reach
   * the RETURN without {@code IllegalOperationException} — i.e., PUSH0 is
   * recognized and not treated as an invalid opcode.
   */
  @Test
  public void vmExecutionDoesNotInvalidOpcodeUnderShanghai() throws Exception {
    long current = 1000L;
    long queried = current - 1L;
    byte[] hash = new byte[32];
    Arrays.fill(hash, (byte) 0xcd);
    writeSlot(queried % HistoryBlockHashUtil.HISTORY_SERVE_WINDOW, hash);

    TVMTestResult result = call(uint256(queried), current);

    Throwable ex = result.getRuntime().getResult().getException();
    assertFalse("PUSH0 must not be an invalid opcode under Shanghai",
        ex instanceof IllegalOperationException);
    assertFalse("normal read must not revert",
        result.getRuntime().getResult().isRevert());
  }
}
