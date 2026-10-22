package org.tron.common.runtime.vm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.ILLEGAL_OPERATION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.OUT_OF_TIME;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.ForkController;
import org.tron.core.actuator.VMActuator;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.db.TransactionContext;
import org.tron.core.db2.ISession;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Storage;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

/**
 * Exercises storage migration through real VM transactions and revocable database sessions.
 * Contract version 0 slots with identical low 128 bits share a legacy physical row.
 */
public class StorageMigrationTest extends VMTestBase {

  private static final String STOP = "00";
  private static final String REVERT_CODE = "60006000fd";
  private static final String INVALID = "fe";
  private static final String READ = "60005450"; // SLOAD(0), POP
  private static final String WRITE = "6009600055"; // SSTORE(0, 9)
  private static final String WRITE_ZERO = "6000600055";
  private static final String WRITE_OTHER = "6003600155"; // SSTORE(1, 3)
  private static final String ALIAS = "00000000000000000000000000000001"
      + "00000000000000000000000000000000";
  private static final String READ_ALIAS = "7f" + ALIAS + "5450";
  private static final String WRITE_ALIAS = "60087f" + ALIAS + "55"; // SSTORE(2^128, 8)

  private int addressSequence = 0x1000;
  private VMConfig.Snapshot savedGlobalConfig;
  private boolean savedConfigLoaderDisabled;
  private boolean savedEnergyLimitHardFork;
  private boolean savedVmTrace;

  @Override
  protected void beforeContext() {
    savedGlobalConfig = (VMConfig.Snapshot) ReflectionTestUtils.getField(VMConfig.class,
        "globalSnapshot");
    // Detach the saved object before any init* setter mutates the global snapshot in place.
    VMConfig.setGlobalSnapshot(new VMConfig.Snapshot());
    savedConfigLoaderDisabled = ConfigLoader.disable;
    savedEnergyLimitHardFork = VMConfig.getEnergyLimitHardFork();
    savedVmTrace = VMConfig.vmTrace();
    ConfigLoader.disable = false;
  }

  @Override
  protected void afterInit() {
    super.afterInit();
    ForkController.instance().init(chainBaseManager);
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    for (ForkBlockVersionEnum fork : new ForkBlockVersionEnum[]{
        ForkBlockVersionEnum.VERSION_4_8_2_3, ForkBlockVersionEnum.VERSION_4_8_2_4}) {
      properties().statsByVersion(fork.getValue(), stats);
    }
    long interval = properties().getMaintenanceTimeInterval();
    long forkTime = ((ForkBlockVersionEnum.VERSION_4_8_2_4.getHardForkTime() - 1)
        / interval + 1) * interval;
    properties().saveLatestBlockHeaderTimestamp(forkTime + 1);
    properties().saveLatestBlockHeaderNumber(100_000_000L);
    properties().saveAllowTvmConstantinople(1);
    properties().saveAllowTvmSolidity059(1);
    setMode(0, 1);
    VMConfig.initVmHardFork(true);
  }

  @Override
  protected void beforeDestroy() {
    VMConfig.setGlobalSnapshot(savedGlobalConfig);
    VMConfig.initVmHardFork(savedEnergyLimitHardFork);
    VMConfig.setVmTrace(savedVmTrace);
    ConfigLoader.disable = savedConfigLoaderDisabled;
  }

  @Test
  public void neitherProposalUsesLegacyLayout() throws Exception {
    assertProposalMode(0, 0);
  }

  @Test
  public void proposal99AloneMigratesReads() throws Exception {
    assertProposalMode(1, 0);
  }

  @Test
  public void proposal102AloneMigratesOnlyWrites() throws Exception {
    assertProposalMode(0, 1);
  }

  @Test
  public void bothProposalsUse102ReadProtection() throws Exception {
    assertProposalMode(1, 1);
  }

