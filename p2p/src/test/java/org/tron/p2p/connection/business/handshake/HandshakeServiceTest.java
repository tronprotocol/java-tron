package org.tron.p2p.connection.business.handshake;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Connect.DisconnectReason;

/**
 * Covers HandshakeService.processMessage, which decides whether an inbound peer is
 * accepted. Every rejection path ends in channel.close(), and getting one wrong either
 * admits peers that should be refused or drops peers that are fine, so each branch is
 * pinned separately.
 *
 * <p>ChannelManager is stubbed statically because processPeer/updateNodeId consult
 * process-wide connection state; the logic under test is HandshakeService's own.
 */
public class HandshakeServiceTest {

  private static final int NETWORK_ID = 11111;

  private MockedStatic<ChannelManager> channelManager;
  private HandshakeService service;
  private Channel channel;
  private HelloMessage msg;
  private List<P2pEventHandler> priorHandlers;

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setNetworkId(NETWORK_ID);

    // handlerList is the process-wide registry P2pService.register() appends to, and
    // framework's test task shares a JVM across up to 100 classes. Clearing it outright
    // would strip a co-resident test's handlers while leaving handlerMap populated, so a
    // later re-register() would throw TYPE_ALREADY_REGISTERED. Save and restore instead.
    priorHandlers = new ArrayList<>(Parameter.handlerList);
    Parameter.handlerList.clear();

    channelManager = mockStatic(ChannelManager.class);
    channelManager.when(() -> ChannelManager.getDisconnectReason(any(DisconnectCode.class)))
        .thenReturn(DisconnectReason.PEER_QUITING);

    service = new HandshakeService();

    channel = mock(Channel.class);
    when(channel.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 18888));
    when(channel.getStartTime()).thenReturn(System.currentTimeMillis());

    Node from = new Node(new InetSocketAddress("127.0.0.1", 18888));
    msg = mock(HelloMessage.class);
    when(msg.getFrom()).thenReturn(from);
    when(msg.getTimestamp()).thenReturn(System.currentTimeMillis());
    when(msg.getCode()).thenReturn(DisconnectCode.NORMAL.getValue());
    when(msg.getNetworkId()).thenReturn(NETWORK_ID);
    when(msg.getVersion()).thenReturn(NETWORK_ID);
  }

  @After
  public void tearDown() {
    channelManager.close();
    Parameter.handlerList.clear();
    Parameter.handlerList.addAll(priorHandlers);
  }

  private void acceptPeer() {
    channelManager.when(() -> ChannelManager.processPeer(any(Channel.class)))
        .thenReturn(DisconnectCode.NORMAL);
  }

  @Test
  public void rejectsASecondHandshakeOnTheSameChannel() {
    when(channel.isFinishHandshake()).thenReturn(true);

    service.processMessage(channel, msg);

    verify(channel).send(any(P2pDisconnectMessage.class));
    verify(channel).close();
    // the peer was already accepted; nothing further may be re-evaluated
    verify(channel, never()).setFinishHandshake(true);
  }

  @Test
  public void closesWhenChannelManagerRejectsThePeer() {
    channelManager.when(() -> ChannelManager.processPeer(any(Channel.class)))
        .thenReturn(DisconnectCode.TOO_MANY_PEERS);
    when(channel.isActive()).thenReturn(false);

    service.processMessage(channel, msg);

    // an inbound (non-active) channel is told why before being dropped
    verify(channel).send(any(HelloMessage.class));
    verify(channel).close();
    verify(channel, never()).setFinishHandshake(true);
  }

  @Test
  public void rejectedActiveChannelIsNotSentAHelloBack() {
    channelManager.when(() -> ChannelManager.processPeer(any(Channel.class)))
        .thenReturn(DisconnectCode.TOO_MANY_PEERS);
    when(channel.isActive()).thenReturn(true);

    service.processMessage(channel, msg);

    // we initiated this connection, so there is nothing to reply to
    verify(channel, never()).send(any(HelloMessage.class));
    verify(channel).close();
  }

  @Test
  public void stopsWhenUpdateNodeIdDisconnectedTheChannel() {
    acceptPeer();
    when(channel.isDisconnect()).thenReturn(true);

    service.processMessage(channel, msg);

    // updateNodeId dropped it as a duplicate; no handshake completion, no close here
    verify(channel, never()).setFinishHandshake(true);
    verify(channel, never()).close();
  }

  @Test
  public void completesHandshakeForAnInboundPeer() {
    acceptPeer();
    when(channel.isActive()).thenReturn(false);

    service.processMessage(channel, msg);

    verify(channel).send(any(HelloMessage.class));
    verify(channel).setFinishHandshake(true);
    verify(channel).updateAvgLatency(org.mockito.ArgumentMatchers.anyLong());
    verify(channel, never()).close();
  }

  @Test
  public void completesHandshakeForAnOutboundPeer() {
    acceptPeer();
    when(channel.isActive()).thenReturn(true);

    service.processMessage(channel, msg);

    // we already sent our hello when we dialled, so none is sent here
    verify(channel, never()).send(any(HelloMessage.class));
    verify(channel).setFinishHandshake(true);
    verify(channel, never()).close();
  }

  @Test
  public void rejectsInboundPeerOnDifferentNetworkId() {
    acceptPeer();
    when(channel.isActive()).thenReturn(false);
    when(msg.getNetworkId()).thenReturn(NETWORK_ID + 1);

    service.processMessage(channel, msg);

    // the peer is told the version differs before the channel is dropped
    verify(channel).send(any(HelloMessage.class));
    verify(channel).close();
    verify(channel, never()).setFinishHandshake(true);
  }

  @Test
  public void rejectsOutboundPeerReportingANonNormalCode() {
    acceptPeer();
    when(channel.isActive()).thenReturn(true);
    when(msg.getCode()).thenReturn(DisconnectCode.TOO_MANY_PEERS.getValue());

    service.processMessage(channel, msg);

    verify(channel).close();
    verify(channel, never()).setFinishHandshake(true);
  }

  @Test
  public void rejectsOutboundPeerWhenBothNetworkIdAndVersionDiffer() {
    acceptPeer();
    when(channel.isActive()).thenReturn(true);
    when(msg.getNetworkId()).thenReturn(NETWORK_ID + 1);
    when(msg.getVersion()).thenReturn(NETWORK_ID + 1);

    service.processMessage(channel, msg);

    verify(channel).close();
    verify(channel, never()).setFinishHandshake(true);
  }

  @Test
  public void acceptsOutboundPeerWhenVersionMatchesEvenIfNetworkIdDoesNot() {
    // v0.1 peers carry only a version; the check accepts a match on either field
    acceptPeer();
    when(channel.isActive()).thenReturn(true);
    when(msg.getNetworkId()).thenReturn(NETWORK_ID + 1);
    when(msg.getVersion()).thenReturn(NETWORK_ID);

    service.processMessage(channel, msg);

    verify(channel).setFinishHandshake(true);
    verify(channel, never()).close();
  }

  @Test
  public void startHandshakeSendsHello() {
    service.startHandshake(channel);

    verify(channel, times(1)).send(any(HelloMessage.class));
  }
}
