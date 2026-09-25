package org.tron.core.capsule;

import static org.tron.protos.Protocol.Transaction.Result.contractResult.BAD_JUMP_DESTINATION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.PRECOMPILED_CONTRACT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.SignatureFormatException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.Transaction.raw;

@Slf4j
public class TransactionCapsuleTest extends BaseTest {

  private static String OWNER_ADDRESS;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        StringUtil.hexString2ByteString(OWNER_ADDRESS), AccountType.Normal, 10_000_000_000L);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }

  @Test
  public void trxCapsuleClearTest() {
    Transaction tx = Transaction.newBuilder()
        .addRet(Result.newBuilder().setContractRet(contractResult.OUT_OF_TIME).build()).build();
    TransactionCapsule trxCap = new TransactionCapsule(tx);
    Result.contractResult contractResult = trxCap.getContractResult();
    trxCap.resetResult();
    Assert.assertEquals(trxCap.getInstance().getRetCount(), 0);
    trxCap.setResultCode(contractResult);
    Assert.assertEquals(trxCap.getInstance()
        .getRet(0).getContractRet(), Result.contractResult.OUT_OF_TIME);
  }

  @Test
  public void shouldGateStrictSignatureLength() throws Exception {
    byte[] hash = new byte[32];
    ECKey key = ECKey.fromPrivate(BigInteger.TEN);
    Permission permission = permissionFor(key);
    ByteString signature = ByteString.copyFrom(key.Base64toBytes(key.signHash(hash)));
    ByteString padded = signature.concat(ByteString.copyFrom(new byte[3]));

    Assert.assertEquals(1L, TransactionCapsule.checkWeight(
        permission, Arrays.asList(signature), hash, null, true));
    Assert.assertEquals(1L, TransactionCapsule.checkWeight(
        permission, Arrays.asList(padded), hash, null, false));
    Assert.assertThrows(SignatureFormatException.class,
        () -> TransactionCapsule.checkWeight(
            permission, Arrays.asList(padded), hash, null, true));
  }

  @Test
  public void shouldRejectInvalidComponentsInStrictMode() {
    byte[] hash = new byte[32];
    ECKey key = ECKey.fromPrivate(BigInteger.TEN);
    Permission permission = permissionFor(key);
    byte[] signature = key.Base64toBytes(key.signHash(hash));
    BigInteger curveOrder = ECKey.CURVE.getN();

    for (BigInteger invalidScalar : Arrays.asList(
        BigInteger.ZERO, curveOrder, curveOrder.add(BigInteger.ONE))) {
      assertStrictComponentRejected(permission, hash,
          replaceScalar(signature, 0, invalidScalar));
      assertStrictComponentRejected(permission, hash,
          replaceScalar(signature, 32, invalidScalar));
    }

    for (byte invalidV : new byte[]{8, 26, 35}) {
      byte[] invalidSignature = Arrays.copyOf(signature, signature.length);
      invalidSignature[64] = invalidV;
      assertStrictComponentRejected(permission, hash, invalidSignature);
    }
  }

  private byte[] replaceScalar(byte[] signature, int offset, BigInteger scalar) {
    byte[] result = Arrays.copyOf(signature, signature.length);
    System.arraycopy(ByteUtil.bigIntegerToBytes(scalar, 32), 0, result, offset, 32);
    return result;
  }

  private void assertStrictComponentRejected(Permission permission, byte[] hash,
      byte[] signature) {
    Assert.assertThrows(SignatureException.class,
        () -> TransactionCapsule.checkWeight(permission,
            Arrays.asList(ByteString.copyFrom(signature)), hash, null, true));
  }

  private Permission permissionFor(ECKey key) {
    return Permission.newBuilder()
        .setThreshold(1)
        .addKeys(Key.newBuilder()
            .setAddress(ByteString.copyFrom(key.getAddress()))
            .setWeight(1))
        .build();
  }

  @Test
  public void testRemoveRedundantRet() {
    Transaction.Builder transaction = Transaction.newBuilder().setRawData(raw.newBuilder()
        .addContract(Transaction.Contract.newBuilder().setType(ContractType.TriggerSmartContract))
        .setFeeLimit(1000000000)).build().toBuilder();
    transaction.addRet(Result.newBuilder().setContractRet(SUCCESS).build());
    transaction.addRet(Result.newBuilder().setContractRet(PRECOMPILED_CONTRACT).build());
    transaction.addRet(Result.newBuilder().setContractRet(BAD_JUMP_DESTINATION).build());
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction.build());
    transactionCapsule.removeRedundantRet();
    Assert.assertEquals(1, transactionCapsule.getInstance().getRetCount());
    Assert.assertEquals(SUCCESS, transactionCapsule.getInstance().getRet(0).getContractRet());
  }

  @Test
  public void slowVerify() {
    Logger capsuleLogger = (Logger) LoggerFactory.getLogger("capsule");
    Level originalLevel = capsuleLogger.getLevel();
    capsuleLogger.setLevel(Level.INFO);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    capsuleLogger.addAppender(appender);
    try {
      TransactionCapsule cap = new TransactionCapsule(Transaction.newBuilder().build());
      long startNs = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(51);
      cap.logSlowSigVerify(startNs);

      List<ILoggingEvent> warns = appender.list.stream()
          .filter(e -> e.getLevel() == Level.WARN)
          .collect(Collectors.toList());
      Assert.assertEquals("expected one WARN for a slow verify", 1, warns.size());
      String rendered = warns.get(0).getFormattedMessage();
      Assert.assertTrue("WARN should mention slow verify: " + rendered,
          rendered.contains("slow verify"));
      Assert.assertTrue("WARN should echo the txId: " + rendered,
          rendered.contains(cap.getTransactionId().toString()));
      Assert.assertTrue("WARN should include sigCount: " + rendered,
          rendered.contains("sigCount="));
      Assert.assertTrue("WARN should include cost in ms: " + rendered,
          rendered.contains("cost="));
      Assert.assertTrue("WARN should render ms suffix: " + rendered,
          rendered.contains(" ms"));
    } finally {
      appender.stop();
      capsuleLogger.detachAppender(appender);
      capsuleLogger.setLevel(originalLevel);
    }
  }

  @Test
  public void fastVerify() {
    Logger capsuleLogger = (Logger) LoggerFactory.getLogger("capsule");
    Level originalLevel = capsuleLogger.getLevel();
    capsuleLogger.setLevel(Level.INFO);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    capsuleLogger.addAppender(appender);
    try {
      TransactionCapsule cap = new TransactionCapsule(Transaction.newBuilder().build());
      cap.logSlowSigVerify(System.nanoTime());
      long warnCount = appender.list.stream()
          .filter(e -> e.getLevel() == Level.WARN)
          .count();
      Assert.assertEquals("no WARN should fire below the threshold", 0, warnCount);
    } finally {
      appender.stop();
      capsuleLogger.detachAppender(appender);
      capsuleLogger.setLevel(originalLevel);
    }
  }

  @Test
  public void shouldInvalidateVerificationCacheAfterInFlightValidation() throws Exception {
    CountDownLatch validationStarted = new CountDownLatch(1);
    CountDownLatch continueValidation = new CountDownLatch(1);
    CountDownLatch invalidationCompleted = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Transaction transaction = Transaction.newBuilder()
        .setRawData(raw.newBuilder()
            .addContract(Transaction.Contract.newBuilder()
                .setType(ContractType.TransferContract)))
        .build();
    TransactionCapsule capsule = new TransactionCapsule(transaction) {
      @Override
      public boolean validatePubSignature(AccountStore accountStore,
          DynamicPropertiesStore dynamicPropertiesStore) throws ValidateSignatureException {
        validationStarted.countDown();
        try {
          if (!continueValidation.await(5, TimeUnit.SECONDS)) {
            throw new ValidateSignatureException("timed out waiting to continue validation");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new ValidateSignatureException("validation interrupted");
        }
        return true;
      }
    };

    Thread validationThread = new Thread(() -> {
      try {
        capsule.validateSignature(null, null);
      } catch (Throwable t) {
        failure.set(t);
      }
    });
    Thread invalidationThread = new Thread(() -> {
      capsule.setVerified(false);
      invalidationCompleted.countDown();
    });

    try {
      validationThread.start();
      Assert.assertTrue(validationStarted.await(5, TimeUnit.SECONDS));
      invalidationThread.start();
      Assert.assertFalse(invalidationCompleted.await(100, TimeUnit.MILLISECONDS));

      continueValidation.countDown();
      validationThread.join(TimeUnit.SECONDS.toMillis(5));
      invalidationThread.join(TimeUnit.SECONDS.toMillis(5));

      Assert.assertFalse(validationThread.isAlive());
      Assert.assertFalse(invalidationThread.isAlive());
      Assert.assertNull(failure.get());
      Assert.assertFalse(capsule.isVerified());
    } finally {
      continueValidation.countDown();
      validationThread.interrupt();
      invalidationThread.interrupt();
    }
  }
}
