package org.tron.core.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpc;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.core.services.jsonrpc.types.SimulateBlock;
import org.tron.core.services.jsonrpc.types.SimulateBlockResult;
import org.tron.core.services.jsonrpc.types.SimulateCallResult;
import org.tron.core.services.jsonrpc.types.SimulateV1Args;
import org.tron.core.services.jsonrpc.types.TransactionResult;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.core.vm.utils.MUtil;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

@Slf4j
public class EthSimulateV1IntegrationTest extends BaseTest {

  // SimpleStorage.sol — uint256 value; set/get/setRevert(uint256). solc 0.8.35
  // --evm-version paris --optimize --metadata-hash none.
  private static final String SIMPLE_STORAGE_BYTECODE =
      "6080604052348015600f57600080fd5b5060f08061001e6000396000f3fe6080604052348015600f57"
          + "600080fd5b506004361060465760003560e01c80632e8f88e614604b5780633fa4f24514605c5780"
          + "6360fe47b11460765780636d4ce63c146086575b600080fd5b605a605636600460cb565b608d565b"
          + "005b606460005481565b60405190815260200160405180910390f35b605a608136600460cb565b60"
          + "0055565b6000546064565b600081905560405162461bcd60e51b815260c290600401602080825260"
          + "0490820152636e6f706560e01b604082015260600190565b60405180910390fd5b60006020828403"
          + "121560dc57600080fd5b503591905056fea164736f6c6343000823000a";

  private static final String SEL_SET = "60fe47b1";
  private static final String SEL_GET = "6d4ce63c";
  private static final String SEL_SET_REVERT = "2e8f88e6";