  @Test
  public void extCodeHashSkipsEmptyCodeWriteWhenProposal102() throws Exception {
    // Non-empty code is written back even when 102 is on. Only empty code skips the write.
    for (boolean emptyCode : new boolean[]{false, true}) {
      byte[] targetCode = emptyCode ? new byte[0] : Hex.decode("600160005500");
      for (int proposal99 : new int[]{0, 1}) {
        for (int proposal102 : new int[]{0, 1}) {
          setMode(proposal99, proposal102);
          boolean skipWrite = proposal102 == 1 && emptyCode;
          for (byte[] initialHash : new byte[][]{new byte[0], new byte[32],
              new DataWord(123).getData()}) {
            String scenario = "empty=" + emptyCode + ", 99=" + proposal99 + ", 102=" + proposal102
                + ", initialHash=" + Hex.toHexString(initialHash);
            byte[] target = install(emptyCode ? "00" : Hex.toHexString(targetCode));
            if (emptyCode) {
              dbManager.getCodeStore().delete(target);
            }
            setCodeHash(target, initialHash);
            byte[] reader = install(readCodeHash(target) + "60005260206000f3");
            boolean missing = ByteUtil.isNullOrZeroArray(initialHash);
            byte[] returned = missing ? Hash.sha3(targetCode) : initialHash;
            byte[] stored = missing && !skipWrite ? Hash.sha3(targetCode) : initialHash;

            for (int read = 0; read < 2; read++) {
              Runtime result = TvmTestUtils.processTransactionAndReturnRuntime(
                  transaction(reader), dbManager, null);
              assertEquals(scenario, SUCCESS, result.getResult().getResultCode());
              assertArrayEquals(scenario, returned, result.getResult().getHReturn());
              assertArrayEquals(scenario, stored,
                  repository().getContract(target).getCodeHash());
            }
          }
        }
      }
    }
  }

  @Test
  public void extCodeHashMetadataRespectsTransactionAndSessionRollback() throws Exception {
    byte[] targetCode = Hex.decode("600160005500");
    for (int proposal99 : new int[]{0, 1}) {
      for (int proposal102 : new int[]{0, 1}) {
        setMode(proposal99, proposal102);
        String scenario = "99=" + proposal99 + ", 102=" + proposal102;
        byte[] target = install(Hex.toHexString(targetCode));
        setCodeHash(target, new byte[0]);
        for (String end : new String[]{REVERT_CODE, INVALID}) {
          byte[] reader = install(readCodeHash(target) + "50" + end);
          assertResult(scenario + ", end=" + end, resultFor(end), reader);
          assertArrayEquals(scenario, new byte[0], repository().getContract(target).getCodeHash());
        }

        byte[] reader = install(readCodeHash(target) + "50" + STOP);
        try (ISession ignored = dbManager.getRevokingStore().buildSession()) {
          assertResult(scenario + " before session rollback", SUCCESS, reader);
          assertArrayEquals(scenario, Hash.sha3(targetCode),
              repository().getContract(target).getCodeHash());
        }
        assertArrayEquals(scenario, new byte[0], repository().getContract(target).getCodeHash());
      }
    }
  }

  private void assertProposalMode(int proposal99, int proposal102) throws Exception {
    setMode(proposal99, proposal102);
    String scenario = "99=" + proposal99 + ", 102=" + proposal102;
    boolean readMigrates = proposal99 == 1 && proposal102 == 0;
    boolean newLayout = proposal99 == 1 || proposal102 == 1;

    byte[] reader = install(READ + STOP);
    seedLegacyRow(reader);
    assertResult(scenario + " read", SUCCESS, reader);
    assertLayout(scenario + " read", reader, readMigrates, 7);

    byte[] aliasReader = install(READ + READ_ALIAS + STOP);
    seedLegacyRow(aliasReader);
    assertResult(scenario + " alias", readMigrates ? SUCCESS : OUT_OF_TIME, aliasReader);
    assertLayout(scenario + " alias", aliasReader, readMigrates, 7);
    assertFalse(scenario, has(newKey(aliasReader, aliasSlot())));

    for (String end : new String[]{STOP, REVERT_CODE, INVALID}) {
      byte[] writer = install(WRITE + end);
      seedLegacyRow(writer);
      assertResult(scenario + " write, end=" + end, resultFor(end), writer);
      boolean success = STOP.equals(end);
      assertLayout(scenario + " write, end=" + end, writer, success && newLayout,
          success ? 9 : 7);
    }
  }

