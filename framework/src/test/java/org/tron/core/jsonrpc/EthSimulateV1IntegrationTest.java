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
import org.tron.core.capsule.AssetIssueCapsule;
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
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
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

  // ERC-7528 native pseudo-address (TRX + TRC-10 synthetic logs share it).
  private static final String ERC7528_NATIVE_LOWER =
      "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
  // keccak256("Transfer(address,address,uint256)").
  private static final String TRANSFER_TOPIC_LOWER =
      "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
  // TRC-10 testing token (>=1_000_001 required by VMConstant.MIN_TOKEN_ID).
  private static final String TRC10_TOKEN_ID = "1000001";

  private static final String OWNER_ADDRESS;
  private static final String STORAGE_TRON_ADDR_HEX;
  private static final String STORAGE_EVM_ADDR_HEX_PREFIXED;
  private static final String SINK_TRON_ADDR_HEX;
  private static final String SINK_EVM_ADDR_HEX_PREFIXED;
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
    // "Accept-anything" sink: runtime bytecode is `00` (STOP). No Solidity
    // non-payable guard, so it accepts both TRX value and TRC-10 transfers
    // without reverting. Used by the mixed TRX+TRC-10 test where the regular
    // Solidity-compiled SimpleStorage would reject msg.value > 0.
    SINK_TRON_ADDR_HEX = Wallet.getAddressPreFixString()
        + "00000000000000000000000000000000000c0de2";
    SINK_EVM_ADDR_HEX_PREFIXED = "0x00000000000000000000000000000000000c0de2";
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
    VMConfig.initAllowTvmTransferTrc10(1);
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

    // Sink contract — `00` (STOP) so it accepts arbitrary calldata, TRX value,
    // and TRC-10 transfers without reverting.
    byte[] sinkAddr = ByteArray.fromHexString(SINK_TRON_ADDR_HEX);
    rootRepository.createAccount(sinkAddr, Protocol.AccountType.Contract);
    rootRepository.createContract(sinkAddr, new ContractCapsule(
        SmartContractOuterClass.SmartContract.newBuilder()
            .setContractAddress(ByteString.copyFrom(sinkAddr))
            .build()));
    rootRepository.saveCode(sinkAddr, new byte[] {0x00});

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
   * Top-level TRC-10 transfer (depth 0): owner sends 50 units of token
   * 1000001 to the pre-deployed SimpleStorage contract, invoking get()
   * (a view function that returns the slot value — picked because it
   * doesn't revert on incoming TRC-10).
   *
   * <p>Expect exactly one synthetic log on the call result:
   * address = ERC-7528 native pseudo-address (lowercased), topic[0] =
   * keccak256("TRC10Transfer(address,address,uint256,uint256)"),
   * topic[1] = padded sender (EVM 20-byte form), topic[2] = padded
   * recipient, topic[3] = padded uint256(tokenId), data = padded
   * uint256(amount).
   */
  @Test
  public void traceTrc10TopLevelCall() throws Exception {
    seedTrc10(500L);

    CallArguments c = new CallArguments();
    c.setFrom(OWNER_ADDRESS_HEX_PREFIXED());
    c.setTo(STORAGE_EVM_ADDR_HEX_PREFIXED);
    c.setData("0x" + SEL_GET);
    c.setTokenId(TRC10_TOKEN_ID);
    c.setTokenValue("0x32"); // 50
    SimulateV1Args args = newArgs(true, false, false, c);

    SimulateCallResult call =
        tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls().get(0);

    assertEquals("0x1", call.getStatus());
    assertEquals("expected exactly one synthetic TRC10Transfer log",
        1, call.getLogs().size());

    TronJsonRpc.LogFilterElement log = call.getLogs().get(0);
    assertEquals(ERC7528_NATIVE_LOWER, log.getAddress());
    String[] topics = log.getTopics();
    assertEquals(4, topics.length);
    assertEquals(trc10TransferTopic(), topics[0]);
    assertEquals(padAddressTopic(OWNER_ADDRESS.substring(2)), topics[1]);
    assertEquals(padAddressTopic(STORAGE_EVM_ADDR_HEX_PREFIXED.substring(2)), topics[2]);
    assertEquals(padUint256Hex(1_000_001L), topics[3]);
    assertEquals(padUint256Hex(50L), log.getData());

    // No commit to disk: owner's TRC-10 balance is unchanged.
    AccountCapsule reread = dbManager.getAccountStore().get(ownerBytes);
    assertEquals(Long.valueOf(500L), reread.getAssetMapV2().get(TRC10_TOKEN_ID));
  }

  /**
   * Mixed top-level transfer: a single call with both {@code value > 0}
   * (TRX) and {@code tokenValue > 0} (TRC-10). Both synthetic logs must
   * appear in the same call result with consecutive {@code logIndex} —
   * TRX first, then TRC-10 — matching VMActuator's depth-0 emission order
   * at lines 559-569 (TRX block before TRC-10 block).
   */
  @Test
  public void traceTrc10MixedWithTrx() throws Exception {
    seedTrc10(500L);

    CallArguments c = new CallArguments();
    c.setFrom(OWNER_ADDRESS_HEX_PREFIXED());
    // Sink accepts arbitrary calldata + value; SimpleStorage would revert
    // because Solidity inlines a non-payable check on every external method.
    c.setTo(SINK_EVM_ADDR_HEX_PREFIXED);
    c.setData("0x");
    c.setValue("0x64"); // 100 sun TRX
    c.setTokenId(TRC10_TOKEN_ID);
    c.setTokenValue("0x32"); // 50 TRC-10
    SimulateV1Args args = newArgs(true, false, false, c);

    SimulateCallResult call =
        tronJsonRpc.ethSimulateV1(args, "latest").get(0).getCalls().get(0);

    assertEquals("0x1", call.getStatus());
    assertEquals("expected two synthetic transfer logs (TRX + TRC-10)",
        2, call.getLogs().size());

    TronJsonRpc.LogFilterElement trxLog = call.getLogs().get(0);
    TronJsonRpc.LogFilterElement trc10Log = call.getLogs().get(1);
    assertEquals(TRANSFER_TOPIC_LOWER, trxLog.getTopics()[0]);
    assertEquals(trc10TransferTopic(), trc10Log.getTopics()[0]);
    assertEquals(padUint256Hex(100L), trxLog.getData());
    assertEquals(padUint256Hex(50L), trc10Log.getData());
    assertEquals(padUint256Hex(1_000_001L), trc10Log.getTopics()[3]);
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

  /**
   * Register an AssetIssue for {@link #TRC10_TOKEN_ID} in the V2 store
   * (V2 keys by tokenId, matching {@code AllowSameTokenName == 1}) and
   * seed the owner's account with the requested balance. VMUtils
   * validateForSmartContract requires both the AssetIssue and a non-zero
   * owner balance to allow the transfer.
   */
  private void seedTrc10(long ownerAmount) {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1L);
    AssetIssueContract asset = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ownerBytes))
        .setName(ByteString.copyFromUtf8("TRC10"))
        .setId(TRC10_TOKEN_ID)
        .setTotalSupply(1_000_000_000L)
        .setTrxNum(1)
        .setNum(1)
        .build();
    AssetIssueCapsule cap = new AssetIssueCapsule(asset);
    dbManager.getAssetIssueV2Store().put(cap.createDbV2Key(), cap);

    AccountCapsule owner = dbManager.getAccountStore().get(ownerBytes);
    owner.setInstance(owner.getInstance().toBuilder()
        .putAssetV2(TRC10_TOKEN_ID, ownerAmount)
        .build());
    dbManager.getAccountStore().put(ownerBytes, owner);
  }

  /** Lower-case hex of keccak256("TRC10Transfer(address,address,uint256,uint256)"). */
  private static String trc10TransferTopic() {
    return "0x" + ByteArray.toHexString(org.tron.common.crypto.Hash.sha3(
        "TRC10Transfer(address,address,uint256,uint256)"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  /** Pad a 20-byte EVM address (hex without 0x) to a 32-byte topic hex string with 0x prefix. */
  private static String padAddressTopic(String evmHex20) {
    char[] zeros = new char[24];
    java.util.Arrays.fill(zeros, '0');
    return "0x" + new String(zeros) + evmHex20.toLowerCase(java.util.Locale.ROOT);
  }

  /** uint256 hex of a non-negative long, 0x-prefixed and left-padded to 32 bytes. */
  private static String padUint256Hex(long v) {
    return "0x" + padUint256(v);
  }
}
