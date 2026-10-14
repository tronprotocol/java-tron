package org.tron.common.runtime.vm;

import static org.tron.protos.Protocol.Transaction.Result.contractResult.OUT_OF_TIME;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.ForkController;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;

public class OptimizeTvmStorageTest extends VMTestBase {

  private static final long FEE_LIMIT = 1_000_000_000L;
  private static final String NON_ALIAS =
      "6001600155600260025500";
  private static final String ALIAS =
      "600160015560027f00000000000000000000000000000001000000000000000000000000000000015500";

  @Override
  protected void beforeDestroy() {
    VMConfig.initAllowOptimizeTvmStorage(0);
  }

  @Test
  public void optimizedKeyKeepsFullSlotAndTombstone() {
    activateFork();
    VMConfig.initAllowOptimizeTvmStorage(1);
    byte[] address = account("00000000000000000000000000000000000000c1");
    rootRepository.createAccount(address, AccountType.Normal);
    rootRepository.addBalance(address, 1_000_000L);

    byte[] slotA = word(1);
    byte[] slotB = word(1);
    slotB[15] = 1;
    rootRepository.putStorageValue(address, new DataWord(slotA), new DataWord(5));
    rootRepository.putStorageValue(address, new DataWord(slotA), new DataWord(0));
    rootRepository.commit();
    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.putStorageValue(address, new DataWord(slotB), new DataWord(6));
    rootRepository.commit();

    byte[] newA = optimizedKey(address, slotA);
    byte[] newB = optimizedKey(address, slotB);
    Assert.assertEquals(48, newA.length);
    Assert.assertFalse(Arrays.equals(newA, newB));
    byte[] storedA = dbManager.getStorageRowStore().get(newA).getValue();
    byte[] storedB = dbManager.getStorageRowStore().get(newB).getValue();
    Assert.assertEquals(32, storedA.length);
    Assert.assertTrue(new DataWord(storedA).isZero());
    Assert.assertEquals(6L, new DataWord(storedB).longValue());

    byte[] legacyOnly = word(2);
    byte[] legacyKey = legacyKey(address, legacyOnly);
    dbManager.getStorageRowStore().put(legacyKey, new StorageRowCapsule(new DataWord(7).getData()));
    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    DataWord loaded = rootRepository.getStorageValue(address, new DataWord(legacyOnly));
    Assert.assertEquals(7L, loaded.longValue());
    rootRepository.putStorageValue(address, new DataWord(legacyOnly), new DataWord(9));
    rootRepository.commit();
    byte[] migrated = dbManager.getStorageRowStore().get(optimizedKey(address, legacyOnly))
        .getValue();
    Assert.assertEquals(9L, new DataWord(migrated).longValue());
    Assert.assertFalse(dbManager.getStorageRowStore().has(legacyKey));
    DataWord after = RepositoryImpl.createRoot(StoreFactory.getInstance())
        .getStorageValue(address, new DataWord(legacyOnly));
    Assert.assertEquals(9L, after.longValue());
  }

  @Test
  public void migratedWriteClearsSharedLegacyRow() {
    activateFork();
    VMConfig.initAllowOptimizeTvmStorage(1);
    byte[] address = account("00000000000000000000000000000000000000c3");
    rootRepository.createAccount(address, AccountType.Normal);
    rootRepository.commit();

    byte[] slotA = word(1);
    byte[] slotB = word(1);
    slotB[15] = 1;
    byte[] legacy = legacyKey(address, slotA);
    Assert.assertArrayEquals(legacy, legacyKey(address, slotB));
    dbManager.getStorageRowStore().put(legacy,
        new StorageRowCapsule(new DataWord(100).getData()));

    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.putStorageValue(address, new DataWord(slotA), DataWord.ZERO());
    rootRepository.commit();

    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    Assert.assertNull(rootRepository.getStorageValue(address, new DataWord(slotB)));
    Assert.assertFalse(dbManager.getStorageRowStore().has(legacy));
    Assert.assertNull(rootRepository.getStorageValue(address, new DataWord(slotA)));
    byte[] stored = dbManager.getStorageRowStore()
        .get(optimizedKey(address, slotA)).getValue();
    Assert.assertTrue(new DataWord(stored).isZero());
  }

  @Test
  public void zeroThenNonZeroKeepsLegacyChargeBeforeProposal() throws Exception {
    activateFork();
    long fresh = deploy("600160005500").getResult().getEnergyUsed();
    long zeroThenSet = deploy("6000600055600160005500").getResult().getEnergyUsed();
    // Proposal off: a cached 0 still counts as an existing value, so the second
    // write is RESET (5000), not SET (20000). Delta against a fresh SET is
    // two extra PUSH1 (6) + 5000 - 20000.
    Assert.assertEquals(-9994L, zeroThenSet - fresh);
  }

  @Test
  public void zeroThenNonZeroKeepsResetChargeWhenOptimized() throws Exception {
    activateFork();
    dbManager.getDynamicPropertiesStore().saveAllowOptimizeTvmStorage(1);
    try {
      long fresh = deploy("FreshSet", "600160005500").getResult().getEnergyUsed();
      long zeroThenSet = deploy("ZeroThenSet", "6000600055600160005500")
          .getResult().getEnergyUsed();
      // A zero written earlier in this transaction stays in the cache, so the
      // following non-zero write is RESET (5000), same as before the proposal.
      Assert.assertEquals(-9994L, zeroThenSet - fresh);
    } finally {
      dbManager.getDynamicPropertiesStore().saveAllowOptimizeTvmStorage(0);
    }
  }

