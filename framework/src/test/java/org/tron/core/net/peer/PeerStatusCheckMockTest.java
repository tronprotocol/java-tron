package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.ReasonCode;

public class PeerStatusCheckMockTest {

  private PeerStatusCheck peerStatusCheck;
  private PeerConnection peer;
  private Channel channel;

  @Before
  public void setUp() {
    peerStatusCheck = spy(new PeerStatusCheck());
    peer = spy(new PeerConnection());
    channel = mock(Channel.class);
    ReflectUtils.setFieldValue(peer, "channel", channel);
    doNothing().when(peer).disconnect(any());
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(peer));
    ReflectUtils.setFieldValue(peerStatusCheck, "tronNetDelegate", delegate);
  }

  @After
  public void tearDown() {
    try {
      peerStatusCheck.close();
    } finally {
      Mockito.framework().clearInlineMocks();
    }
  }

  @Test
  public void testInitException() {
    peerStatusCheck.close();
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
  public void testHelloTimeoutWithoutSync() {
    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(false);
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis());
    assertTimeout(false);

    when(channel.getStartTime()).thenReturn(System.currentTimeMillis() - 10_000);
    assertTimeout(true);
  }

  @Test
  public void testHelloTimeoutDependsOnHandshakeCompletion() {
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis() - 60_000);
    peer.setLastInteractiveTime(System.currentTimeMillis());
    peer.setBlockBothHave(new BlockId());
    // Recent traffic and block progress must not extend the HELLO deadline.
    assertTimeout(true);

    peer.setHelloMessageReceive(mock(HelloMessage.class));
    peer.setNeedSyncFromPeer(false);
    assertTimeout(false);
  }

  @Test
  public void testHelloCheckPreservesExistingTimeouts() {
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis());
    ReflectUtils.setFieldValue(peer, "blockBothHaveUpdateTime", 0L);
    // A fresh connection must still be subject to the existing sync timeout.
    assertTimeout(true);

    peer.setHelloMessageReceive(mock(HelloMessage.class));
    peer.setNeedSyncFromPeer(false);
    peer.getSyncBlockRequested().put(new BlockId(),
        System.currentTimeMillis() - NetConstants.SYNC_TIME_OUT - 1_000);
    assertTimeout(true);
  }

  private void assertTimeout(boolean expected) {
    clearInvocations(peer);
    peerStatusCheck.statusCheck();
    if (expected) {
      verify(peer).disconnect(ReasonCode.TIME_OUT);
    } else {
      verify(peer, never()).disconnect(any());
    }
  }

}