  @Test
  public void topLevelReadsDoNotMigrateAndWritesMigrateOnlyOnSuccess() throws Exception {
    for (String operations : new String[]{READ, READ + WRITE_OTHER, READ + WRITE,
        WRITE, WRITE_ZERO}) {
      for (String end : new String[]{STOP, REVERT_CODE, INVALID}) {
        String scenario = "operations=" + operations + ", end=" + end;
        byte[] address = install(operations + end);
        seedLegacyRow(address);
        assertResult(scenario, resultFor(end), address);
        boolean dirty = operations.equals(READ + WRITE) || operations.equals(WRITE)
            || operations.equals(WRITE_ZERO);
        boolean migrated = STOP.equals(end) && dirty;
        assertLayout(scenario, address, migrated,
            migrated ? (operations.equals(WRITE_ZERO) ? 0 : 9) : 7);
        if (operations.equals(READ + WRITE_OTHER)) {
          byte[] otherKey = newKey(address, new DataWord(1));
          assertEquals(scenario, STOP.equals(end), has(otherKey));
          if (STOP.equals(end)) {
            assertStored(scenario, otherKey, 3);
          }
        }
      }
    }
  }

  @Test
  public void nestedCallsCommitOnlyDirtySuccessfulScopes() throws Exception {
    // CALL, CALLCODE, DELEGATECALL, STATICCALL; the middle two use the caller's storage.
    for (String opcode : new String[]{"f1", "f2", "f4", "fa"}) {
      for (boolean write : new boolean[]{false, true}) {
        for (String childEnd : new String[]{STOP, REVERT_CODE, INVALID}) {
          for (boolean parentSuccess : new boolean[]{false, true}) {
            String scenario = "opcode=" + opcode + ", write=" + write + ", childEnd="
                + childEnd + ", parentSuccess=" + parentSuccess;
            byte[] child = install(READ + (write ? WRITE : "") + childEnd);
            byte[] parent = install(call(child, opcode)
                + (parentSuccess ? STOP : REVERT_CODE));
            byte[] storageOwner = opcode.equals("f2") || opcode.equals("f4") ? parent : child;
            seedLegacyRow(storageOwner);
            assertResult(scenario, parentSuccess ? SUCCESS : REVERT, parent);
            boolean migrated = parentSuccess && write && childEnd.equals(STOP)
                && !opcode.equals("fa");
            assertLayout(scenario, storageOwner, migrated, migrated ? 9 : 7);
          }
        }
      }
    }
  }

  @Test
  public void aliasReadWriteCombinationsAbortWholeTransactionWithLegacyRow() throws Exception {
    for (String first : new String[]{READ, WRITE}) {
      for (String second : new String[]{READ_ALIAS, WRITE_ALIAS}) {
        String scenario = "first=" + first + ", second=" + second;
        byte[] address = install(first + second + STOP);
        seedLegacyRow(address);
        assertResult(scenario, OUT_OF_TIME, address);
        assertLayout(scenario, address, false, 7);
        assertFalse(scenario, has(newKey(address, aliasSlot())));
      }
    }
    byte[] fresh = install(WRITE + WRITE_ALIAS + STOP);
    assertResult("no legacy row", SUCCESS, fresh);
    assertLayout("no legacy row", fresh, true, 9);
    assertStored("independent alias slot", newKey(fresh, aliasSlot()), 8);
  }