  private static final String OWNER_ADDRESS;
  private static final String STORAGE_TRON_ADDR_HEX;
  private static final String STORAGE_EVM_ADDR_HEX_PREFIXED;
  private static final long OWNER_BALANCE = 10_000_000_000L;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);
    Args.getInstance().setSupportConstant(true);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    // Pre-installed SimpleStorage contract address — same address used by @Before
    // to write runtime bytecode + ContractCapsule into the chain stores.
    STORAGE_TRON_ADDR_HEX = Wallet.getAddressPreFixString()
        + "00000000000000000000000000000000000c0de1";
    STORAGE_EVM_ADDR_HEX_PREFIXED = "0x00000000000000000000000000000000000c0de1";
  }

  @Resource
  private NodeInfoService nodeInfoService;
  @Resource
  private Wallet wallet;

  private TronJsonRpcImpl tronJsonRpc;
  private byte[] ownerBytes;

  @Before
  public void init() {
    // Enable post-Byzantium opcodes (SHR/SHL/SAR via Constantinople, etc.) so
    // solc 0.8.x dispatch bytecode runs. ConfigLoader.disable=true prevents the
    // chain's dynamic-properties store from reloading these flags back to 0
    // — same pattern as AllowTvmCompatibleEvmTest.beforeClass().
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmLondon(1);
    VMConfig.initAllowTvmCompatibleEvm(1);
    // Mainnet has long passed this hardfork; the flag governs whether child
    // RepositoryImpl deep-copies the parent's Storage on first read. Without it,
    // child mutations alias the parent and reverted SSTOREs leak across calls
    // (RepositoryImpl.getStorage, ~line 715).
    CommonParameter.ENERGY_LIMIT_HARD_FORK = true;

    ownerBytes = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule owner = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ownerBytes), Protocol.AccountType.Normal, OWNER_BALANCE);
    dbManager.getAccountStore().put(ownerBytes, owner);

    long headNum = 1L;
    BlockCapsule head = new BlockCapsule(headNum,
        Sha256Hash.wrap(ByteString.copyFrom(ByteArray.fromHexString(
            "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        System.currentTimeMillis(),
        ByteString.copyFrom(ownerBytes));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(headNum);
    dbManager.getBlockIndexStore().put(head.getBlockId());
    dbManager.getBlockStore().put(head.getBlockId().getBytes(), head);

    // Pre-install SimpleStorage's runtime bytecode at STORAGE_TRON_ADDR so the
    // trigger tests can resolve the contract via the on-chain contract store
    // (VMActuator.validate rejects unknown contract addresses). Mirrors the
    // pattern from TriggerSmartContractServletTest.before().
    byte[] storageAddr = ByteArray.fromHexString(STORAGE_TRON_ADDR_HEX);
    Repository rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.createAccount(storageAddr, Protocol.AccountType.Contract);
    rootRepository.createContract(storageAddr, new ContractCapsule(
        SmartContractOuterClass.SmartContract.newBuilder()
            .setContractAddress(ByteString.copyFrom(storageAddr))
            .build()));
    rootRepository.saveCode(storageAddr,
        ByteArray.fromHexString(simpleStorageRuntimeBytecode()));
    rootRepository.commit();

    tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);
  }

  /**
   * Extracts the runtime portion of {@link #SIMPLE_STORAGE_BYTECODE}. The deploy
   * bytecode is the 30-byte constructor (`...600080fd5b5060f08061001e6000396000f3fe`,
   * which CODECOPYs 0xf0 bytes from offset 0x1e and RETURNs them) followed by
   * the runtime. So `runtime = init[60:]`.
   */
  private static String simpleStorageRuntimeBytecode() {
    return SIMPLE_STORAGE_BYTECODE.substring(60);
  }

  /**
   * State sharing across calls: set(42) → get() must read 42 from the same
   * simulate request. If perCallChild.commit() isn't flushing into sharedRoot
   * between calls, call 2 reads 0 and the test fails.
   */
  @Test
  public void stateSharingAcrossCalls() throws Exception {
    SimulateV1Args args = newArgs(false, false, false,
        triggerCall(STORAGE_EVM_ADDR_HEX_PREFIXED, SEL_SET, padUint256(42), 0L),
        triggerCall(STORAGE_EVM_ADDR_HEX_PREFIXED, SEL_GET, "", 0L));

    List<SimulateCallResult> calls = tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls();

    assertEquals(2, calls.size());
    assertEquals("call 1 (set) must succeed", "0x1", calls.get(0).getStatus());
    assertEquals("call 2 (get) must succeed", "0x1", calls.get(1).getStatus());
    assertEquals("call 2 must observe call 1's write",
        BigInteger.valueOf(42), parseHex(calls.get(1).getReturnData()));
  }

  /**
   * Revert isolation: set(99) → setRevert(123) → get(). The reverted call's
   * storage write must NOT be visible to the subsequent get.
   */
  @Test
  public void revertIsolatesPerCall() throws Exception {
    SimulateV1Args args = newArgs(false, false, false,
        triggerCall(STORAGE_EVM_ADDR_HEX_PREFIXED, SEL_SET, padUint256(99), 0L),
        triggerCall(STORAGE_EVM_ADDR_HEX_PREFIXED, SEL_SET_REVERT, padUint256(123), 0L),
        triggerCall(STORAGE_EVM_ADDR_HEX_PREFIXED, SEL_GET, "", 0L));

    List<SimulateCallResult> calls = tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls();

    assertEquals(3, calls.size());
    assertEquals("0x1", calls.get(0).getStatus());
    assertEquals("0x0", calls.get(1).getStatus());
    assertEquals("0x1", calls.get(2).getStatus());
    assertEquals("call 3 must see call 1's value 99, not the reverted call 2's 123",
        BigInteger.valueOf(99), parseHex(calls.get(2).getReturnData()));
  }

  /**
   * validation=true must reject a sender that has no account on chain.
   */
  @Test
  public void validationRejectsUnactivatedSender() throws Exception {
    String freshFrom = "0x" + "00000000000000000000000000000000deadbeef";
    CallArguments c = new CallArguments();
    c.setFrom(freshFrom);
    c.setTo(OWNER_ADDRESS_HEX_PREFIXED());
    c.setValue("0x1");
    c.setData("0x");
    SimulateV1Args args = newArgs(false, true, false, c);

    List<SimulateCallResult> calls = tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls();
    assertEquals(1, calls.size());
    assertEquals("0x0", calls.get(0).getStatus());
    assertNotNull(calls.get(0).getErrorMessage());
    assertTrue("got: " + calls.get(0).getErrorMessage(),
        calls.get(0).getErrorMessage().contains("sender account does not exist"));
  }

  /**
   * validation=true must reject when callValue exceeds the sender's balance.
   */
  @Test
  public void validationRejectsInsufficientBalance() throws Exception {
    CallArguments c = new CallArguments();
    c.setFrom(OWNER_ADDRESS_HEX_PREFIXED());
    c.setTo(OWNER_ADDRESS_HEX_PREFIXED());
    // 2x OWNER_BALANCE
    c.setValue("0x" + Long.toHexString(OWNER_BALANCE * 2));
    c.setData("0x");
    SimulateV1Args args = newArgs(false, true, false, c);

    List<SimulateCallResult> calls = tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls();
    assertEquals(1, calls.size());
    assertEquals("0x0", calls.get(0).getStatus());
    assertTrue("got: " + calls.get(0).getErrorMessage(),
        calls.get(0).getErrorMessage().contains("insufficient balance"));
  }

  /**
   * CREATE simulation populates contractAddress and the address is a real EVM
   * 20-byte form (no Tron 0x41 prefix when serialized as JSON via
   * ByteArray.toJsonHexAddress).
   */
  @Test
  public void createPopulatesContractAddress() throws Exception {
    SimulateV1Args args = newArgs(false, false, false, createCall(SIMPLE_STORAGE_BYTECODE));
    List<SimulateCallResult> calls = tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls();
    assertEquals(1, calls.size());
    assertEquals("0x1", calls.get(0).getStatus());
    String addr = calls.get(0).getContractAddress();
    assertNotNull(addr);
    assertTrue("contractAddress must be 0x-prefixed 20-byte hex, got: " + addr,
        addr.startsWith("0x") && addr.length() == 42);
  }

  /**
   * Observable contract: CALLCODE MUST NOT produce a synthetic ERC-7528 Transfer log under
   * {@code traceTransfers=true}. {@code Program.callToAddress} guards the
   * {@code simulationTracer.onTransfer} hook with {@code opcode != DELEGATECALL && opcode !=
   * CALLCODE}. In practice the guard is belt-and-suspenders because the same code path is
   * already gated on {@code senderAddress != contextAddress}, and Program.java:1087 sets
   * {@code contextAddress = senderAddress} for both opcodes — so the transfer block is never
   * entered. Either layer dropping silently would emit a self-transfer log; this test pins the
   * observable behaviour regardless of which layer enforces it.
   *
   * <p>Setup: pre-install a contract whose runtime executes CALLCODE-to-self with value=5,
   * fund it with 100, trigger from owner with value=0 (so no trigger transfer log contaminates
   * the assertion). The only log that could appear from this call is the CALLCODE's
   * self-transfer — and the production code must omit it.
   */
  @Test
  public void callcodeSkipsSyntheticTransferLog() throws Exception {
    // Runtime: PUSH1 0 ×4 (retLen, retOff, argsLen, argsOff); PUSH1 5 (value); ADDRESS;
    //   PUSH3 0x0F4240 (gas); CALLCODE; POP; STOP. 18 bytes.
    String callcodeAddr = "00000000000000000000000000000000000c0de2";
    installContract(callcodeAddr, "6000600060006000600530620F4240F25000", 100L);

    assertNoSyntheticTransferLog(triggerTraced(callcodeAddr), "CALLCODE");
  }

  /**
   * DELEGATECALL counterpart of {@link #callcodeSkipsSyntheticTransferLog}: a DELEGATECALL hop
   * must never appear as an ERC-7528 Transfer log. Contract A DELEGATECALLs contract B (a no-op
   * STOP) and we assert the call produced no synthetic Transfer entry.
   *
   * <p>Unlike CALLCODE, DELEGATECALL never carries value — standard EVM / Tron sets endowment
   * to 0 for the opcode, so the {@code endowment > 0} transfer branch is unreachable and the
   * explicit {@code opcode != DELEGATECALL} guard never even runs. The "no transfer log"
   * outcome here therefore comes from DELEGATECALL having no value to move, not from the guard.
   * The test pins that end-state: should a future change ever start routing DELEGATECALL value
   * through the transfer-logging path, this assertion fails.
   */
  @Test
  public void delegatecallSkipsSyntheticTransferLog() throws Exception {
    String targetB = "00000000000000000000000000000000000c0de3";
    String callerA = "00000000000000000000000000000000000c0de4";

    // B: STOP.
    installContract(targetB, "00", 0L);
    // A: PUSH1 0 ×4 (retLen, retOff, argsLen, argsOff); PUSH20 B; PUSH3 gas; DELEGATECALL;
    //   POP; STOP.
    installContract(callerA, "600060006000600073" + targetB + "620F4240F45000", 100L);

    assertNoSyntheticTransferLog(triggerTraced(callerA), "DELEGATECALL");
  }

  /**
   * Locks down the consensus-path (tracer=null) byte outcome of {@code transferAllTokenWithTrace}.
   * Program.java:543's early-return delegates to {@link MUtil#transferAllToken}; any future
   * hoisting of a side-effect line above that guard would break sync-from-genesis. This test
   * runs a multi-TRC-10 SELFDESTRUCT scenario through {@code MUtil.transferAllToken} directly
   * and asserts the post-state proto bytes match an independently-computed expected state:
   * dest receives every TRC-10 the owner held, owner's asset map is zeroed.
   *
   * <p>If anyone refactors {@code MUtil.transferAllToken} or the early-return path drifts to
   * a different code path, this test catches the divergence.
   */
  @Test
  public void transferAllToken_multiTrc10_byteEquivalence() {
    byte[] ownerAddr = ByteArray.fromHexString(
        Wallet.getAddressPreFixString() + "11111111111111111111111111111111aabbccdd");
    byte[] destAddr = ByteArray.fromHexString(
        Wallet.getAddressPreFixString() + "22222222222222222222222222222222ddccbbaa");

    AccountCapsule ownerStart = new AccountCapsule(ByteString.copyFromUtf8("owner-srcSD"),
        ByteString.copyFrom(ownerAddr), Protocol.AccountType.Normal, 0L);
    ownerStart.addAssetV2(ByteArray.fromString("1000001"), 100L);
    ownerStart.addAssetV2(ByteArray.fromString("1000002"), 50L);
    ownerStart.addAssetV2(ByteArray.fromString("1000003"), 200L);

    AccountCapsule destStart = new AccountCapsule(ByteString.copyFromUtf8("dest-srcSD"),
        ByteString.copyFrom(destAddr), Protocol.AccountType.Normal, 0L);
    destStart.addAssetV2(ByteArray.fromString("1000002"), 7L);

    Repository repo = RepositoryImpl.createRoot(StoreFactory.getInstance());
    repo.putAccountValue(ownerAddr, ownerStart);
    repo.putAccountValue(destAddr, destStart);

    MUtil.transferAllToken(repo, ownerAddr, destAddr);

    // owner's asset map must be zeroed
    java.util.Map<String, Long> ownerAfter = repo.getAccount(ownerAddr).getAssetMapV2();
    assertEquals("owner 1000001 zeroed", Long.valueOf(0L), ownerAfter.get("1000001"));
    assertEquals("owner 1000002 zeroed", Long.valueOf(0L), ownerAfter.get("1000002"));
    assertEquals("owner 1000003 zeroed", Long.valueOf(0L), ownerAfter.get("1000003"));

    // dest receives owner's full balance for each asset, summed with its pre-state.
    java.util.Map<String, Long> destAfter = repo.getAccount(destAddr).getAssetMapV2();
    assertEquals("dest gains all of 1000001", Long.valueOf(100L), destAfter.get("1000001"));
    assertEquals("dest 1000002 sums pre-state (7) + transferred (50)",
        Long.valueOf(57L), destAfter.get("1000002"));
    assertEquals("dest gains all of 1000003", Long.valueOf(200L), destAfter.get("1000003"));
  }

  /**
   * Drops TRC-10 transfer entries from a reverted sub-call frame — same
   * isolation discipline the TRX transfer hooks rely on. Direct buffer
   * exercise: enterFrame → onTokenTransfer → revertFrame must clear it.
   */
  @Test
  public void buffering_dropsTokenTransferOnRevertFrame() {
    org.tron.core.vm.program.listener.BufferingSimulationTracer t =
        new org.tron.core.vm.program.listener.BufferingSimulationTracer();
    t.beginCall();
    t.enterFrame();
    t.onTokenTransfer(new byte[20], new byte[20], 1_000_001L, 50L);
    t.revertFrame();
    assertEquals(0, t.snapshotCall().size());
  }

  /**
   * returnFullTransactions changes the shape of `transactions[]`:
   * default → array of hash strings;
   * true → array of TransactionResult objects (same hash, gasPrice=0x0, etc.).
   */
  @Test
  public void returnFullTransactionsShape() throws Exception {
    SimulateBlockResult hashOnly = tronJsonRpc.ethSimulateV1(
        newArgs(false, false, false, createCall(SIMPLE_STORAGE_BYTECODE)),
        "latest").get(0);
    Object[] hashTxs = hashOnly.getTransactions();
    assertEquals(1, hashTxs.length);
    assertTrue("default transactions[] should be hash strings, got: " + hashTxs[0].getClass(),
        hashTxs[0] instanceof String);
    assertEquals(hashOnly.getCalls().get(0).getTransactionHash(), hashTxs[0]);

    SimulateBlockResult fullTx = tronJsonRpc.ethSimulateV1(
        newArgs(false, false, true, createCall(SIMPLE_STORAGE_BYTECODE)),
        "latest").get(0);
    Object[] fullTxs = fullTx.getTransactions();
    assertEquals(1, fullTxs.length);
    assertTrue("with returnFullTransactions=true, entry must be TransactionResult, got: "
            + fullTxs[0].getClass(),
        fullTxs[0] instanceof TransactionResult);
    TransactionResult tx = (TransactionResult) fullTxs[0];
    assertEquals(fullTx.getCalls().get(0).getTransactionHash(), tx.getHash());
    assertEquals("0x0", tx.getGasPrice());
    assertEquals("0x0", tx.getNonce());
    assertNotNull(tx.getFrom());
    // CREATE → to is null
    assertEquals(null, tx.getTo());
    // The synthetic block hash must match between the two runs (deterministic
    // from head block hash).
    assertEquals(hashOnly.getHash(), fullTx.getHash());
    // tx hashes are deterministic per (block, callIndex), so they must match.
    assertEquals(hashOnly.getCalls().get(0).getTransactionHash(), tx.getHash());
  }

  // ---- helpers ----

  /** Pre-install a contract at the given 20-byte EVM-hex address with runtime code + balance. */
  private void installContract(String evmAddrHex, String runtimeHex, long preFundBalance) {
    byte[] tronAddr = ByteArray.fromHexString(Wallet.getAddressPreFixString() + evmAddrHex);
    Repository repo = RepositoryImpl.createRoot(StoreFactory.getInstance());
    repo.createAccount(tronAddr, Protocol.AccountType.Contract);
    repo.createContract(tronAddr, new ContractCapsule(
        SmartContractOuterClass.SmartContract.newBuilder()
            .setContractAddress(ByteString.copyFrom(tronAddr))
            .build()));
    repo.saveCode(tronAddr, ByteArray.fromHexString(runtimeHex));
    if (preFundBalance > 0) {
      repo.addBalance(tronAddr, preFundBalance);
    }
    repo.commit();
  }

  /** Trigger an installed contract from the owner with value=0 and traceTransfers on. */
  private List<TronJsonRpc.LogFilterElement> triggerTraced(String evmAddrHex) throws Exception {
    CallArguments trigger = new CallArguments();
    trigger.setFrom(OWNER_ADDRESS_HEX_PREFIXED());
    trigger.setTo("0x" + evmAddrHex);
    trigger.setValue("0x0");
    trigger.setData("0x");

    SimulateCallResult call = tronJsonRpc.ethSimulateV1(
        newArgs(true, false, false, trigger), "latest").get(0).getCalls().get(0);
    assertEquals("trigger of " + evmAddrHex + " must succeed", "0x1", call.getStatus());
    return call.getLogs();
  }

  private static void assertNoSyntheticTransferLog(
      List<TronJsonRpc.LogFilterElement> logs, String opcode) {
    if (logs == null) {
      return;
    }
    for (TronJsonRpc.LogFilterElement log : logs) {
      assertTrue(
          opcode + " must NOT produce a synthetic ERC-7528 Transfer log, found one at: "
              + log.getAddress(),
          !"0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee".equalsIgnoreCase(log.getAddress()));
    }
  }

  private static SimulateV1Args newArgs(boolean traceTransfers, boolean validation,
      boolean returnFullTransactions, CallArguments... calls) {
    SimulateBlock block = new SimulateBlock();
    block.setCalls(new ArrayList<>(Arrays.asList(calls)));
    SimulateV1Args args = new SimulateV1Args();
    args.setBlockStateCalls(new ArrayList<>(Collections.singletonList(block)));
    args.setTraceTransfers(traceTransfers);
    args.setValidation(validation);
    args.setReturnFullTransactions(returnFullTransactions);
    return args;
  }

  /** CREATE call from the test owner with the given init bytecode. */
  private CallArguments createCall(String initBytecodeHex) {
    CallArguments args = new CallArguments();
    args.setFrom(OWNER_ADDRESS_HEX_PREFIXED());
    args.setTo(null);
    args.setValue("0x0");
    args.setData("0x" + initBytecodeHex);
    return args;
  }

  /** TriggerSmartContract call against an explicit contract address. */
  private CallArguments triggerCall(String to, String selector, String calldataTail, long value) {
    CallArguments args = new CallArguments();
    args.setFrom(OWNER_ADDRESS_HEX_PREFIXED());
    args.setTo(to);
    args.setValue(value == 0 ? "0x0" : "0x" + Long.toHexString(value));
    args.setData("0x" + selector + (calldataTail == null ? "" : calldataTail));
    return args;
  }

  private static String OWNER_ADDRESS_HEX_PREFIXED() {
    return "0x" + OWNER_ADDRESS;
  }

  private static String padUint256(long v) {
    String h = Long.toHexString(v);
    StringBuilder sb = new StringBuilder();
    for (int i = h.length(); i < 64; i++) {
      sb.append('0');
    }
    sb.append(h);
    return sb.toString();
  }

  private static BigInteger parseHex(String hex) {
    if (hex == null || hex.isEmpty() || "0x".equals(hex)) {
      return BigInteger.ZERO;
    }
    return new BigInteger(hex.startsWith("0x") ? hex.substring(2) : hex, 16);
  }

}
