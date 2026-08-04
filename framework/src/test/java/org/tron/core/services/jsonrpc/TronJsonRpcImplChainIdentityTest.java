package org.tron.core.services.jsonrpc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;

public class TronJsonRpcImplChainIdentityTest {

  private static final String FAILED = "Chain identity lookup failed";
  private static final String MARKER = "ordinary-marker";

  private Wallet wallet;
  private TronJsonRpcImpl rpc;

  @Before
  public void setUp() {
    wallet = mock(Wallet.class);
    rpc = new TronJsonRpcImpl(null, wallet, null);
  }

  @After
  public void tearDown() throws Exception {
    rpc.close();
  }

  @Test
  public void everyFailureIsLoggedAtDebugWithCause() {
    RuntimeException failure = new RuntimeException(MARKER);
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(failure);

    try (ApiLogCapture logs = new ApiLogCapture()) {
      for (int i = 0; i < 3; i++) {
        JsonRpcInternalException thrown =
            Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId);
        Assert.assertEquals("Chain identity unavailable", thrown.getMessage());
        Assert.assertSame(failure, thrown.getCause());
        Assert.assertNull(thrown.getData());
      }

      Assert.assertTrue(events(logs, Level.WARN, FAILED).isEmpty());
      List<ILoggingEvent> debugs = events(logs, Level.DEBUG, FAILED);
      Assert.assertEquals(3, debugs.size());
      Assert.assertEquals(debugs.size(), logs.events().size());
      for (ILoggingEvent event : debugs) {
        Assert.assertSame(failure, loggedThrowable(event));
      }
    }
  }

  @Test
  public void failuresAroundRecoveryAreEachLogged() throws Exception {
    // The realistic failure: the wallet returns null for the genesis block, and dereferencing
    // it throws a NullPointerException inside the lookup.
    BlockCapsule genesis = mock(BlockCapsule.class);
    when(genesis.getBlockId()).thenReturn(new BlockCapsule.BlockId(new byte[32], 0));
    when(wallet.getBlockCapsuleByNum(0))
        .thenReturn(null)
        .thenReturn(genesis)
        .thenReturn(null);

    try (ApiLogCapture logs = new ApiLogCapture()) {
      assertNullLookupFailure(
          Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId));
      Assert.assertEquals("0x00000000", rpc.ethChainId());
      assertNullLookupFailure(
          Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId));

      Assert.assertTrue(events(logs, Level.WARN, FAILED).isEmpty());
      List<ILoggingEvent> debugs = events(logs, Level.DEBUG, FAILED);
      Assert.assertEquals(2, debugs.size());
      Assert.assertEquals(debugs.size(), logs.events().size());
      for (ILoggingEvent event : debugs) {
        Assert.assertNotNull(event.getThrowableProxy());
        Assert.assertEquals(NullPointerException.class.getName(),
            event.getThrowableProxy().getClassName());
      }
    }
  }

  @Test
  public void wrappedFatalCauseIsRethrownWithoutLogging() {
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(new RuntimeException("wrapper", fatal));

    try (ApiLogCapture logs = new ApiLogCapture()) {
      Assert.assertSame(fatal, Assert.assertThrows(OutOfMemoryError.class, rpc::ethChainId));
      Assert.assertTrue("a fatal cause must not be logged", logs.events().isEmpty());
    }
  }

  @Test
  public void directFatalIsRethrownWithoutLogging() {
    // A direct Error bypasses catch (Exception). This test guards unchanged,
    // log-free propagation, not the ordering of wrapped-fatal classification.
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(fatal);

    try (ApiLogCapture logs = new ApiLogCapture()) {
      Assert.assertSame(fatal, Assert.assertThrows(OutOfMemoryError.class, rpc::ethChainId));
      Assert.assertTrue("a fatal cause must not be logged", logs.events().isEmpty());
    }
  }

  @Test
  public void netVersionGoesThroughTheSameLookup() {
    RuntimeException failure = new RuntimeException(MARKER);
    when(wallet.getBlockCapsuleByNum(0)).thenThrow(failure);

    try (ApiLogCapture logs = new ApiLogCapture()) {
      Assert.assertThrows(JsonRpcInternalException.class, rpc::ethChainId);
      Assert.assertThrows(JsonRpcInternalException.class, rpc::getNetVersion);

      Assert.assertTrue(events(logs, Level.WARN, FAILED).isEmpty());
      List<ILoggingEvent> debugs = events(logs, Level.DEBUG, FAILED);
      Assert.assertEquals(2, debugs.size());
      Assert.assertEquals(debugs.size(), logs.events().size());
      for (ILoggingEvent event : debugs) {
        Assert.assertSame(failure, loggedThrowable(event));
      }
    }
  }

  private static void assertNullLookupFailure(JsonRpcInternalException thrown) {
    Assert.assertEquals("Chain identity unavailable", thrown.getMessage());
    Assert.assertTrue(thrown.getCause() instanceof NullPointerException);
  }

  private static Throwable loggedThrowable(ILoggingEvent event) {
    Assert.assertNotNull(event.getThrowableProxy());
    return ((ThrowableProxy) event.getThrowableProxy()).getThrowable();
  }

  private static List<ILoggingEvent> events(ApiLogCapture logs, Level level, String marker) {
    return logs.events().stream()
        .filter(e -> e.getLevel() == level)
        .filter(e -> e.getFormattedMessage().contains(marker))
        .collect(Collectors.toList());
  }
}
