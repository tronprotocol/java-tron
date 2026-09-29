package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.TestConstants;
import org.tron.common.parameter.RateLimiterInitialization.HttpRateLimiterItem;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
import org.tron.core.services.WalletOnCursor;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;

/**
 * Verifies RateLimiterServlet's adapter resolution: strict whitelist
 * (no Class.forName arbitrary class loading), fail-fast on unknown or
 * empty names, and successful construction of every whitelisted adapter.
 *
 * <p>Also covers the rate-limiting logic in {@link RateLimiterServlet#service}:
 * <ol>
 *   <li>Per-endpoint check runs <em>before</em> the global check, so a per-endpoint
 *       rejection never consumes a global IP/QPS token.</li>
 *   <li>A {@link IPreemptibleRateLimiter} permit is always released — whether the
 *       global limiter rejects the request or the request handler completes normally.</li>
 *   <li>The per-endpoint limiter is the one of the request's surface, so each port keeps its
 *       own configuration and quota.</li>
 *   <li>On PBFT the read cursor is selected only after both limiters admitted the request, and
 *       is always reset.</li>
 * </ol>
 */
public class RateLimiterServletTest {

  private static final Map<String, Class<? extends IRateLimiter>> allowedAdapters =
      RateLimiterServlet.ALLOWED_ADAPTERS;

  private static final String KEY_HTTP = "http_";

