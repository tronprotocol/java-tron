package org.tron.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;

/**
 * Regression tests for the CLOSE_EXCHANGE actuator-level gate.
 *
 * Covers:
 *  - DynamicPropertiesStore CLOSE_EXCHANGE default (missing key -> 0) and round trip.
 *  - Build phase (Wallet.createTransactionCapsule pre-validate): level 1 rejects
 *    Create/Inject/Transaction and lets Withdraw through the gate; level 2 rejects all
 *    four; level 0 adds no new rejection.
 *  - Broadcast path (pushTransaction -> processTransaction -> actuator.validate()):
 *    signed transactions are rejected with
 *    "&lt;ContractType&gt; is rejected by exchange close level &lt;level&gt;".
 *  - The legacy 4.8.0.1 ExchangeTransaction predicate is untouched (level 0 + harden 0
 *    still rejects with the legacy message) and allowHardenExchangeCalculation cannot
 *    bypass the actuator gate.
 *  - generateBlock skips closed exchange contracts but keeps packing unrelated ones;
 *    level 0 keeps ExchangeCreate packable.
 *  - The block validation path rejects blocks containing closed exchange contracts.
 */
@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloseExchangeGateTest extends BaseTest {

  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String OWNER_ADDRESS_SECOND;
  /**
   * Valid signing key for this test. config-test.conf ships an empty localwitness list, so
   * Args.getLocalWitnesses().getPrivateKey() returns null and any ECKey.fromPrivate(...) built
   * from it yields null (NPE in packedTrx/buildLocalWitnessBlock). Mirrors the established
   * fixture pattern in ManagerTest.afterInit.
   *
   * <p>The key doubles as the owner key of OWNER_ADDRESS_SECOND: the broadcast
   * (pushTransaction), packing (generateBlock) and block-apply paths run the full
   * processTransaction pipeline, which validates transaction signatures, so executable
   * transactions must be signed by the owner key itself.
   */
  private static final String OWNER_PRIVATE_KEY = PublicMethod.getRandomPrivateKey();
  private static boolean consensusStarted = false;
  /**
   * Salt for executable transactions: identical raw bytes produce an identical transaction
   * id, which would be rejected by validateDup when a transaction is packed/executed twice
   * within the same Spring context. Kept small: the salt is added to transaction amounts,
   * which must stay far below the fixture account balances for them to be executable.
   */
  private static final java.util.concurrent.atomic.AtomicLong TRX_NONCE =
      new java.util.concurrent.atomic.AtomicLong(System.nanoTime() % 100_000L);

  @Resource
  private ConsensusService consensusService;

  @Resource
  private Wallet wallet;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + ByteArray.toHexString(
            PublicMethod.getAddressByteByPrivateKey(OWNER_PRIVATE_KEY))
            .substring(Wallet.getAddressPreFixString().length());
  }

  @Before
  public void initTest() {
    // Gate level must always start from a known value; the Spring context is shared
    // between test methods of this class. The session reset guarantees the write below
    // lands at the chain root instead of a stale long-lived session layer (see
    // createExchangeOnChain).
    dbManager.getSession().reset();
    dbManager.getDynamicPropertiesStore().saveCloseExchange(0);
    dbManager.getDynamicPropertiesStore().saveAllowHardenExchangeCalculation(0);
    // Provide a valid local witness key (see OWNER_PRIVATE_KEY javadoc).
    LocalWitnesses localWitnesses = new LocalWitnesses();
    localWitnesses.setPrivateKeys(Arrays.asList(OWNER_PRIVATE_KEY));
    localWitnesses.initWitnessAccountAddress(null, true);
    Args.setLocalWitnesses(localWitnesses);
    registerLocalWitnessForConsensus();
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            10000_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            20000_000_000L);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);
  }

  // --------------------------------------------------------------------------------------------
  // DynamicPropertiesStore
  // --------------------------------------------------------------------------------------------

  @Test
  public void closeExchangeDefaultsToZeroAndRoundTrips() {
    assertEquals(0, dbManager.getDynamicPropertiesStore().getCloseExchange());
    assertFalse(dbManager.getDynamicPropertiesStore().allowHardenExchangeCalculation());

    dbManager.getDynamicPropertiesStore().saveCloseExchange(1);
    assertEquals(1, dbManager.getDynamicPropertiesStore().getCloseExchange());
    dbManager.getDynamicPropertiesStore().saveCloseExchange(2);
    assertEquals(2, dbManager.getDynamicPropertiesStore().getCloseExchange());
  }

  // --------------------------------------------------------------------------------------------
  // Build phase: Wallet.createTransactionCapsule pre-validates via actuator.validate()
  // --------------------------------------------------------------------------------------------

  @Test
  public void buildPhaseLevel0AddsNoNewRejection() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(0);
    prepareExchangeFixture();

    // ExchangeCreate validates fully at level 0 (funded fixture).
    TransactionCapsule trx = wallet.createTransactionCapsule(
        buildContractMessage(ContractType.ExchangeCreateContract),
        ContractType.ExchangeCreateContract);
    assertNotNull(trx);

    // The remaining three need an on-chain exchange in their doValidate, which this
    // fixture does not create; the gate assertion is that no close-level message fires.
    for (ContractType type : exchangeTypes()) {
      assertFalse("level 0 must not fire the close gate for " + type,
          buildPhaseRejectionContainsCloseLevel(type, 0));
    }
  }

  @Test
  public void buildPhaseLevel1RejectsCreateInjectTransactionButNotWithdraw() {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(1);

    assertBuildPhaseGateRejects(ContractType.ExchangeCreateContract, 1);
    assertBuildPhaseGateRejects(ContractType.ExchangeInjectContract, 1);
    assertBuildPhaseGateRejects(ContractType.ExchangeTransactionContract, 1);

    // Withdraw requires level 2, so it passes the gate at level 1 (its remaining
    // doValidate checks are out of scope here).
    assertFalse("Withdraw must pass the actuator gate at level 1",
        buildPhaseRejectionContainsCloseLevel(ContractType.ExchangeWithdrawContract, 1));
  }

  @Test
  public void buildPhaseLevel2RejectsAllExchangeContracts() {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(2);

    assertBuildPhaseGateRejects(ContractType.ExchangeCreateContract, 2);
    assertBuildPhaseGateRejects(ContractType.ExchangeInjectContract, 2);
    assertBuildPhaseGateRejects(ContractType.ExchangeTransactionContract, 2);
    assertBuildPhaseGateRejects(ContractType.ExchangeWithdrawContract, 2);
  }

  private void assertBuildPhaseGateRejects(ContractType type, int level) {
    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> wallet.createTransactionCapsule(buildContractMessage(type), type));
    assertEquals(type + " is rejected by exchange close level " + level, e.getMessage());
  }

  private boolean buildPhaseRejectionContainsCloseLevel(ContractType type, int level) {
    try {
      wallet.createTransactionCapsule(buildContractMessage(type), type);
      return false;
    } catch (ContractValidateException e) {
      return e.getMessage().contains("is rejected by exchange close level " + level);
    }
  }

  // --------------------------------------------------------------------------------------------
  // Broadcast path: pushTransaction -> processTransaction -> actuator.validate()
  // --------------------------------------------------------------------------------------------

  @Test
  public void pushTransactionLevel0RejectsNothingNewAndKeepsLegacyBehavior() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(0);
    ensureHeadAdvanced();

    // Level 0: the actuator gate never fires; signed transactions pass the gate stage and
    // fail (if at all) later in their own doValidate - never with a gate message.
    assertPushPassesGate(packedTrx(buildTrx(ContractType.ExchangeCreateContract)));
    assertPushPassesGate(packedTrx(buildTrx(ContractType.ExchangeInjectContract)));
    assertPushPassesGate(packedTrx(buildTrx(ContractType.ExchangeWithdrawContract)));

    // ExchangeTransaction is still handled by the untouched legacy 4.8.0.1 predicate.
    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> dbManager.pushTransaction(buildTrx(ContractType.ExchangeTransactionContract)));
    assertEquals("ExchangeTransactionContract is rejected", e.getMessage());

    // Non-exchange contract: unaffected.
    assertPushPassesGate(packedTrx(buildTransferTrx()));
  }

  @Test
  public void pushTransactionLevel1RejectsCreateInjectTransactionButNotWithdraw() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(1);
    ensureHeadAdvanced();

    assertGateRejects(ContractType.ExchangeCreateContract, 1);
    assertGateRejects(ContractType.ExchangeInjectContract, 1);
    // with harden=0 the untouched legacy 4.8.0.1 predicate fires before the actuator gate
    // and rejects with its own message; the gate message for this type is asserted in
    // hardenModeCannotBypassCloseGate.
    assertGateRejects(ContractType.ExchangeTransactionContract, 1);

    // Withdraw requires level 2, so it passes the gate at level 1 and fails later in its
    // own doValidate (this fixture has no on-chain exchange).
    assertFalse("Withdraw must pass the actuator gate at level 1",
        broadcastRejectionContainsCloseLevel(ContractType.ExchangeWithdrawContract));
  }

  @Test
  public void pushTransactionLevel2RejectsAllExchangeContracts() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(2);
    ensureHeadAdvanced();

    assertGateRejects(ContractType.ExchangeCreateContract, 2);
    assertGateRejects(ContractType.ExchangeInjectContract, 2);
    assertGateRejects(ContractType.ExchangeTransactionContract, 2);
    assertGateRejects(ContractType.ExchangeWithdrawContract, 2);

    // Non-exchange contract: unaffected even at the highest level - the transfer must
    // not be gate-rejected. (A DupTransactionException may still occur when an
    // identical-id transfer was already applied earlier in the shared context; that
    // proves the gate passed, too.)
    assertPushPassesGate(packedTrx(buildTransferTrx()));
  }

  @Test
  public void hardenModeCannotBypassCloseGate() throws Exception {
    // allowHardenExchangeCalculation=1 disables the legacy ExchangeTransaction predicate
    // (isExchangeTransaction returns false), but the actuator gate must still fire.
    dbManager.getDynamicPropertiesStore().saveAllowHardenExchangeCalculation(1);
    ensureHeadAdvanced();

    dbManager.getDynamicPropertiesStore().saveCloseExchange(1);
    assertGateRejects(ContractType.ExchangeTransactionContract, 1);

    dbManager.getDynamicPropertiesStore().saveCloseExchange(2);
    assertGateRejects(ContractType.ExchangeWithdrawContract, 2);

    // Level 0 + harden 1: neither legacy predicate nor actuator gate applies. The signed
    // transaction passes the gate stage and fails (if at all) in its own doValidate -
    // full execution would need an on-chain exchange fixture.
    dbManager.getDynamicPropertiesStore().saveCloseExchange(0);
    assertPushPassesGate(packedTrx(buildTrx(ContractType.ExchangeTransactionContract)));
  }

  private void assertGateRejects(ContractType type, int level) {
    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> dbManager.pushTransaction(packedTrx(buildRejectionTrx(type))));
    if (type == ContractType.ExchangeTransactionContract
        && !dbManager.getDynamicPropertiesStore().allowHardenExchangeCalculation()) {
      // legacy 4.8.0.1 predicate runs before the actuator gate and rejects with its own
      // message
      assertEquals("ExchangeTransactionContract is rejected", e.getMessage());
    } else {
      assertEquals(type + " is rejected by exchange close level " + level, e.getMessage());
    }
  }

  private boolean broadcastRejectionContainsCloseLevel(ContractType type) {
    try {
      dbManager.pushTransaction(packedTrx(buildRejectionTrx(type)));
      return false;
    } catch (Exception e) {
      return String.valueOf(e.getMessage()).contains("is rejected by exchange close level");
    }
  }

  /**
   * Fixtures for gate-rejection cells only need to reach the actuator gate: the gate fires
   * before every doValidate check, so the payload can be arbitrary - but the transaction id
   * must be unique per call. An id that was already executed on-chain (e.g. the withdraw
   * cell packed by an earlier test of this class) fails validateDup BEFORE the gate and
   * surfaces as DupTransactionException instead of the expected gate message.
   */
  private TransactionCapsule buildRejectionTrx(ContractType type) {
    long nonce = TRX_NONCE.incrementAndGet();
    switch (type) {
      case ExchangeInjectContract:
        return new TransactionCapsule(
            ((ExchangeInjectContract) buildContractMessage(type)).toBuilder()
                .setQuant(nonce).build(), type);
      case ExchangeTransactionContract:
        return new TransactionCapsule(
            ((ExchangeTransactionContract) buildContractMessage(type)).toBuilder()
                .setQuant(nonce).build(), type);
      case ExchangeWithdrawContract:
        return new TransactionCapsule(
            ((ExchangeWithdrawContract) buildContractMessage(type)).toBuilder()
                .setQuant(nonce).build(), type);
      default:
        // ExchangeCreateContract already carries a nonce in firstTokenBalance
        return buildTrx(type);
    }
  }

  private void assertPushPassesGate(TransactionCapsule trx) {
    // The actuator gate runs inside processTransaction, AFTER signature validation, so a
    // signed transaction that passes the gate fails (if at all) in its own doValidate -
    // never with a gate message.
    try {
      dbManager.pushTransaction(trx);
    } catch (Exception e) {
      assertFalse("transaction must pass the close-exchange gate, but got: " + e.getMessage(),
          String.valueOf(e.getMessage()).contains("is rejected by exchange close level"));
    }
  }

  private Message buildContractMessage(ContractType exchangeType) {
    ByteString owner = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND));
    switch (exchangeType) {
      case ExchangeCreateContract:
        return ExchangeCreateContract.newBuilder()
            .setOwnerAddress(owner)
            .setFirstTokenId(ByteString.copyFrom("_".getBytes()))
            .setFirstTokenBalance(100_000_000L + TRX_NONCE.incrementAndGet())
            .setSecondTokenId(ByteString.copyFrom("1".getBytes()))
            .setSecondTokenBalance(100_000_000L)
            .build();
      case ExchangeInjectContract:
        return ExchangeInjectContract.newBuilder()
            .setOwnerAddress(owner)
            .setExchangeId(1)
            .setTokenId(ByteString.copyFrom("_".getBytes()))
            .setQuant(1)
            .build();
      case ExchangeTransactionContract:
        return ExchangeTransactionContract.newBuilder()
            .setOwnerAddress(owner)
            .setExchangeId(1)
            .setTokenId(ByteString.copyFrom("_".getBytes()))
            .setQuant(1)
            .setExpected(1)
            .build();
      case ExchangeWithdrawContract:
        // withdraw 1e5 of the V2 asset "1" (the pair's second token): the withdraw ratio
        // precision guard ("Not precise enough", threshold 1e-4) requires a quant large
        // enough to absorb the pool's nonce-skew rounding error (<= 1 unit => <= 1e-5)
        return ExchangeWithdrawContract.newBuilder()
            .setOwnerAddress(owner)
            .setExchangeId(1)
            .setTokenId(ByteString.copyFrom("1".getBytes()))
            .setQuant(100_000L)
            .build();
      default:
        throw new IllegalArgumentException("unsupported type " + exchangeType);
    }
  }

  private java.util.List<ContractType> exchangeTypes() {
    return java.util.Arrays.asList(
        ContractType.ExchangeCreateContract,
        ContractType.ExchangeInjectContract,
        ContractType.ExchangeTransactionContract,
        ContractType.ExchangeWithdrawContract);
  }

  private TransactionCapsule buildTrx(ContractType exchangeType) {
    return new TransactionCapsule(buildContractMessage(exchangeType), exchangeType);
  }

  private TransactionCapsule buildTransferTrx() {
    return new TransactionCapsule(TransferContract.newBuilder()
        .setAmount(10 + TRX_NONCE.incrementAndGet())
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)))
        .build(), ContractType.TransferContract);
  }

  // --------------------------------------------------------------------------------------------
  // generateBlock: closed contracts are skipped, unrelated ones are packed
  // --------------------------------------------------------------------------------------------

  @Test
  public void generateBlockSkipsClosedExchangeButPacksUnrelated() throws Exception {
    preparePackableState(1);

    dbManager.getPendingTransactions()
        .add(packedTrx(buildTrx(ContractType.ExchangeCreateContract)));
    dbManager.getPendingTransactions()
        .add(packedTrx(buildTrx(ContractType.ExchangeWithdrawContract)));
    dbManager.getPendingTransactions().add(packedTrx(buildTransferTrx()));

    BlockCapsule block = generateBlockWithRandomWitness();

    assertNotNull(block);
    assertEquals("closed ExchangeCreateContract must be skipped when packing",
        0, countContract(block, ContractType.ExchangeCreateContract));
    assertEquals("ExchangeWithdrawContract is still allowed at level 1 and must be packed",
        1, countContract(block, ContractType.ExchangeWithdrawContract));
    assertEquals("unrelated TransferContract must still be packed",
        1, countContract(block, ContractType.TransferContract));
  }

  @Test
  public void generateBlockSkipsAllExchangeContractsAtLevel2() throws Exception {
    preparePackableState(2);

    dbManager.getPendingTransactions()
        .add(packedTrx(buildTrx(ContractType.ExchangeCreateContract)));
    dbManager.getPendingTransactions()
        .add(packedTrx(buildTrx(ContractType.ExchangeWithdrawContract)));
    dbManager.getPendingTransactions().add(packedTrx(buildTransferTrx()));

    BlockCapsule block = generateBlockWithRandomWitness();

    assertNotNull(block);
    assertEquals("ExchangeCreateContract must be skipped when packing at level 2",
        0, countContract(block, ContractType.ExchangeCreateContract));
    assertEquals("ExchangeWithdrawContract must be skipped when packing at level 2",
        0, countContract(block, ContractType.ExchangeWithdrawContract));
    assertEquals("unrelated TransferContract must still be packed",
        1, countContract(block, ContractType.TransferContract));
  }

  @Test
  public void generateBlockPacksExchangeCreateAtLevel0() throws Exception {
    preparePackableState(0);

    dbManager.getPendingTransactions()
        .add(packedTrx(buildTrx(ContractType.ExchangeCreateContract)));
    dbManager.getPendingTransactions().add(packedTrx(buildTransferTrx()));

    BlockCapsule block = generateBlockWithRandomWitness();

    assertNotNull(block);
    assertEquals("level 0 must keep the legacy behavior: ExchangeCreateContract is packable",
        1, countContract(block, ContractType.ExchangeCreateContract));
    assertEquals(1, countContract(block, ContractType.TransferContract));
  }

  /**
   * Creates exchange id 1 on-chain (via the block path, at level 0) so that
   * ExchangeWithdrawContract fixtures targeting id 1 pass their own doValidate and the
   * packing cells below measure the close gate instead of fixture gaps.
   */
  private void createExchangeOnChain() throws Exception {
    ensureHeadAdvanced();
    // The level write must land at the chain root: while a long-lived push session is
    // alive, a direct store write is captured in that session's layer, and the next
    // pushBlock's PendingManager reset() DISCARDS the layer (SnapshotManager.retreat) -
    // the write is lost and a stale level read by the block-apply path.
    dbManager.getSession().reset();
    dbManager.getDynamicPropertiesStore().saveCloseExchange(0);
    dbManager.pushBlock(
        buildLocalWitnessBlock(packedTrx(buildTrx(ContractType.ExchangeCreateContract))));
  }

  /**
   * Full state for packing-path cells: funded owner with V2 asset, exchange id 1 on-chain,
   * shared pending queue flushed, gate level persisted at the chain root (read back) and
   * flushed through a successful push.
   */
  private void preparePackableState(int level) throws Exception {
    dbManager.getSession().reset();
    prepareExchangeFixture();
    createExchangeOnChain();
    dbManager.getSession().reset();
    dbManager.getDynamicPropertiesStore().saveCloseExchange(level);
    dbManager.getPendingTransactions().clear();
    assertEquals(level, dbManager.getDynamicPropertiesStore().getCloseExchange());
    dbManager.pushTransaction(packedTrx(buildTransferTrx()));
    dbManager.getPendingTransactions().clear();
  }

  /**
   * Executable transactions reference the current head via tapos, so at least one real
   * block must exist below genesis.
   */
  private void ensureHeadAdvanced() throws Exception {
    if (chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() < 1) {
      dbManager.pushBlock(buildLocalWitnessBlock());
    }
  }

  /**
   * The packing loop runs processTransaction, which validates tapos and signatures, so a
   * transaction must be referenced to the current head, carry a valid expiration window and
   * be signed to be executable (and therefore packable).
   */
  private TransactionCapsule packedTrx(TransactionCapsule trx) {
    chainBaseManager.setBlockReference(trx);
    trx.setExpiration(chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp()
        + 60_000L);
    trx.sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    return trx;
  }

  /**
   * ExchangeCreateContract must be executable in the packing path (owner account exists,
   * holds enough TRX and enough of the V2 asset "1"), otherwise the block-packing
   * assertions above would be vacuous.
   */
  private void prepareExchangeFixture() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveLatestExchangeNum(0);

    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom("1".getBytes()))
            .setId(String.valueOf(1L))
            .build());
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(owner);
    accountCapsule.setBalance(200_000_000_000L);
    accountCapsule.addAssetV2("1".getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(owner, accountCapsule);
  }

  private BlockCapsule generateBlockWithRandomWitness() {
    String key = PublicMethod.getRandomPrivateKey();
    byte[] privateKey = ByteArray.fromHexString(key);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    WitnessCapsule witnessCapsule = new WitnessCapsule(
        ByteString.copyFrom(ecKey.getAddress()));
    Param param = Param.getInstance();
    Miner miner = param.new Miner(privateKey, witnessCapsule.getAddress(),
        witnessCapsule.getAddress());
    return dbManager.generateBlock(miner, 1533529947843L, System.currentTimeMillis() + 1000);
  }

  private int countContract(BlockCapsule block, ContractType type) {
    return (int) block.getTransactions().stream()
        .filter(trx -> trx.getInstance().getRawData().getContract(0).getType() == type)
        .count();
  }

  // --------------------------------------------------------------------------------------------
  // Block validation path (pushBlock -> processTransaction -> actuator.validate())
  // --------------------------------------------------------------------------------------------

  @Test
  public void pushBlockRejectsClosedExchangeContractInBlock() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(1);

    // advance the chain so the packed transaction has a valid reference block
    dbManager.pushBlock(buildLocalWitnessBlock());

    TransactionCapsule trx = packedTrx(buildTrx(ContractType.ExchangeCreateContract));
    BlockCapsule block = buildLocalWitnessBlock(trx);

    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> dbManager.pushBlock(block));
    assertEquals("ExchangeCreateContract is rejected by exchange close level 1", e.getMessage());
  }

  @Test
  public void pushBlockAcceptsExchangeContractAtLevel0() throws Exception {
    // preparePackableState resets the long-lived session before writing the gate level,
    // so the level (0) is visible at the chain root when the block-apply path reads it.
    preparePackableState(0);

    long headBefore = dbManager.getChainBaseManager().getDynamicPropertiesStore()
        .getLatestBlockHeaderNumber();
    // one warm-up block carrying a transfer proves the block path itself accepts
    // transactions at level 0 before the exchange block below is measured
    dbManager.pushBlock(
        buildLocalWitnessBlock(packedTrx(buildTransferTrx())));

    TransactionCapsule trx = packedTrx(buildTrx(ContractType.ExchangeCreateContract));
    BlockCapsule exBlock = buildLocalWitnessBlock(trx);
    dbManager.pushBlock(exBlock);

    assertEquals("ExchangeCreateContract must be accepted at level 0",
        headBefore + 2,
        dbManager.getChainBaseManager().getDynamicPropertiesStore()
            .getLatestBlockHeaderNumber());
  }

  @Test
  public void pushBlockRejectsWithdrawContractAtLevel2() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(2);
    ensureHeadAdvanced();

    TransactionCapsule trx = packedTrx(buildTrx(ContractType.ExchangeWithdrawContract));
    BlockCapsule block = buildLocalWitnessBlock(trx);

    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> dbManager.pushBlock(block));
    assertEquals("ExchangeWithdrawContract is rejected by exchange close level 2",
        e.getMessage());
  }

  @Test
  public void pushBlockWithoutClosedContractsStillWorksAtLevel2() throws Exception {
    dbManager.getDynamicPropertiesStore().saveCloseExchange(2);

    long headBefore = dbManager.getChainBaseManager().getDynamicPropertiesStore()
        .getLatestBlockHeaderNumber();
    dbManager.pushBlock(buildLocalWitnessBlock());

    TransactionCapsule trx = packedTrx(buildTransferTrx());
    dbManager.pushBlock(buildLocalWitnessBlock(trx));

    // both blocks must be accepted: the empty one and the one carrying the transfer
    assertEquals(headBefore + 2,
        dbManager.getChainBaseManager().getDynamicPropertiesStore()
            .getLatestBlockHeaderNumber());
  }

  /**
   * Registers the local witness as the only scheduled witness (pattern borrowed from
   * {@link ManagerForTest}) so that blocks signed by the local key pass DPoS validBlock
   * regardless of the slot, and starts the consensus service once so Manager.pushBlock
   * does not hit a null consensusInterface. The schedule is written directly (never
   * read) because at genesis the store has no active_witnesses key yet.
   */
  private void registerLocalWitnessForConsensus() {
    byte[] witnessAddressBytes = PublicMethod.getAddressByteByPrivateKey(OWNER_PRIVATE_KEY);
    ByteString witnessAddress = ByteString.copyFrom(witnessAddressBytes);
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(witnessAddress));
    dbManager.getWitnessStore().put(witnessAddressBytes, new WitnessCapsule(witnessAddress));
    // Only create the account if absent: tests may have funded this same address
    // (it is also OWNER_ADDRESS), and overwriting it would wipe the funding.
    if (!dbManager.getAccountStore().has(witnessAddressBytes)) {
      dbManager.getAccountStore().put(witnessAddressBytes,
          new AccountCapsule(Protocol.Account.newBuilder().setAddress(witnessAddress).build()));
    }
    if (!consensusStarted) {
      consensusService.start();
      consensusStarted = true;
    }
  }

  private BlockCapsule buildLocalWitnessBlock(TransactionCapsule... trxs) {
    registerLocalWitnessForConsensus();
    byte[] privateKey = ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey());
    ByteString witnessAddress = ByteString.copyFrom(ECKey.fromPrivate(privateKey).getAddress());

    long number = dbManager.getChainBaseManager().getDynamicPropertiesStore()
        .getLatestBlockHeaderNumber() + 1;
    BlockCapsule blockCapsule = new BlockCapsule(
        number,
        dbManager.getChainBaseManager().getDynamicPropertiesStore()
            .getLatestBlockHeaderHash(),
        dbManager.getChainBaseManager().getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 3000L,
        witnessAddress);
    for (TransactionCapsule trx : trxs) {
      blockCapsule.addTransaction(trx);
    }
    // generatedByMyself skips preValidateTransactionSign, so the actuator gate under test
    // is the first per-transaction check in the block validation path.
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(privateKey);
    return blockCapsule;
  }
}
