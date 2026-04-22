package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;

public class PeerStatusCheckMockTest {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testInitException() {
    PeerStatusCheck peerStatusCheck = spy(new PeerStatusCheck());
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    ReflectUtils.setFieldValue(peerStatusCheck, "peerStatusCheckExecutor", executor);
    doThrow(new RuntimeException("test exception")).when(peerStatusCheck).statusCheck();

    peerStatusCheck.init();

    Mockito.verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(5L), eq(2L),
        eq(TimeUnit.SECONDS));
    Runnable scheduledTask = Mockito.mockingDetails(executor).getInvocations().stream()
        .filter(invocation -> invocation.getMethod().getName().equals("scheduleWithFixedDelay"))
        .map(invocation -> (Runnable) invocation.getArgument(0))
        .findFirst()
        .orElseThrow(() -> new AssertionError("scheduled task was not registered"));

    scheduledTask.run();

    Mockito.verify(peerStatusCheck).statusCheck();
  }

}