  private TestServlet servlet;
  private RateLimiterContainer container;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  /** Minimal concrete subclass — only {@code doGet} is needed for the happy-path test. */
  static class TestServlet extends RateLimiterServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      // intentional no-op
    }
  }

  /** Records the endpoint body among the admission and cursor events of a request. */
  static class RecordingServlet extends RateLimiterServlet {
    final List<String> events = new ArrayList<>();
    boolean fail;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      events.add("body");
      if (fail) {
        throw new IOException("boom");
      }
    }
  }

  static class OversizedRequestServlet extends RateLimiterServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
      throw new BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413,
          "Request body is too large");
    }
  }

  /**
   * GlobalRateLimiter's static initializer calls Args.getInstance().getRateLimiterGlobalQps().
   * Without Args being initialized the default QPS is 0, causing RateLimiter.create(0) to throw.
   * Initializing Args here (before the class is first loaded inside each test method) prevents
   * the static initialization failure that would otherwise break mockStatic().
   */
  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[0], TestConstants.TEST_CONF);
    servlet = new TestServlet();
    container = new RateLimiterContainer();
    Field f = RateLimiterServlet.class.getDeclaredField("container");
    f.setAccessible(true);
    f.set(servlet, container);

    request = new MockHttpServletRequest("GET", "/test");
    request.setRemoteAddr("10.0.0.1");
    response = new MockHttpServletResponse();
  }

  @AfterClass
  public static void tearDown() {
    Args.clearParam();
  }

  @Test
  public void testWhitelistContents() {
    assertEquals(GlobalPreemptibleAdapter.class,
        allowedAdapters.get(GlobalPreemptibleAdapter.class.getSimpleName()));
    assertEquals(QpsRateLimiterAdapter.class,
        allowedAdapters.get(QpsRateLimiterAdapter.class.getSimpleName()));
    assertEquals(IPQPSRateLimiterAdapter.class,
        allowedAdapters.get(IPQPSRateLimiterAdapter.class.getSimpleName()));
    assertEquals(DefaultBaseQqsAdapter.class,
        allowedAdapters.get(DefaultBaseQqsAdapter.class.getSimpleName()));
  }

  @Test
  public void testWhitelistRejectsUnknownAdapter() {
    assertNull(allowedAdapters.get("EvilAdapter"));
    assertNull(allowedAdapters.get("java.lang.Runtime"));
  }

  @Test
  public void testUnknownAdapterThrowsTronError() {
    // Fail-fast parity with the pre-whitelist Class.forName behavior: an unknown
    // adapter name raises TronError from @PostConstruct so Spring startup aborts
    // rather than silently masking a misconfigured node.
    TronError e = assertThrows(TronError.class,
        () -> RateLimiterServlet.buildAdapter("UnknownAdapter", "qps=100", "TestServlet"));
    assertEquals(TronError.ErrCode.RATE_LIMITER_INIT, e.getErrCode());
    assertTrue(e.getMessage().contains("UnknownAdapter"));
    assertTrue(e.getMessage().contains("TestServlet"));
  }

  @Test
  public void testDefaultAdapterNameBuildsDefaultBaseQqsAdapter() {
    // When no config entry exists for a servlet, addRateContainer passes
    // DEFAULT_ADAPTER_NAME to buildAdapter; verify it resolves to
    // DefaultBaseQqsAdapter.
    IRateLimiter limiter = RateLimiterServlet.buildAdapter(
        RateLimiterServlet.DEFAULT_ADAPTER_NAME, "qps=100", "TestServlet");
    assertNotNull(limiter);
    assertTrue(limiter instanceof DefaultBaseQqsAdapter);
  }

  @Test
  public void testEmptyAdapterNameThrowsTronError() {
    // Fail-fast parity with original: a configured-but-empty strategy name is
    // a configuration bug and must not be silently replaced by the default.
    TronError e = assertThrows(TronError.class,
        () -> RateLimiterServlet.buildAdapter("", "qps=100", "TestServlet"));
    assertEquals(TronError.ErrCode.RATE_LIMITER_INIT, e.getErrCode());
  }

  @Test
  public void testBuildsEachWhitelistedAdapter() {
    // Exercises the newInstance(String) constructor path for every whitelisted
    // adapter so a signature/strategy-class break on any entry fails here
    // instead of at node startup.
    assertTrue(RateLimiterServlet.buildAdapter(
        QpsRateLimiterAdapter.class.getSimpleName(), "qps=100", "TestServlet")
        instanceof QpsRateLimiterAdapter);
    assertTrue(RateLimiterServlet.buildAdapter(
        IPQPSRateLimiterAdapter.class.getSimpleName(), "qps=100", "TestServlet")
        instanceof IPQPSRateLimiterAdapter);
    assertTrue(RateLimiterServlet.buildAdapter(
        GlobalPreemptibleAdapter.class.getSimpleName(), "permit=1", "TestServlet")
        instanceof GlobalPreemptibleAdapter);
  }

  /**
   * Per-endpoint rejects → GlobalRateLimiter must NOT be invoked.
   * The global IP/QPS quota is fully preserved for other clients.
   */
  @Test
  public void testPerEndpointRejectedDoesNotConsumeGlobalQuota() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(false);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      servlet.service(request, response);

      globalMock.verify(() -> GlobalRateLimiter.acquirePermit(any()), never());
      // acquirePermit returned false — no permit was taken, nothing to release
      verify(perEndpoint, never()).release();
    }
  }

  /**
   * Per-endpoint (QPS-only, non-preemptible) rejects → global not called,
   * and no release() attempt on a non-IPreemptibleRateLimiter.
   */
  @Test
  public void testNonPreemptiblePerEndpointRejectedDoesNotConsumeGlobal() throws Exception {
    IRateLimiter perEndpoint = Mockito.mock(IRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(false);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      servlet.service(request, response);

      globalMock.verify(() -> GlobalRateLimiter.acquirePermit(any()), never());
    }
  }

  /**
   * Per-endpoint (IPreemptibleRateLimiter) acquires the permit, but the global limiter
   * then rejects. The finally block must release the permit to avoid a semaphore leak.
   */
  @Test
  public void testGlobalRejectedReleasesPreemptiblePermit() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(false);

      servlet.service(request, response);

      // Permit was acquired but request blocked — must be returned
      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * Both limiters pass → request executes and the permit is released exactly once
   * in the finally block after the handler returns.
   */
  @Test
  public void testBothPassPermitReleasedAfterRequest() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      servlet.service(request, response);

      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * No per-endpoint limiter configured (null) → only GlobalRateLimiter is consulted,
   * and nothing is released (no permit to hold).
   */
  @Test
  public void testNullRateLimiterConsultsOnlyGlobal() throws Exception {
    // No entry added to container — container.get() returns null
    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      servlet.service(request, response);

      globalMock.verify(() -> GlobalRateLimiter.acquirePermit(any()), times(1));
    }
  }

  @Test
  public void testOversizedRequestBadMessagePropagates() throws Exception {
    OversizedRequestServlet oversizedServlet = new OversizedRequestServlet();
    Field f = RateLimiterServlet.class.getDeclaredField("container");
    f.setAccessible(true);
    f.set(oversizedServlet, container);
    MockHttpServletRequest postRequest = new MockHttpServletRequest("POST", "/jsonrpc");
    postRequest.setRemoteAddr("10.0.0.1");
    postRequest.setServletPath("/jsonrpc");

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      BadMessageException e = assertThrows(BadMessageException.class,
          () -> oversizedServlet.service(postRequest, response));

      assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413, e.getCode());
    }
  }

  @Test
  public void testLimiterNamePerSurface() {
    assertEquals("GetAccountServlet",
        RateLimiterServlet.limiterName(GetAccountServlet.class, Surface.FULL));
    assertEquals("GetAccountOnSolidityServlet",
        RateLimiterServlet.limiterName(GetAccountServlet.class, Surface.SOLIDITY));
    assertEquals("GetAccountOnPBFTServlet",
        RateLimiterServlet.limiterName(GetAccountServlet.class, Surface.PBFT));
    assertEquals("GetAccountServlet",
        RateLimiterServlet.limiterName(GetAccountServlet.class, Surface.SOLIDITY_NODE));
    assertEquals("GetTransactionByIdSolidityServlet",
        RateLimiterServlet.limiterName(GetTransactionByIdServlet.class, Surface.SOLIDITY_NODE));
    assertEquals("GetTransactionInfoByIdSolidityServlet",
        RateLimiterServlet.limiterName(GetTransactionInfoByIdServlet.class,
            Surface.SOLIDITY_NODE));
  }

  /**
   * A servlet builds one limiter per surface its @HttpApi declares, each from the
   * rate.limiter.http entry under that surface's name.
   */
  @Test
  public void testEachSurfaceGetsItsOwnConfiguredLimiter() throws Exception {
    Map<String, HttpRateLimiterItem> config =
        Args.getInstance().getRateLimiterInitialization().getHttpMap();
    config.put("GetAccountOnSolidityServlet", new HttpRateLimiterItem(
        "GetAccountOnSolidityServlet", QpsRateLimiterAdapter.class.getSimpleName(), "qps=5"));
    config.put("GetAccountOnPBFTServlet", new HttpRateLimiterItem(
        "GetAccountOnPBFTServlet", GlobalPreemptibleAdapter.class.getSimpleName(), "permit=1"));
    try {
      GetAccountServlet getAccount = new GetAccountServlet();
      inject(getAccount, "container", container);
      Method init = RateLimiterServlet.class.getDeclaredMethod("addRateContainer");
      init.setAccessible(true);
      init.invoke(getAccount);

      assertTrue(container.get(KEY_HTTP, "GetAccountServlet") instanceof DefaultBaseQqsAdapter);
      assertTrue(container.get(KEY_HTTP, "GetAccountOnSolidityServlet")
          instanceof QpsRateLimiterAdapter);
      assertTrue(container.get(KEY_HTTP, "GetAccountOnPBFTServlet")
          instanceof GlobalPreemptibleAdapter);
      // SOLIDITY_NODE shares the FULL name, so three limiters in all
      assertEquals(3, container.getMap().size());
    } finally {
      config.remove("GetAccountOnSolidityServlet");
      config.remove("GetAccountOnPBFTServlet");
    }
  }

  /** Traffic on one port is admitted by that port's limiter only, never another port's. */
  @Test
  public void testEachSurfaceUsesItsOwnLimiter() throws Exception {
    inject(servlet, "walletOnPBFT", Mockito.mock(WalletOnPBFT.class));
    Map<Surface, IRateLimiter> limiters = new EnumMap<>(Surface.class);
    for (Surface surface : new Surface[] {Surface.FULL, Surface.SOLIDITY, Surface.PBFT}) {
      IRateLimiter limiter = Mockito.mock(IRateLimiter.class);
      when(limiter.acquirePermit(any(RuntimeData.class))).thenReturn(true);
      container.add(KEY_HTTP, RateLimiterServlet.limiterName(TestServlet.class, surface), limiter);
      limiters.put(surface, limiter);
    }

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);
      servlet.service(requestOn(Surface.PBFT), response);
      servlet.service(requestOn(Surface.PBFT), response);
      servlet.service(requestOn(Surface.SOLIDITY), response);
    }

    verify(limiters.get(Surface.PBFT), times(2)).acquirePermit(any(RuntimeData.class));
    verify(limiters.get(Surface.SOLIDITY), times(1)).acquirePermit(any(RuntimeData.class));
    verify(limiters.get(Surface.FULL), never()).acquirePermit(any(RuntimeData.class));
  }

  /**
   * A PBFT cursor is an offset from the live head, so it must be selected only after both
   * limiters admitted the request; selected earlier, a blocking admission would let the read
   * pass the PBFT-finalized block while the head advances.
   */
  @Test
  public void testPbftCursorSelectedOnlyAfterBothLimitersAdmit() throws Exception {
    RecordingServlet recording = recordingOnPbft(true);
    try (MockedStatic<GlobalRateLimiter> globalMock = recordingGlobal(recording.events, true)) {
      recording.service(requestOn(Surface.PBFT), response);
    }
    assertEquals(Arrays.asList("endpoint", "global", "cursor", "body", "reset"),
        recording.events);
  }

  @Test
  public void testPbftCursorNotSelectedWhenAdmissionFails() throws Exception {
    RecordingServlet endpointRejects = recordingOnPbft(false);
    try (MockedStatic<GlobalRateLimiter> globalMock =
        recordingGlobal(endpointRejects.events, true)) {
      endpointRejects.service(requestOn(Surface.PBFT), response);
    }
    assertEquals(Collections.singletonList("endpoint"), endpointRejects.events);

    RecordingServlet globalRejects = recordingOnPbft(true);
    try (MockedStatic<GlobalRateLimiter> globalMock =
        recordingGlobal(globalRejects.events, false)) {
      globalRejects.service(requestOn(Surface.PBFT), response);
    }
    assertEquals(Arrays.asList("endpoint", "global"), globalRejects.events);
  }

  @Test
  public void testPbftCursorResetWhenEndpointThrows() throws Exception {
    RecordingServlet recording = recordingOnPbft(true);
    recording.fail = true;
    try (MockedStatic<GlobalRateLimiter> globalMock = recordingGlobal(recording.events, true)) {
      assertThrows(IOException.class,
          () -> recording.service(requestOn(Surface.PBFT), response));
    }
    // a cursor left selected would leak into the next request on this pooled thread
    assertEquals(Arrays.asList("endpoint", "global", "cursor", "body", "reset"),
        recording.events);
  }

  /** SOLIDITY selects its cursor in a filter; FULL and SOLIDITY_NODE read the node's head. */
  @Test
  public void testCursorSelectedOnlyOnPbft() throws Exception {
    WalletOnPBFT view = Mockito.mock(WalletOnPBFT.class);
    inject(servlet, "walletOnPBFT", view);
    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);
      for (Surface surface : new Surface[] {Surface.FULL, Surface.SOLIDITY,
          Surface.SOLIDITY_NODE}) {
        servlet.service(requestOn(surface), response);
      }
      // a context that declares no surface
      servlet.service(request, response);
    }
    verify(view, never()).selectCursor();
  }

  /** A servlet on PBFT whose per-endpoint limiter admits or rejects, recording each event. */
  private RecordingServlet recordingOnPbft(boolean endpointAdmits) throws Exception {
    RecordingServlet recording = new RecordingServlet();
    List<String> events = recording.events;
    inject(recording, "container", container);

    WalletOnPBFT view = Mockito.mock(WalletOnPBFT.class);
    when(view.selectCursor()).thenAnswer(inv -> {
      events.add("cursor");
      return (WalletOnCursor.CursorScope) () -> events.add("reset");
    });
    inject(recording, "walletOnPBFT", view);

    IRateLimiter perEndpoint = Mockito.mock(IRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenAnswer(inv -> {
      events.add("endpoint");
      return endpointAdmits;
    });
    container.add(KEY_HTTP,
        RateLimiterServlet.limiterName(RecordingServlet.class, Surface.PBFT), perEndpoint);
    return recording;
  }

  private static MockedStatic<GlobalRateLimiter> recordingGlobal(List<String> events,
      boolean admits) {
    MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class);
    globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenAnswer(inv -> {
      events.add("global");
      return admits;
    });
    return globalMock;
  }

  private static MockHttpServletRequest requestOn(Surface surface) {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/test");
    req.setRemoteAddr("10.0.0.1");
    req.getServletContext().setAttribute(RateLimiterServlet.SURFACE_ATTRIBUTE, surface);
    return req;
  }

  private static void inject(RateLimiterServlet target, String field, Object value)
      throws Exception {
    Field f = RateLimiterServlet.class.getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
  }
}
