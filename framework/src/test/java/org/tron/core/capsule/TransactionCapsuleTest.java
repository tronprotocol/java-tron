package org.tron.core.capsule;

import static org.tron.protos.Protocol.Transaction.Result.contractResult.BAD_JUMP_DESTINATION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.PRECOMPILED_CONTRACT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;
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
}