  @Test
  public void committedReadOwnershipPropagatesButRevertedReadOwnershipDoesNot() throws Exception {
    for (boolean childSuccess : new boolean[]{false, true}) {
      String scenario = "childSuccess=" + childSuccess;
      byte[] child = install(READ + (childSuccess ? STOP : REVERT_CODE));
      byte[] parent = install(call(child, "f4") + READ_ALIAS + STOP);
      seedLegacyRow(parent);
      assertResult(scenario, childSuccess ? OUT_OF_TIME : SUCCESS, parent);
      assertLayout(scenario, parent, false, 7);
      assertFalse(scenario, has(newKey(parent, aliasSlot())));
    }
    byte[] child = install(READ_ALIAS + STOP);
    byte[] parent = install(READ + call(child, "f4") + STOP);
    seedLegacyRow(parent);
    assertResult("child inherits parent's read ownership", OUT_OF_TIME, parent);
    assertLayout("child failure aborts parent", parent, false, 7);
    assertFalse(has(newKey(parent, aliasSlot())));
  }

  @Test
  public void crossTransactionReadsKeepOldRowUntilWriteAndAllowAliasesAfterMigration()
      throws Exception {
    byte[] address = install(READ + STOP);
    seedLegacyRow(address);
    assertResult("first read", SUCCESS, address);
    assertLayout("first read", address, false, 7);

    replaceCode(address, READ_ALIAS + STOP);
    assertResult("alias read in next transaction", SUCCESS, address);
    assertLayout("alias read in next transaction", address, false, 7);
    assertFalse(has(newKey(address, aliasSlot())));

    replaceCode(address, WRITE + STOP);
    assertResult("write migrates", SUCCESS, address);
    assertLayout("write migrates", address, true, 9);

    replaceCode(address, READ + WRITE_ALIAS + STOP);
    assertResult("alias after migration", SUCCESS, address);
    assertLayout("alias after migration", address, true, 9);
    assertStored("alias after migration", newKey(address, aliasSlot()), 8);
  }

  @Test
  public void constantCallDoesNotPersistMigration() throws Exception {
    byte[] address = install(WRITE + STOP);
    seedLegacyRow(address);
    TransactionContext context = new TransactionContext(null,
        new TransactionCapsule(transaction(address)), StoreFactory.getInstance(), false, true);
    VMActuator actuator = new VMActuator(true);
    actuator.validate(context);
    actuator.execute(context);
    assertNull(context.getProgramResult().getException());
    assertFalse(context.getProgramResult().isRevert());
    assertLayout("constant call", address, false, 7);
  }

  @Test
  public void revokingSuccessfulTransactionRestoresLegacyRow() throws Exception {
    byte[] address = install(WRITE + STOP);
    seedLegacyRow(address);
    try (ISession ignored = dbManager.getRevokingStore().buildSession()) {
      assertResult("before session rollback", SUCCESS, address);
      assertLayout("before session rollback", address, true, 9);
    }
    assertLayout("after session rollback", address, false, 7);
  }

