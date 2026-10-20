package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.ReasonCode;

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

  @Test
  public void testHelloTimeout() {
    PeerStatusCheck check = new PeerStatusCheck();
    PeerConnection peer = mock(PeerConnection.class);
    Channel channel = mock(Channel.class);
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(peer));
    when(peer.getChannel()).thenReturn(channel);
    when(peer.getAdvInvRequest()).thenReturn(Collections.emptyMap());
    when(peer.getSyncBlockRequested()).thenReturn(Collections.emptyMap());
    ReflectUtils.setFieldValue(check, "tronNetDelegate", delegate);
    try {
      long now = System.currentTimeMillis();
      when(channel.getStartTime()).thenReturn(now);
      check.statusCheck();
      verify(peer, never()).disconnect(any());

      when(channel.getStartTime()).thenReturn(now - NetConstants.HELLO_TIME_OUT - 1);
      check.statusCheck();
      verify(peer).disconnect(ReasonCode.TIME_OUT);

      clearInvocations(peer);
      when(peer.getHelloMessageReceive()).thenReturn(mock(HelloMessage.class));
      check.statusCheck();
      verify(peer, never()).disconnect(any());
    } finally {
      check.close();
    }
  }
}
