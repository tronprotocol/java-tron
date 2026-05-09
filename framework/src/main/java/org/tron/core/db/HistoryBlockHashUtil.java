package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

/**
 * TIP-2935 (EIP-2935): serve historical block hashes from state.
 *
 * <p>Approach A1 — at proposal activation, deploy the BlockHashHistory bytecode
 * and minimal contract/account metadata via direct store writes; on every block
 * (before the tx loop) write the parent block hash to slot
 * {@code (blockNum - 1) % HISTORY_SERVE_WINDOW} via {@link Storage}.
 * No VM execution is needed for {@code set()}; user contracts read via normal
 * STATICCALL which executes the deployed bytecode.
 */
@Slf4j(topic = "DB")
public class HistoryBlockHashUtil {

  public static final long HISTORY_SERVE_WINDOW = 8191L;

  // 21-byte TRON address (0x41 prefix + 20-byte EVM address 0x0000F908...2935)
  public static final byte[] HISTORY_STORAGE_ADDRESS =
      Hex.decode("410000f90827f1c53a10cb7a02335b175320002935");

  // Recovered sender of the EIP-2935 presigned (no-private-key) deploy
  // transaction on Ethereum, in TRON 21-byte form. Used as {@code originAddress}
  // on the deployed SmartContract so the deployer-of-record matches Ethereum
  // byte-for-byte; cross-chain tooling that inspects this field sees the same
  // address on both sides.
  public static final byte[] HISTORY_DEPLOYER_ADDRESS =
      Hex.decode("413462413af4609098e1e27a490f554f260213d685");

  // TIP-2935 runtime bytecode (83 bytes, no constructor prefix). Identical to
  // EIP-2935's so the same address resolves to the same code on both chains.
  public static final byte[] HISTORY_STORAGE_CODE = Hex.decode(
      "3373fffffffffffffffffffffffffffffffffffffffe"
          + "14604657602036036042575f35600143038111604257"
          + "611fff81430311604257611fff9006545f5260205ff3"
          + "5b5f5ffd5b5f35611fff60014303065500");

  public static final String HISTORY_STORAGE_NAME = "BlockHashHistory";

  // Account template for the new-account branch of {@code deploy()} (no prior
  // state at the canonical address). Equivalent to create2's
  // {@code createAccount(addr, name, Contract)}: only type, accountName, and
  // address are set. The pre-existing-account branch never uses this template
  // — it mutates the existing capsule in place to preserve balance / asset
  // state, mirroring the CREATE2 collision path. Safe to share: the proto is
  // immutable, and AccountCapsule mutations rebuild via {@code toBuilder}.
  private static final Account HISTORY_STORAGE_ACCOUNT = Account.newBuilder()
      .setType(Protocol.AccountType.Contract)
      .setAccountName(ByteString.copyFromUtf8(HISTORY_STORAGE_NAME))
      .setAddress(ByteString.copyFrom(HISTORY_STORAGE_ADDRESS))
      .build();

  // SmartContract template: every field is fixed at activation time, so the
  // proto is immutable and shared across calls. Mirrors the create2 path's
  // shape (version=0, contractAddress, consumeUserResourcePercent=100,
  // originAddress) plus a descriptive name. No trxHash since activation is
  // not a transaction.
  private static final SmartContract HISTORY_STORAGE_CONTRACT = SmartContract.newBuilder()
      .setName(HISTORY_STORAGE_NAME)
      .setContractAddress(ByteString.copyFrom(HISTORY_STORAGE_ADDRESS))
      .setOriginAddress(ByteString.copyFrom(HISTORY_DEPLOYER_ADDRESS))
      .setConsumeUserResourcePercent(100L)
      .build();

  private HistoryBlockHashUtil() {
  }