  @Test
  public void tombstoneThenNonZeroChargesAsFirstWrite() throws Exception {
    activateFork();
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    dbManager.getDynamicPropertiesStore().saveAllowOptimizeTvmStorage(1);
    VMConfig.initAllowOptimizeTvmStorage(1);
    try {
      String init = "6006600c60003960066000f3600160005500";
      Runtime seededRuntime = deploy("Seeded", init);
      Runtime freshRuntime = deploy("Fresh", init);
      Assert.assertEquals(SUCCESS, seededRuntime.getResult().getResultCode());
      Assert.assertEquals(SUCCESS, freshRuntime.getResult().getResultCode());
      byte[] seeded = seededRuntime.getResult().getContractAddress();
      byte[] fresh = freshRuntime.getResult().getContractAddress();

      RepositoryImpl repo = RepositoryImpl.createRoot(StoreFactory.getInstance());
      repo.putStorageValue(seeded, new DataWord(0), DataWord.ZERO());
      repo.commit();
      DataWord tombstone = RepositoryImpl.createRoot(StoreFactory.getInstance())
          .getStorageValue(seeded, new DataWord(0));
      Assert.assertNull(tombstone);

      byte[] caller = Hex.decode(OWNER_ADDRESS);
      Runtime seededCall = TvmTestUtils.processTransactionAndReturnRuntime(
          TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
              caller, seeded, new byte[0], 0, FEE_LIMIT),
          dbManager, null);
      Runtime freshCall = TvmTestUtils.processTransactionAndReturnRuntime(
          TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
              caller, fresh, new byte[0], 0, FEE_LIMIT),
          dbManager, null);
      Assert.assertEquals(SUCCESS, seededCall.getResult().getResultCode());
      Assert.assertEquals(SUCCESS, freshCall.getResult().getResultCode());
      Assert.assertEquals(freshCall.getResult().getEnergyUsed(),
          seededCall.getResult().getEnergyUsed());
      DataWord written = RepositoryImpl.createRoot(StoreFactory.getInstance())
          .getStorageValue(seeded, new DataWord(0));
      Assert.assertEquals(1L, written.longValue());
    } finally {
      dbManager.getDynamicPropertiesStore().saveAllowOptimizeTvmStorage(0);
      dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(0);
    }
  }

  @Test
  public void distinctSlotsStaySuccessfulAfterFork() throws Exception {
    activateFork();
    Runtime runtime = deploy(NON_ALIAS);
    Assert.assertEquals(SUCCESS, runtime.getResult().getResultCode());
    Assert.assertNull(runtime.getResult().getException());
  }

  @Test
  public void aliasedSstoreTimesOutAfterFork() throws Exception {
    activateFork();
    dbManager.getDynamicPropertiesStore().saveAllowTvmCompatibleEvm(0);
    VMConfig.initAllowTvmCompatibleEvm(0);
    byte[] address = account("00000000000000000000000000000000000000c2");
    rootRepository.createAccount(address, AccountType.Normal);
    byte[] slotA = word(1);
    byte[] slotB = word(1);
    slotB[15] = 1;
    rootRepository.putStorageValue(address, new DataWord(slotA), new DataWord(1));
    try {
      rootRepository.putStorageValue(address, new DataWord(slotB), new DataWord(2));
      Assert.fail("aliased sstore must time out");
    } catch (OutOfTimeException expected) {
      Assert.assertNotNull(expected);
    }

    Runtime runtime = deploy(ALIAS);
    Assert.assertEquals(OUT_OF_TIME, runtime.getResult().getResultCode());
    Assert.assertTrue(runtime.getResult().getException() instanceof OutOfTimeException);
  }

  private Runtime deploy(String codeHex) throws Exception {
    return deploy("Alias", codeHex);
  }

  private Runtime deploy(String name, String codeHex) throws Exception {
    byte[] caller = Hex.decode(OWNER_ADDRESS);
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        name, caller, "[]", codeHex, 0, FEE_LIMIT, 100, null, 0);
    return TvmTestUtils.processTransactionAndReturnRuntime(trx, dbManager, null);
  }

  private void activateFork() {
    ForkController.instance().init(chainBaseManager);
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    dbManager.getDynamicPropertiesStore().statsByVersion(
        ForkBlockVersionEnum.VERSION_4_8_2_3.getValue(), stats);
    long interval = dbManager.getDynamicPropertiesStore().getMaintenanceTimeInterval();
    long hardForkTime = ((ForkBlockVersionEnum.VERSION_4_8_2_3.getHardForkTime() - 1)
        / interval + 1) * interval;
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(hardForkTime + 1);
    Assert.assertTrue(ForkController.instance().pass(ForkBlockVersionEnum.VERSION_4_8_2_3));
  }

  private static byte[] account(String tail) {
    return Hex.decode("41" + tail);
  }

  private static byte[] word(int value) {
    byte[] out = new byte[32];
    out[31] = (byte) value;
    return out;
  }

  private static byte[] optimizedKey(byte[] address, byte[] slot) {
    byte[] addrHash = Hash.sha3(address);
    byte[] key = new byte[48];
    System.arraycopy(addrHash, 0, key, 0, 16);
    System.arraycopy(Hash.sha3(ByteUtil.merge(addrHash, slot)), 0, key, 16, 32);
    return key;
  }

  private static byte[] legacyKey(byte[] address, byte[] slot) {
    byte[] key = new byte[32];
    System.arraycopy(Hash.sha3(address), 0, key, 0, 16);
    System.arraycopy(slot, 16, key, 16, 16);
    return key;
  }
}
