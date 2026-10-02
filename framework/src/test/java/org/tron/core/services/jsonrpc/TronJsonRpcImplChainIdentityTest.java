package org.tron.core.services.jsonrpc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;

public class TronJsonRpcImplChainIdentityTest {

  private static final String FAILED = "Chain identity lookup failed";
  private static final String REPEATED = "Repeated chain identity lookup failure";
  private static final String MARKER = "ordinary-marker";

  private final Logger apiLogger = (Logger) LoggerFactory.getLogger("API");
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private Level originalLevel;
  private Wallet wallet;
  private TronJsonRpcImpl rpc;

  @Before
  public void setUp() {
    JsonRpcErrorResolver.clearSeenFailuresForTest();
    originalLevel = apiLogger.getLevel();
    // Observe every level so the fatal-path tests can assert that no log event is added at all.
    apiLogger.setLevel(Level.TRACE);
    appender.start();
    apiLogger.addAppender(appender);
    wallet = mock(Wallet.class);
    rpc = new TronJsonRpcImpl(null, wallet);
  }

  @After
  public void tearDown() throws Exception {
    apiLogger.setLevel(originalLevel);
    apiLogger.detachAppender(appender);
    appender.stop();
    JsonRpcErrorResolver.clearSeenFailuresForTest();
    rpc.close();
  }

  @Test
  public void repeatedFailuresWarnOnceThenDebugWithoutDetails() {
    RuntimeException failure = new RuntimeException(MARKER);
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(failure);

    for (int i = 0; i < 3; i++) {
      JsonRpcInternalException thrown =
          Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId);
      Assert.assertEquals("Chain identity unavailable", thrown.getMessage());
      Assert.assertSame(failure, thrown.getCause());
      Assert.assertNull(thrown.getData());
    }

    List<ILoggingEvent> warns = events(Level.WARN, FAILED);
    Assert.assertEquals(1, warns.size());
    Assert.assertNotNull(warns.get(0).getThrowableProxy());
    List<ILoggingEvent> debugs = events(Level.DEBUG, REPEATED);
    Assert.assertEquals(2, debugs.size());
    for (ILoggingEvent event : debugs) {
      Assert.assertNull(event.getThrowableProxy());
      Assert.assertFalse(event.getFormattedMessage().contains(MARKER));
    }
  }

  @Test
  public void deduplicationDoesNotResetAfterRecovery() throws Exception {
    // The realistic failure: the wallet returns null for the genesis block, and dereferencing
    // it throws a NullPointerException inside the lookup.
    BlockCapsule genesis = mock(BlockCapsule.class);
    when(genesis.getBlockId()).thenReturn(new BlockCapsule.BlockId(new byte[32], 0));
    when(wallet.getBlockCapsuleByNum(0))
        .thenReturn(null)
        .thenReturn(genesis)
        .thenReturn(null);

    assertNullLookupFailure(Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId));
    Assert.assertEquals("0x00000000", rpc.ethChainId());
    assertNullLookupFailure(Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId));

    Assert.assertEquals(1, events(Level.WARN, FAILED).size());
    Assert.assertEquals(1, events(Level.DEBUG, REPEATED).size());
    Assert.assertTrue(events(null, "recovered").isEmpty());
  }

  @Test
  public void wrappedFatalCauseIsRethrownWithoutLogging() {
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(new RuntimeException("wrapper", fatal));

    int before = appender.list.size();
    Assert.assertSame(fatal, Assert.assertThrows(OutOfMemoryError.class, rpc::ethChainId));
    Assert.assertEquals("a fatal cause must not be logged", before, appender.list.size());
  }

  @Test
  public void directFatalIsRethrownWithoutLogging() {
    // A direct Error bypasses catch (Exception). This test guards unchanged,
    // log-free propagation, not the ordering of wrapped-fatal classification.
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(fatal);

    int before = appender.list.size();
    Assert.assertSame(fatal, Assert.assertThrows(OutOfMemoryError.class, rpc::ethChainId));
    Assert.assertEquals("a fatal cause must not be logged", before, appender.list.size());
  }

  @Test
  public void fatalCauseDoesNotConsumeFirstWarning() {
    // Both failures share the outer type, so they share the deduplication key. If the fatal
    // path registered that key, the ordinary failure below would only be logged at DEBUG.
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    when(wallet.getBlockCapsuleByNum(0))
        .thenThrow(new RuntimeException("wrapper", fatal))
        .thenThrow(new RuntimeException(MARKER));

    int before = appender.list.size();
    Assert.assertSame(fatal, Assert.assertThrows(OutOfMemoryError.class, rpc::ethChainId));
    Assert.assertEquals("a fatal cause must not be logged", before, appender.list.size());
    Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId);

    Assert.assertEquals(1, events(Level.WARN, FAILED).size());
    Assert.assertTrue(events(Level.DEBUG, REPEATED).isEmpty());
  }

  @Test
  public void netVersionSharesTheChainIdentityKey() {
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(new RuntimeException(MARKER));

    Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId);
    Assert.assertThrows(JsonRpcInternalException.class, rpc::getNetVersion);

    Assert.assertEquals(1, events(Level.WARN, FAILED).size());
    Assert.assertEquals(1, events(Level.DEBUG, REPEATED).size());
  }

  private static void assertNullLookupFailure(JsonRpcInternalException thrown) {
    Assert.assertEquals("Chain identity unavailable", thrown.getMessage());
    Assert.assertTrue(thrown.getCause() instanceof NullPointerException);
  }

  private List<ILoggingEvent> events(Level level, String marker) {
    return appender.list.stream()
        .filter(e -> level == null || e.getLevel() == level)
        .filter(e -> e.getFormattedMessage().contains(marker))
        .collect(Collectors.toList());
  }
}
