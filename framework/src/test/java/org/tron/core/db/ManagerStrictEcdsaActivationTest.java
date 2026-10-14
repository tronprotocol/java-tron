package org.tron.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ReflectUtils;
import org.tron.consensus.Consensus;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Proposal.State;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

/**
 * Exercises real block processing, proposal approval, revoking stores and signature verification.
 * DPoS scheduling/application is stubbed so these tests isolate the ECDSA activation boundary.
 */
public class ManagerStrictEcdsaActivationTest extends BaseMethodTest {

  private static final long ACTIVATION_TIME = 1533529947000L;
  private static final byte[] INFINITY_ADDRESS =
      ByteArray.fromHexString("41dcc703c0e500b653ca82273b7bfad8045d85a470");
  private final ECKey witnessKey = ECKey.fromPrivate(BigInteger.ONE);
  private final ECKey ownerKey = ECKey.fromPrivate(BigInteger.valueOf(2));
  private DynamicPropertiesStore properties;
  private Consensus consensus;
  private BlockCapsule parent;

  @Override
  protected void afterInit() {
    // Prevent background re-push from draining the queues under test.
    dbManager.stopRePushThread();
    properties = dbManager.getDynamicPropertiesStore();
    properties.saveAllowStrictEcdsaValidation(0);
    properties.saveAllowMultiSign(1);
    properties.saveNextMaintenanceTime(ACTIVATION_TIME);
    consensus = mock(Consensus.class);
    when(consensus.validBlock(any())).thenReturn(true);
    when(consensus.applyBlock(any())).thenReturn(true);
    ReflectUtils.setFieldValue(dbManager, "consensus", consensus);

    ByteString witness = ByteString.copyFrom(witnessKey.getAddress());
    dbManager.getAccountStore().put(witness.toByteArray(),
        new AccountCapsule(Account.newBuilder().setAddress(witness).build()));
    dbManager.getWitnessStore().put(witness.toByteArray(), new WitnessCapsule(witness));
    ByteString otherWitness = ByteString.copyFrom(ownerKey.getAddress());
    dbManager.getWitnessStore().put(otherWitness.toByteArray(), new WitnessCapsule(otherWitness));
    // Two witnesses require one approval; with only one, the integer threshold would be zero.
    dbManager.getWitnessScheduleStore().saveActiveWitnesses(Arrays.asList(witness, otherWitness));
    Account owner = Account.newBuilder().setAddress(ByteString.copyFrom(ownerKey.getAddress()))
        .setBalance(1_000_000_000L)
        .setOwnerPermission(AccountCapsule.getDefaultPermission(
            ByteString.copyFrom(INFINITY_ADDRESS))).build();
    dbManager.getAccountStore().put(ownerKey.getAddress(), new AccountCapsule(owner));
  }

