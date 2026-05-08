package org.tron.core.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.accountstate.callback.AccountStateCallBack;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.program.Storage;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

/**
 * TIP-2935 end-to-end: activation deploys the contract, subsequent blocks
 * populate the ring buffer via the pre-tx hook, and the VM repository reads
 * back written hashes through the same {@code Storage.compose()} layer that
 * production {@code SLOAD} uses.
 */
public class HistoryBlockHashIntegrationTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  @Before
  public void resetState() {
    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmPrague(0L);
    chainBaseManager.getDynamicPropertiesStore().saveBlockHashHistoryInstalled(0L);
    chainBaseManager.getCodeStore().delete(addr);
    chainBaseManager.getContractStore().delete(addr);
    chainBaseManager.getAccountStore().delete(addr);
    // Storage.commit() translates a zero write into a row delete (see
    // Storage#commit), so writing ZERO to every slot the suite touches is
    // the cheapest way to clear leftover state between tests.
    Storage storage = new Storage(addr, chainBaseManager.getStorageRowStore());
    for (long slot : new long[]{0L, 99L, 499L, 776L}) {
      storage.put(new DataWord(slot), DataWord.ZERO());
    }
    storage.commit();
  }

  private DataWord readSlot(long slot) {
    Storage storage = new Storage(
        HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS,
        chainBaseManager.getStorageRowStore());
    return storage.getValue(new DataWord(slot));
  }

  @Test
  public void activationDeploysContractAndFlagIsSet() {
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;

    assertEquals(0L, dps.getAllowTvmPrague());
    assertFalse(chainBaseManager.getCodeStore().has(addr));

    dps.saveAllowTvmPrague(1L);
    HistoryBlockHashUtil.deploy(dbManager);

    assertEquals(1L, dps.getAllowTvmPrague());
    assertTrue(chainBaseManager.getCodeStore().has(addr));
    CodeCapsule code = chainBaseManager.getCodeStore().get(addr);
    assertNotNull(code);
    assertArrayEquals(HistoryBlockHashUtil.HISTORY_STORAGE_CODE, code.getData());
  }

  @Test
  public void writeAfterActivationFillsStorageSlot() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmPrague(1L);
    HistoryBlockHashUtil.deploy(dbManager);

    long blockNum = 500L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0x5a);
    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, block);

    DataWord readBack = readSlot(499L);
    assertNotNull(readBack);
    assertArrayEquals(parentHash, readBack.getData());
  }

  @Test
  public void vmRepositoryReadsBackWrittenHash() {
    // Full round-trip: direct-write through Storage -> VM Repository -> getStorageValue.
    // Proves write and read go through the same Storage.compose() layer.
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmPrague(1L);
    HistoryBlockHashUtil.deploy(dbManager);

    long blockNum = 777L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0x77);
    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));
    HistoryBlockHashUtil.write(dbManager, block);

    RepositoryImpl repo = RepositoryImpl.createRoot(StoreFactory.getInstance());

    // (777 - 1) % 8191 = 776
    DataWord slotKey = new DataWord(776L);
    DataWord readBack = repo.getStorageValue(
        HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS, slotKey);

    assertNotNull("VM repository failed to read stored hash", readBack);
    assertArrayEquals("VM read-back != direct-written hash",
        parentHash, readBack.getData());
  }

  @Test
  public void noWriteBeforeActivation() {
    assertEquals(0L,
        chainBaseManager.getDynamicPropertiesStore().getAllowTvmPrague());
    assertFalse(chainBaseManager.getDynamicPropertiesStore()
        .isBlockHashHistoryInstalled());

    long blockNum = 100L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0xff);
    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    // Manager calls write() unconditionally; the install marker stays 0
    // before activation, so write() must early-return.
    HistoryBlockHashUtil.write(dbManager, block);

    assertNull(readSlot(99L));
  }

  /**
   * Block 1 is the first block to go through {@code applyBlock -> processBlock}.
   * Its parent is the genesis block, so slot 0 must hold the genesis block hash.
   */
  @Test
  public void writeForBlock1StoresGenesisHashAtSlot0() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmPrague(1L);
    HistoryBlockHashUtil.deploy(dbManager);

    byte[] genesisHash = new byte[32];
    Arrays.fill(genesisHash, (byte) 0x01);
    BlockCapsule block1 = new BlockCapsule(
        1L,
        Sha256Hash.wrap(genesisHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, block1);

    DataWord readBack = readSlot(0L);
    assertNotNull(readBack);
    assertArrayEquals(genesisHash, readBack.getData());
  }

  /**
   * Genesis never goes through {@code applyBlock}, but the guard keeps
   * {@code (0 - 1) % 8191 = -1} from ever corrupting a slot if it ever did.
   */
  @Test
  public void writeIsNoOpForGenesisBlock() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmPrague(1L);
    HistoryBlockHashUtil.deploy(dbManager);

    byte[] zeroHash = new byte[32];
    BlockCapsule genesis = new BlockCapsule(
        0L,
        Sha256Hash.wrap(zeroHash),
        0L,
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, genesis);

    assertNull(readSlot(0L));
  }

  /**
   * Collision guard: if foreign bytecode already sits at the canonical address
   * (theoretically impossible short of a hash pre-image), activation must skip
   * the deploy entirely — leaving the foreign code intact and writing nothing
   * to ContractStore / AccountStore — rather than silently merging into a
   * broken contract. Same expectation applies to foreign contract metadata.
   */
  @Test
  public void deploySkipsWhenForeignBytecodePresent() {
    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;
    byte[] foreignCode = new byte[]{0x60, 0x00};
    chainBaseManager.getCodeStore().put(addr, new CodeCapsule(foreignCode));

    HistoryBlockHashUtil.deploy(dbManager);

    assertArrayEquals(foreignCode,
        chainBaseManager.getCodeStore().get(addr).getData());
    assertFalse(chainBaseManager.getContractStore().has(addr));
    assertFalse(chainBaseManager.getAccountStore().has(addr));
  }

  @Test
  public void deploySkipsWhenForeignContractPresent() {
    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;
    SmartContract foreign = SmartContract.newBuilder()
        .setName("NotBlockHashHistory")
        .setContractAddress(ByteString.copyFrom(addr))
        .setOriginAddress(ByteString.copyFrom(addr))
        .build();
    chainBaseManager.getContractStore().put(addr, new ContractCapsule(foreign));

    HistoryBlockHashUtil.deploy(dbManager);

    assertEquals("NotBlockHashHistory",
        chainBaseManager.getContractStore().get(addr).getInstance().getName());
    assertFalse(chainBaseManager.getCodeStore().has(addr));
    assertFalse(chainBaseManager.getAccountStore().has(addr));
  }

  /**
   * Anyone can transfer TRX to {@code HISTORY_STORAGE_ADDRESS} before the
   * proposal fires, leaving an EOA at the canonical address. Activation must
   * upgrade the type to {@code Contract} in place — preserving balance —
   * rather than failing or zeroing the account.
   */
  /**
   * SR / validator parity: the producer's {@code generateBlock} simulation
   * loop and the validator's {@code processBlock} apply loop must see the
   * same storage state when transactions hit {@code HISTORY_STORAGE_ADDRESS}.
   * That requires {@link HistoryBlockHashUtil#write} to run before the tx
   * loop on both paths. {@code processBlock} writes at line 1858; this test
   * pins the matching write inside {@code generateBlock}.
   *
   * <p>Spy {@code accountStateCallBack.preExecute} — called between the
   * write and the tx loop on both paths — and snapshot the slot from inside
   * the revoking session. Pre-fix the slot is empty (write never ran);
   * post-fix it holds the parent block hash.
   */
  @Test
  public void generateBlockWritesParentHashBeforeTxLoop() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmPrague(1L);
    HistoryBlockHashUtil.deploy(dbManager);

    byte[] expectedParentHash = chainBaseManager.getHeadBlockId().getBytes();
    long nextBlockNum = chainBaseManager.getHeadBlockNum() + 1;
    long expectedSlot =
        (nextBlockNum - 1) % HistoryBlockHashUtil.HISTORY_SERVE_WINDOW;

    Field cbField = Manager.class.getDeclaredField("accountStateCallBack");
    cbField.setAccessible(true);
    AccountStateCallBack realCb = (AccountStateCallBack) cbField.get(dbManager);
    AccountStateCallBack spy = Mockito.spy(realCb);
    AtomicReference<DataWord> captured = new AtomicReference<>();
    Mockito.doAnswer(inv -> {
      Storage st = new Storage(
          HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS,
          chainBaseManager.getStorageRowStore());
      captured.set(st.getValue(new DataWord(expectedSlot)));
      return inv.callRealMethod();
    }).when(spy).preExecute(Mockito.any(BlockCapsule.class));
    cbField.set(dbManager, spy);

    try {
      ECKey ecKey = new ECKey();
      ByteString witness = ByteString.copyFrom(ecKey.getAddress());
      Param.Miner miner =
          Param.getInstance().new Miner(ecKey.getPrivKeyBytes(), witness, witness);
      long blockTime = System.currentTimeMillis() / 3000 * 3000;
      BlockCapsule generated = dbManager.generateBlock(
          miner, blockTime, System.currentTimeMillis() + 1000);
      assertNotNull("generateBlock returned null", generated);
    } finally {
      cbField.set(dbManager, realCb);
    }

    assertNotNull(
        "preExecute fired with an empty slot — write() must run before preExecute",
        captured.get());
    assertArrayEquals(
        "slot must hold the parent block hash before the tx loop runs",
        expectedParentHash, captured.get().getData());
  }

  @Test
  public void deployUpgradesPreExistingNormalAccountPreservingBalance() {
    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;
    long balance = 12345L;
    AccountCapsule eoa = new AccountCapsule(
        ByteString.copyFrom(addr), Protocol.AccountType.Normal);
    eoa.setBalance(balance);
    chainBaseManager.getAccountStore().put(addr, eoa);

    HistoryBlockHashUtil.deploy(dbManager);

    AccountCapsule after = chainBaseManager.getAccountStore().get(addr);
    assertEquals(Protocol.AccountType.Contract, after.getType());
    assertEquals(balance, after.getBalance());
    assertTrue(chainBaseManager.getCodeStore().has(addr));
    assertTrue(chainBaseManager.getContractStore().has(addr));
  }

  @Test
  public void deployCreatesCodeContractAndAccount() {
    HistoryBlockHashUtil.deploy(dbManager);

    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;

    assertTrue(chainBaseManager.getCodeStore().has(addr));
    CodeCapsule code = chainBaseManager.getCodeStore().get(addr);
    assertNotNull(code);
    assertArrayEquals(HistoryBlockHashUtil.HISTORY_STORAGE_CODE, code.getData());

    ContractCapsule contract = chainBaseManager.getContractStore().get(addr);
    assertNotNull(contract);
    SmartContract proto = contract.getInstance();
    assertEquals(HistoryBlockHashUtil.HISTORY_STORAGE_NAME, proto.getName());
    assertArrayEquals(addr, proto.getContractAddress().toByteArray());
    assertEquals("version must be 0", 0, proto.getVersion());
    assertEquals(100L, proto.getConsumeUserResourcePercent());
    assertArrayEquals("originAddress must be the EIP-2935 system caller",
        HistoryBlockHashUtil.HISTORY_DEPLOYER_ADDRESS,
        proto.getOriginAddress().toByteArray());

    assertTrue(chainBaseManager.getAccountStore().has(addr));
    AccountCapsule account = chainBaseManager.getAccountStore().get(addr);
    assertEquals(HistoryBlockHashUtil.HISTORY_STORAGE_NAME,
        account.getAccountName().toStringUtf8());
    assertEquals(Protocol.AccountType.Contract, account.getType());
    assertTrue("install marker must flip after a successful deploy",
        chainBaseManager.getDynamicPropertiesStore().isBlockHashHistoryInstalled());
  }

  @Test
  public void writeStoresParentHashAtCorrectSlot() {
    HistoryBlockHashUtil.deploy(dbManager);

    long blockNum = 100L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0xab);

    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, block);

    DataWord readBack = readSlot(99L);
    assertNotNull(readBack);
    assertArrayEquals(parentHash, readBack.getData());
  }

  @Test
  public void writeUsesRingBufferModulo() {
    HistoryBlockHashUtil.deploy(dbManager);

    // (8192 - 1) % 8191 = 0
    long blockNum = 8192L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0xcd);

    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, block);

    DataWord readBack = readSlot(0L);
    assertNotNull(readBack);
    assertArrayEquals(parentHash, readBack.getData());
  }

  @Test
  public void beforeDeployNothingIsWritten() {
    assertFalse(chainBaseManager.getCodeStore()
        .has(HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS));
    assertFalse(chainBaseManager.getContractStore()
        .has(HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS));
    assertFalse(chainBaseManager.getAccountStore()
        .has(HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS));
  }

  /**
   * If {@code deploy()} never ran (e.g. flag flipped without the deploy path),
   * {@code write()} must not mutate {@code StorageRowStore} at the canonical
   * address — otherwise the next call to {@code deploy()} would land on top of
   * partially-written state.
   */
  @Test
  public void writeIsNoOpBeforeDeploy() {
    long blockNum = 100L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0xab);
    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, block);

    assertNull("write() must be a no-op without an installed BlockHashHistory",
        readSlot(99L));
  }

  /**
   * Defense-in-depth: when foreign bytecode sits at the canonical address,
   * {@code deploy()} skips and the install marker stays 0, so {@code write()}
   * must refuse to overwrite that contract's storage every block. Triggering
   * the collision in practice requires a SHA-3 pre-image of the address, but
   * the marker check is a single cached store hit.
   */
  @Test
  public void writeIsNoOpOnForeignCode() {
    byte[] addr = HistoryBlockHashUtil.HISTORY_STORAGE_ADDRESS;
    byte[] foreignCode = Hex.decode("60016002");
    chainBaseManager.getCodeStore().put(addr, new CodeCapsule(foreignCode));

    HistoryBlockHashUtil.deploy(dbManager);

    assertFalse("install marker must stay 0 when deploy skipped",
        chainBaseManager.getDynamicPropertiesStore().isBlockHashHistoryInstalled());

    long blockNum = 100L;
    byte[] parentHash = new byte[32];
    Arrays.fill(parentHash, (byte) 0xcd);
    BlockCapsule block = new BlockCapsule(
        blockNum,
        Sha256Hash.wrap(parentHash),
        System.currentTimeMillis(),
        ByteString.copyFrom(new byte[21]));

    HistoryBlockHashUtil.write(dbManager, block);

    assertNull("write() must not overwrite a foreign contract's storage",
        readSlot(99L));
    assertArrayEquals("foreign code must remain intact",
        foreignCode, chainBaseManager.getCodeStore().get(addr).getData());
  }

}
