package org.tron.core.services.ratelimiter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;

/**
 * Unit tests for the rate-limiting logic in
 * {@link RateLimiterInterceptor#interceptCall}.
 *
 * <p>The key invariants under test:
 * <ol>
 *   <li>Per-endpoint check runs <em>before</em> the global check — a per-endpoint
 *       rejection must not consume any global IP/QPS token.</li>
 *   <li>A {@link IPreemptibleRateLimiter} permit is always released:
 *       <ul>
 *         <li>immediately, when the global limiter rejects after per-endpoint passes;</li>
 *         <li>in the catch block, when {@code next.startCall()} throws after both pass;</li>
 *         <li>via {@code onComplete()} / {@code onCancel()} on the returned listener
 *             for successful calls.</li>
 *       </ul>
 *   </li>
 * </ol>
 */
@SuppressWarnings("unchecked")
public class RateLimiterInterceptorTest {

  private static final String METHOD_NAME = "tron.api.Wallet/GetNowBlock";
  private static final String KEY_RPC = "rpc_";

  private RateLimiterInterceptor interceptor;
  private RateLimiterContainer container;

  private ServerCall<Object, Object> call;
  private Metadata headers;
  private ServerCallHandler<Object, Object> next;

  @AfterClass
  public static void tearDown() {
    Args.clearParam();
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
    interceptor = new RateLimiterInterceptor();
    container = new RateLimiterContainer();
    Field f = RateLimiterInterceptor.class.getDeclaredField("container");
    f.setAccessible(true);
    f.set(interceptor, container);

    call = Mockito.mock(ServerCall.class);
    MethodDescriptor<Object, Object> descriptor = Mockito.mock(MethodDescriptor.class);
    when(call.getMethodDescriptor()).thenReturn(descriptor);
    when(descriptor.getFullMethodName()).thenReturn(METHOD_NAME);
    // Attributes.EMPTY causes RuntimeData to catch the NPE and set address=""
    when(call.getAttributes()).thenReturn(Attributes.EMPTY);

    headers = new Metadata();
    next = Mockito.mock(ServerCallHandler.class);
  }

  /**
   * Per-endpoint rejects → GlobalRateLimiter must NOT be called.
   * No permit was acquired, so release() must not be called either.
   */
  @Test
  public void testPerEndpointRejectedDoesNotConsumeGlobalQuota() {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(false);
    container.add(KEY_RPC, METHOD_NAME, perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      interceptor.interceptCall(call, headers, next);

      globalMock.verify(() -> GlobalRateLimiter.acquirePermit(any()), never());
      verify(perEndpoint, never()).release();
    }
  }

  /**
   * Non-preemptible per-endpoint rejects → global not called.
   */
  @Test
  public void testNonPreemptiblePerEndpointRejectedDoesNotConsumeGlobal() {
    IRateLimiter perEndpoint = Mockito.mock(IRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(false);
    container.add(KEY_RPC, METHOD_NAME, perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      interceptor.interceptCall(call, headers, next);

      globalMock.verify(() -> GlobalRateLimiter.acquirePermit(any()), never());
    }
  }

  /**
   * Per-endpoint (IPreemptibleRateLimiter) acquires, but global rejects.
   * The early-return rejection path must release the permit immediately.
   */
  @Test
  public void testGlobalRejectedReleasesPreemptiblePermit() {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_RPC, METHOD_NAME, perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(false);

      interceptor.interceptCall(call, headers, next);

      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * Both limiters pass but {@code next.startCall()} throws.
   * The catch block must:
   *   - release the permit to prevent a permanent semaphore leak (the
   *     SimpleForwardingServerCallListener that holds the release logic is never
   *     assigned when the exception is thrown);
   *   - close the call with INTERNAL so the client fails immediately instead of
   *     waiting for the transport-level deadline.
   */
  @Test
  public void testStartCallExceptionReleasesPermitAndClosesCall() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_RPC, METHOD_NAME, perEndpoint);
    when(next.startCall(any(), any())).thenThrow(new RuntimeException("handler crash"));

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      interceptor.interceptCall(call, headers, next);

      verify(perEndpoint, times(1)).release();
      ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
      verify(call, times(1)).close(statusCaptor.capture(), any(Metadata.class));
      assertEquals(Status.Code.INTERNAL, statusCaptor.getValue().getCode());
    }
  }

  /**
   * Normal successful flow: both pass, {@code next.startCall()} succeeds.
   * The returned listener's {@code onComplete()} must release the permit exactly once.
   */
  @Test
  public void testListenerReleasesPermitOnComplete() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_RPC, METHOD_NAME, perEndpoint);

    ServerCall.Listener<Object> delegate = Mockito.mock(ServerCall.Listener.class);
    when(next.startCall(any(), any())).thenReturn(delegate);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      ServerCall.Listener<Object> listener = interceptor.interceptCall(call, headers, next);
      listener.onComplete();

      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * Normal successful flow: both pass, {@code next.startCall()} succeeds.
   * The returned listener's {@code onCancel()} must release the permit exactly once.
   */
  @Test
  public void testListenerReleasesPermitOnCancel() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_RPC, METHOD_NAME, perEndpoint);

    ServerCall.Listener<Object> delegate = Mockito.mock(ServerCall.Listener.class);
    when(next.startCall(any(), any())).thenReturn(delegate);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      ServerCall.Listener<Object> listener = interceptor.interceptCall(call, headers, next);
      listener.onCancel();

      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * No per-endpoint limiter configured (null) → GlobalRateLimiter is still called once.
   */
  @Test
  public void testNullRateLimiterConsultsOnlyGlobal() throws Exception {
    // Nothing registered in container — container.get() returns null
    ServerCall.Listener<Object> delegate = Mockito.mock(ServerCall.Listener.class);
    when(next.startCall(any(), any())).thenReturn(delegate);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenReturn(true);

      interceptor.interceptCall(call, headers, next);

      globalMock.verify(() -> GlobalRateLimiter.acquirePermit(any()), times(1));
    }
  }
}