  @Test
  public void proposalActivationAndRollbackRestoreFlagsAndRows() throws Exception {
    // Activate either proposal first, then activate the other. Rollback is the only disabling path.
    for (int[] transition : new int[][]{{0, 0, 1, 0}, {0, 0, 0, 1},
        {1, 0, 1, 1}, {0, 1, 1, 1}}) {
      int before99 = transition[0];
      int before102 = transition[1];
      int after99 = transition[2];
      int after102 = transition[3];
      String scenario = Arrays.toString(transition);
      setMode(before99, before102);
      boolean readMigratesBefore = before99 == 1 && before102 == 0;
      boolean readMigratesAfter = after99 == 1 && after102 == 0;

      byte[] existing = install(READ + STOP);
      seedLegacyRow(existing);
      assertResult(scenario + " existing read", SUCCESS, existing);
      assertLayout(scenario + " existing read", existing, readMigratesBefore, 7);
      byte[] reader = install(READ + STOP);
      seedLegacyRow(reader);
      byte[] writer = install(WRITE + STOP);
      seedLegacyRow(writer);

      try (ISession ignored = dbManager.getRevokingStore().buildSession()) {
        setMode(after99, after102);
        assertResult(scenario + " existing row after activation", SUCCESS, existing);
        assertLayout(scenario + " existing row after activation", existing,
            readMigratesBefore || readMigratesAfter, 7);
        assertEquals(scenario, new DataWord(7),
            repository().getStorageValue(existing, DataWord.ZERO()));
        assertResult(scenario + " read after activation", SUCCESS, reader);
        assertLayout(scenario + " read after activation", reader, readMigratesAfter, 7);
        assertResult(scenario + " write after activation", SUCCESS, writer);
        assertLayout(scenario + " write after activation", writer, true, 9);
      }

      assertEquals(scenario, before99, properties().getAllowOptimizeTvmStorage());
      assertEquals(scenario, before102, properties().getAllowOptimizeTvmStorageKey());
      assertLayout(scenario + " existing row after rollback", existing, readMigratesBefore, 7);
      assertLayout(scenario + " reader after rollback", reader, false, 7);
      assertLayout(scenario + " writer after rollback", writer, false, 7);
      // ConfigLoader must reload the rolled-back flags even though the last VM run used new flags.
      assertResult(scenario + " read after rollback", SUCCESS, reader);
      assertLayout(scenario + " read after rollback", reader, readMigratesBefore, 7);
      assertResult(scenario + " write after rollback", SUCCESS, writer);
      assertLayout(scenario + " write after rollback", writer,
          before99 == 1 || before102 == 1, 9);
    }
  }

  @Test
  public void databaseConstructorAndRepositoryIgnoreStaleVmFlags() {
    for (int proposal99 : new int[]{0, 1}) {
      for (int proposal102 : new int[]{0, 1}) {
        setMode(proposal99, proposal102);
        boolean readMigrates = proposal99 == 1 && proposal102 == 0;
        // Exercise stale flags in both directions, including a leftover constant-call snapshot.
        for (boolean staleFlag : new boolean[]{false, true}) {
          String scenario = "99=" + proposal99 + ", 102=" + proposal102 + ", stale=" + staleFlag;
          VMConfig.initAllowOptimizeTvmStorageKey(staleFlag ? 1 : 0);
          VMConfig.Snapshot local = new VMConfig.Snapshot();
          local.allowOptimizeTvmStorageKey = staleFlag;
          VMConfig.setLocalSnapshot(local);
          try {
            byte[] directAddress = install(STOP);
            seedLegacyRow(directAddress);
            Storage direct = new Storage(directAddress, dbManager.getStorageRowStore(),
                properties());
            assertEquals(scenario, new DataWord(7), direct.getValue(DataWord.ZERO()));
            direct.commit();
            assertLayout(scenario + " direct Storage", directAddress, readMigrates, 7);

            byte[] repositoryAddress = install(STOP);
            seedLegacyRow(repositoryAddress);
            Repository repository = repository();
            assertEquals(scenario, new DataWord(7),
                repository.getStorageValue(repositoryAddress, DataWord.ZERO()));
            repository.commit();
            assertLayout(scenario + " repository", repositoryAddress, readMigrates, 7);
          } finally {
            VMConfig.clearLocalSnapshot();
          }
        }
      }
    }
  }

  private DynamicPropertiesStore properties() {
    return dbManager.getDynamicPropertiesStore();
  }

  private void setMode(int proposal99, int proposal102) {
    properties().saveAllowOptimizeTvmStorage(proposal99);
    properties().saveAllowOptimizeTvmStorageKey(proposal102);
  }

  private Repository repository() {
    return RepositoryImpl.createRoot(StoreFactory.getInstance());
  }

