package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.ReasonCode;

public class PeerHelloTimeoutTest {

  private PeerStatusCheck service;
  private PeerConnection peer;
  private Channel channel;

  @Before
  public void setUp() {
    service = new PeerStatusCheck();
    peer = spy(new PeerConnection());
    channel = mock(Channel.class);
    ReflectUtils.setFieldValue(peer, "channel", channel);
    doNothing().when(peer).disconnect(any());
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(peer));
    ReflectUtils.setFieldValue(service, "tronNetDelegate", delegate);
  }

  @After
  public void tearDown() {
    service.close();
  }

  @Test
  public void testIncompleteHelloTimesOutAtTenSecondsWithoutSync() {
    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(false);
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis() - 10_000);

    service.statusCheck();

    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }

  @Test
  public void testRecentInteractionDoesNotExtendHelloTimeout() {
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis() - 20_000);
    peer.setLastInteractiveTime(System.currentTimeMillis());
    peer.setBlockBothHave(new BlockId());

    service.statusCheck();

    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }

  @Test
  public void testIncompleteHelloWithinDeadlineDoesNotTimeOut() {
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis());

    service.statusCheck();

    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testIncompleteHelloStillChecksExistingSyncTimeout() {
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis());
    ReflectUtils.setFieldValue(peer, "blockBothHaveUpdateTime", 0L);

    service.statusCheck();

    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }

  @Test
  public void testCompletedHelloDoesNotTimeOut() {
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis() - 60_000);
    peer.setHelloMessageReceive(mock(HelloMessage.class));
    peer.setNeedSyncFromPeer(false);

    service.statusCheck();

    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testCompletedHelloStillChecksRequestTimeout() {
    peer.setHelloMessageReceive(mock(HelloMessage.class));
    peer.setNeedSyncFromPeer(false);
    peer.getSyncBlockRequested().put(new BlockId(),
        System.currentTimeMillis() - NetConstants.SYNC_TIME_OUT - 1_000);

    service.statusCheck();

    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }
}