  @Test
  public void shouldActivateAfterHAndRejectCachedLegacySignaturesInHPlusOne() throws Exception {
    prepareProposal(true);
    List<TransactionCapsule> queued = populateQueues();
    TransactionCapsule inH = legacyTransaction(10);
    assertFalse(inH.isVerified());
    BlockCapsule h = nextBlock(ACTIVATION_TIME, inH);

    dbManager.pushBlock(h);

    assertTrue(properties.allowStrictEcdsaValidation());
    assertEquals(State.APPROVED, proposal().getState());
    assertTrue(dbManager.getTransactionStore().has(inH.getTransactionId().getBytes()));
    assertEquals(h.getBlockId(), chainBaseManager.getHeadBlockId());
    for (TransactionCapsule tx : queued) {
      assertFalse("Activation must invalidate every queue", tx.isVerified());
      // Exercise reuse of the original cached capsule, not a fresh unverified replacement.
      BlockCapsule next = nextBlock(ACTIVATION_TIME + 3000, tx);
      assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(next));
      assertFalse(dbManager.getTransactionStore().has(tx.getTransactionId().getBytes()));
      assertEquals(h.getBlockId(), chainBaseManager.getHeadBlockId());
    }
  }

  @Test
  public void shouldNotInvalidateWhenAlreadyEnabled() throws Exception {
    prepareProposal(true);
    properties.saveAllowStrictEcdsaValidation(1);
    List<TransactionCapsule> queued = populateValidQueues();

    dbManager.pushBlock(nextBlock(ACTIVATION_TIME));

    assertEquals(State.APPROVED, proposal().getState());
    assertTrue(properties.allowStrictEcdsaValidation());
    for (TransactionCapsule tx : queued) {
      assertTrue("An already-enabled flag is not an activation transition", tx.isVerified());
    }
  }

  @Test
  public void shouldNotInvalidateWhenProposalIsDisapproved() throws Exception {
    prepareProposal(false);
    List<TransactionCapsule> queued = populateQueues();

    dbManager.pushBlock(nextBlock(ACTIVATION_TIME));

    assertEquals(State.DISAPPROVED, proposal().getState());
    assertFalse(properties.allowStrictEcdsaValidation());
    for (TransactionCapsule tx : queued) {
      assertTrue(tx.isVerified());
    }
  }

  @Test
  public void shouldRestoreLegacyRulesWhenActivationBlockFails() throws Exception {
    prepareProposal(true);
    List<TransactionCapsule> queued = populateQueues();
    when(consensus.applyBlock(any())).thenAnswer(call -> {
      assertTrue(properties.allowStrictEcdsaValidation());
      assertEquals(State.APPROVED, proposal().getState());
      for (TransactionCapsule tx : queued) {
        assertFalse(tx.isVerified());
      }
      return false;
    });

    assertThrows(BadBlockException.class,
        () -> dbManager.pushBlock(nextBlock(ACTIVATION_TIME)));

    assertRolledBack(queued);
  }

  @Test
  public void shouldRestoreLegacyRulesWhenActivationBlockIsErased() throws Exception {
    prepareProposal(true);
    List<TransactionCapsule> queued = populateQueues();
    dbManager.pushBlock(nextBlock(ACTIVATION_TIME));
    assertTrue(properties.allowStrictEcdsaValidation());

    dbManager.eraseBlock();

    assertRolledBack(queued);
    // Reapplying the same approved proposal must invalidate the freshly reverified caches again.
    dbManager.pushBlock(nextBlock(ACTIVATION_TIME));
    assertTrue(properties.allowStrictEcdsaValidation());
    for (TransactionCapsule tx : queued) {
      assertFalse(tx.isVerified());
    }
  }

  @Test
  public void shouldInvalidateInFlightAdmissionBeforeItCanReuseLegacyVerification()
      throws Exception {
    prepareProposal(true);
    CountDownLatch verifiedWithLegacyRules = new CountDownLatch(1);
    CountDownLatch finishVerification = new CountDownLatch(1);
    CountDownLatch invalidationStarted = new CountDownLatch(1);
    TransactionCapsule tx = new TransactionCapsule(legacyTransaction(20).getInstance()) {
      @Override
      public boolean validatePubSignature(AccountStore accounts, DynamicPropertiesStore store)
          throws ValidateSignatureException {
        boolean result = super.validatePubSignature(accounts, store);
        verifiedWithLegacyRules.countDown();
        try {
          if (!finishVerification.await(10, TimeUnit.SECONDS)) {
            throw new ValidateSignatureException("Timed out waiting for activation");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new ValidateSignatureException("Verification interrupted");
        }
        return result;
      }

      @Override
      public void setVerified(boolean verified) {
        if (!verified) {
          invalidationStarted.countDown();
        }
        super.setVerified(verified);
      }
    };
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> admission = workers.submit(() -> dbManager.pushTransaction(tx));
      assertTrue(verifiedWithLegacyRules.await(10, TimeUnit.SECONDS));
      BlockCapsule h = nextBlock(ACTIVATION_TIME);
      Future<?> activation = workers.submit(() -> {
        dbManager.pushBlock(h);
        return null;
      });
      assertTrue(invalidationStarted.await(10, TimeUnit.SECONDS));
      assertFalse("Activation must wait for the capsule's verification lock", activation.isDone());
      finishVerification.countDown();
      activation.get(10, TimeUnit.SECONDS);
      ExecutionException failure = assertThrows(ExecutionException.class,
          () -> admission.get(10, TimeUnit.SECONDS));
      assertTrue(failure.getCause() instanceof ValidateSignatureException);
      assertTrue(properties.allowStrictEcdsaValidation());
      assertEquals(h.getBlockId(), chainBaseManager.getHeadBlockId());
      assertFalse(tx.isVerified());
      assertFalse(dbManager.getPendingTransactions().contains(tx));
      assertFalse(dbManager.getTransactionStore().has(tx.getTransactionId().getBytes()));
      assertTrue(pushingQueue().isEmpty());
    } finally {
      finishVerification.countDown();
      workers.shutdownNow();
      assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @Test
  public void shouldPreserveSupportedPaddingAcrossActivation() throws Exception {
    prepareProposal(true);
    AccountCapsule owner = dbManager.getAccountStore().get(ownerKey.getAddress());
    owner.setInstance(owner.getInstance().toBuilder().setOwnerPermission(
        AccountCapsule.getDefaultPermission(ByteString.copyFrom(ownerKey.getAddress()))).build());
    dbManager.getAccountStore().put(ownerKey.getAddress(), owner);

    // Legacy block validation also accepts padding beyond the new strict upper bound.
    TransactionCapsule[] inH = new TransactionCapsule[5];
    for (int i = 0; i < inH.length; i++) {
      inH[i] = paddedTransaction(i + 1, 65 + i);
    }
    dbManager.pushBlock(nextBlock(ACTIVATION_TIME, inH));
    assertTrue(properties.allowStrictEcdsaValidation());

    TransactionCapsule overlong = paddedTransaction(10, 69);
    assertThrows(ValidateSignatureException.class,
        () -> dbManager.pushBlock(nextBlock(ACTIVATION_TIME + 3000, overlong)));
    assertFalse(dbManager.getTransactionStore().has(overlong.getTransactionId().getBytes()));

    TransactionCapsule[] inNext = new TransactionCapsule[4];
    for (int i = 0; i < inNext.length; i++) {
      inNext[i] = paddedTransaction(20 + i, 65 + i);
    }
    BlockCapsule next = nextBlock(ACTIVATION_TIME + 3000, inNext);
    dbManager.pushBlock(next);
    assertEquals(next.getBlockId(), chainBaseManager.getHeadBlockId());
    for (TransactionCapsule[] transactions : Arrays.asList(inH, inNext)) {
      for (TransactionCapsule tx : transactions) {
        assertEquals(tx.getInstance().getSignatureList(), dbManager.getTransactionStore()
            .get(tx.getTransactionId().getBytes()).getInstance().getSignatureList());
      }
    }
  }

  private TransactionCapsule paddedTransaction(long amount, int length) throws Exception {
    TransactionCapsule tx = unsignedTransaction(amount);
    byte[] signature = Arrays.copyOf(
        ownerKey.sign(tx.getTransactionId().getBytes()).toByteArray(), length);
    Arrays.fill(signature, 65, length, (byte) 0x7f);
    return signedTransaction(tx, signature);
  }

  private void prepareProposal(boolean approved) throws Exception {
    parent = nextBlock(ACTIVATION_TIME - 3000);
    dbManager.pushBlock(parent);
    ProposalCapsule proposal = new ProposalCapsule(ByteString.copyFrom(witnessKey.getAddress()), 1);
    proposal.setParameters(Collections.singletonMap(
        ProposalType.ALLOW_STRICT_ECDSA_VALIDATION.getCode(), 1L));
    proposal.setExpirationTime(ACTIVATION_TIME);
    if (approved) {
      proposal.addApproval(ByteString.copyFrom(witnessKey.getAddress()));
    }
    dbManager.getProposalStore().put(proposal.createDbKey(), proposal);
    properties.saveLatestProposalNum(1);
  }

  private ProposalCapsule proposal() throws Exception {
    return dbManager.getProposalStore().get(ProposalCapsule.calculateDbKey(1));
  }

  private BlockCapsule nextBlock(long time, TransactionCapsule... transactions) {
    BlockCapsule block = new BlockCapsule(properties.getLatestBlockHeaderNumber() + 1,
        properties.getLatestBlockHeaderHash(), time, ByteString.copyFrom(witnessKey.getAddress()));
    for (TransactionCapsule tx : transactions) {
      block.addTransaction(tx);
    }
    block.setMerkleRoot();
    block.sign(witnessKey.getPrivKeyBytes());
    return block;
  }

  private TransactionCapsule unsignedTransaction(long amount) throws Exception {
    TransferContract transfer = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ownerKey.getAddress()))
        .setToAddress(ByteString.copyFrom(witnessKey.getAddress())).setAmount(amount).build();
    TransactionCapsule tx = new TransactionCapsule(transfer, ContractType.TransferContract);
    tx.setReference(parent.getNum(), parent.getBlockId().getBytes());
    tx.setTimestamp(ACTIVATION_TIME - 1000);
    tx.setExpiration(ACTIVATION_TIME + 60_000);
    return tx;
  }

  private TransactionCapsule legacyTransaction(long amount) throws Exception {
    TransactionCapsule tx = unsignedTransaction(amount);
    byte[] hash = tx.getTransactionId().getBytes();
    // R = G and s = hash mod n recover infinity in legacy mode; the encoding is exactly 65 bytes.
    ECDSASignature signature = new ECDSASignature(
        ECKey.CURVE.getG().getAffineXCoord().toBigInteger(),
        new BigInteger(1, hash).mod(ECKey.CURVE.getN()));
    signature.v = 27;
    assertEquals(ByteString.copyFrom(INFINITY_ADDRESS),
        ByteString.copyFrom(SignUtils.signatureToAddress(hash, signature.toBase64(), false)));
    return signedTransaction(tx, signature.toByteArray());
  }

  private TransactionCapsule signedTransaction(TransactionCapsule tx, byte[] signature) {
    TransactionCapsule signed = new TransactionCapsule(tx.getInstance().toBuilder()
        .addSignature(ByteString.copyFrom(signature))
        .addRet(Transaction.Result.newBuilder()
            .setContractRet(Transaction.Result.contractResult.SUCCESS))
        .build());
    signed.setTime(System.currentTimeMillis());
    return signed;
  }

  private List<TransactionCapsule> populateQueues() throws Exception {
    return enqueue(Arrays.asList(legacyTransaction(1), legacyTransaction(2),
        legacyTransaction(3), legacyTransaction(4)));
  }

  private List<TransactionCapsule> populateValidQueues() throws Exception {
    AccountCapsule owner = dbManager.getAccountStore().get(ownerKey.getAddress());
    owner.setInstance(owner.getInstance().toBuilder().setOwnerPermission(
        AccountCapsule.getDefaultPermission(ByteString.copyFrom(ownerKey.getAddress()))).build());
    dbManager.getAccountStore().put(ownerKey.getAddress(), owner);
    TransactionCapsule[] transactions = new TransactionCapsule[4];
    for (int i = 0; i < transactions.length; i++) {
      TransactionCapsule tx = unsignedTransaction(i + 1);
      transactions[i] = signedTransaction(tx,
          ownerKey.sign(tx.getTransactionId().getBytes()).toByteArray());
    }
    return enqueue(Arrays.asList(transactions));
  }

  private List<TransactionCapsule> enqueue(List<TransactionCapsule> transactions) throws Exception {
    for (TransactionCapsule tx : transactions) {
      assertTrue(tx.validateSignature(dbManager.getAccountStore(), properties));
      assertTrue(tx.isVerified());
    }
    dbManager.getPendingTransactions().add(transactions.get(0));
    dbManager.getRePushTransactions().add(transactions.get(1));
    dbManager.getPoppedTransactions().add(transactions.get(2));
    pushingQueue().add(transactions.get(3));
    return transactions;
  }

  @SuppressWarnings("unchecked")
  private BlockingQueue<TransactionCapsule> pushingQueue() {
    return (BlockingQueue<TransactionCapsule>) ReflectUtils.getFieldObject(
        dbManager, "pushTransactionQueue");
  }

  private void assertRolledBack(List<TransactionCapsule> queued) throws Exception {
    assertFalse(properties.allowStrictEcdsaValidation());
    assertEquals(State.PENDING, proposal().getState());
    assertEquals(parent.getBlockId(), chainBaseManager.getHeadBlockId());
    for (TransactionCapsule tx : queued) {
      assertFalse("Rollback may conservatively leave caches invalidated", tx.isVerified());
      assertTrue(tx.validateSignature(dbManager.getAccountStore(), properties));
    }
  }
}