  private byte[] install(String bytecode) {
    byte[] address = Hex.decode(String.format("41%040x", addressSequence++));
    Repository repository = repository();
    repository.createAccount(address, AccountType.Contract);
    repository.createContract(address, new ContractCapsule(SmartContract.newBuilder()
        .setContractAddress(ByteString.copyFrom(address))
        .setOriginAddress(ByteString.copyFrom(Hex.decode(OWNER_ADDRESS)))
        .setConsumeUserResourcePercent(100).build()));
    repository.saveCode(address, Hex.decode(bytecode));
    repository.commit();
    return address;
  }

  private void replaceCode(byte[] address, String bytecode) {
    Repository repository = repository();
    repository.saveCode(address, Hex.decode(bytecode));
    repository.commit();
  }

  private void setCodeHash(byte[] address, byte[] codeHash) {
    Repository repository = repository();
    ContractCapsule contract = repository.getContract(address);
    contract.setCodeHash(codeHash);
    repository.updateContract(address, contract);
    repository.commit();
  }

  private String readCodeHash(byte[] address) {
    return "73" + Hex.toHexString(Arrays.copyOfRange(address, 1, 21)) + "3f";
  }

  private Transaction transaction(byte[] address) {
    return TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
        Hex.decode(OWNER_ADDRESS), address, new byte[0], 0, 1_000_000_000L);
  }

  private void assertResult(String scenario, contractResult expected, byte[] address)
      throws Exception {
    assertEquals(scenario, expected, TvmTestUtils.processTransactionAndReturnRuntime(
        transaction(address), dbManager, null).getResult().getResultCode());
  }

  private contractResult resultFor(String end) {
    return STOP.equals(end) ? SUCCESS : (REVERT_CODE.equals(end) ? REVERT : ILLEGAL_OPERATION);
  }

  private DataWord aliasSlot() {
    return new DataWord(Hex.decode(ALIAS));
  }

  // Derive the physical keys independently of Storage, so a wrong layout cannot validate itself.
  private byte[] oldKey(byte[] address) {
    byte[] key = new byte[32];
    System.arraycopy(Hash.sha3(address), 0, key, 0, 16);
    return key;
  }

  private byte[] newKey(byte[] address, DataWord slot) {
    byte[] hash = Hash.sha3(address);
    byte[] key = new byte[48];
    System.arraycopy(hash, 0, key, 0, 16);
    System.arraycopy(Hash.sha3(ByteUtil.merge(hash, slot.getData())), 0, key, 16, 32);
    return key;
  }

  private void seedLegacyRow(byte[] address) {
    dbManager.getStorageRowStore().put(oldKey(address),
        new StorageRowCapsule(new DataWord(7).getData()));
  }

  private boolean has(byte[] key) {
    return dbManager.getStorageRowStore().has(key);
  }

  private void assertStored(String scenario, byte[] key, long expected) {
    StorageRowCapsule row = dbManager.getStorageRowStore().get(key);
    assertNotNull(scenario + " row", row);
    assertNotNull(scenario + " value", row.getValue());
    assertEquals(scenario, new DataWord(expected), new DataWord(row.getValue()));
  }

  private void assertLayout(String scenario, byte[] address, boolean migrated, long value) {
    assertEquals(scenario + " legacy row", !migrated, has(oldKey(address)));
    assertEquals(scenario + " new row", migrated, has(newKey(address, DataWord.ZERO())));
    assertStored(scenario, migrated ? newKey(address, DataWord.ZERO()) : oldKey(address), value);
  }

  private String call(byte[] target, String opcode) {
    String arguments = "6000600060006000"; // Empty input and output ranges.
    if (opcode.equals("f1") || opcode.equals("f2")) {
      arguments += "6000"; // CALL and CALLCODE also pop a value argument.
    }
    return arguments + "73" + Hex.toHexString(Arrays.copyOfRange(target, 1, 21))
        + "620186a0" + opcode + "50"; // Forward 100,000 energy; discard the child status.
  }
}