  /**
   * Deploy the TIP-2935 BlockHashHistory contract at {@code HISTORY_STORAGE_ADDRESS}.
   * If foreign code or contract metadata already sits at the canonical address,
   * logs a warning and returns without writing — the collision is deterministic
   * across nodes (same pre-state ⇒ same decision), so the proposal flag still
   * commits and chain consensus is intact. The foreign contract executes as-is
   * on every node; TIP-2935 functionality is silently absent at this address.
   * A SHA-3 pre-image of the address is the only realistic way that branch
   * fires, so it's belt-and-braces. A pre-existing non-contract account at the
   * address is the common case (anyone can transfer TRX there to activate it
   * as an EOA), so we upgrade its type to {@code Contract} in place — matching
   * the CREATE2 collision branch ({@code updateAccountType} +
   * {@code clearDelegatedResource}) and preserving balance/asset state.
   *
   * <p>Called only from {@code ProposalService} inside maintenance-time block
   * processing. Proposal validation rejects re-activation, so this runs at most
   * once per chain history; the three store writes share the block's revoking
   * session, so any node-local exception (RocksDB / IO) propagates and rolls
   * the {@code saveAllowTvmPrague(1)} write back atomically.
   */
  public static void deploy(Manager manager) {
    if (manager.getCodeStore().has(HISTORY_STORAGE_ADDRESS)
        || manager.getContractStore().has(HISTORY_STORAGE_ADDRESS)) {
      logger.warn("TIP-2935: foreign state at {}, skipping deploy",
          Hex.toHexString(HISTORY_STORAGE_ADDRESS));
      return;
    }

    manager.getCodeStore().put(HISTORY_STORAGE_ADDRESS,
        new CodeCapsule(HISTORY_STORAGE_CODE));
    manager.getContractStore().put(HISTORY_STORAGE_ADDRESS,
        new ContractCapsule(HISTORY_STORAGE_CONTRACT));

    AccountCapsule account = manager.getAccountStore().get(HISTORY_STORAGE_ADDRESS);
    boolean accountExisting = account != null;
    if (!accountExisting) {
      account = new AccountCapsule(HISTORY_STORAGE_ACCOUNT);
    } else {
      account.updateAccountType(Protocol.AccountType.Contract);
      account.clearDelegatedResource();
    }
    manager.getAccountStore().put(HISTORY_STORAGE_ADDRESS, account);

    // Flip the install marker only after all three store writes succeed; this
    // gates the per-block write() path so a skipped deploy never mutates
    // foreign storage. Any node-local exception above propagates and rolls
    // the marker back together with the partial writes via the revoking session.
    manager.getDynamicPropertiesStore().saveBlockHashHistoryInstalled(1L);

    logger.info("TIP-2935: deployed BlockHashHistory at {} (preExistingAccount={})",
        Hex.toHexString(HISTORY_STORAGE_ADDRESS), accountExisting);
  }

  /**
   * Write the parent block hash to storage at slot
   * {@code (blockNum - 1) % HISTORY_SERVE_WINDOW}. Called from
   * {@code Manager.processBlock} before the tx loop so transactions can SLOAD
   * it via STATICCALL to the deployed bytecode.
   */
  public static void write(Manager manager, BlockCapsule block) {
    // Genesis has no parent; applyBlock never invokes this for block 0, but be
    // explicit so (0-1) % 8191 = -1 in Java can never corrupt a slot.
    if (block.getNum() <= 0) {
      return;
    }
    // Defense-in-depth: deploy() skips on foreign state at the canonical
    // address, but the proposal flag still commits. Gate on the install
    // marker (set at the tail of a successful deploy()) so write() can never
    // overwrite an unrelated contract's storage. Single store hit, cached.
    if (!manager.getDynamicPropertiesStore().isBlockHashHistoryInstalled()) {
      return;
    }
    long slot = (block.getNum() - 1) % HISTORY_SERVE_WINDOW;
    Storage storage = new Storage(HISTORY_STORAGE_ADDRESS, manager.getStorageRowStore());
    storage.put(new DataWord(slot), new DataWord(block.getParentHash().getBytes()));
    storage.commit();
  }
}
