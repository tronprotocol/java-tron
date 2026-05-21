package org.tron.core.net;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.statistics.PeerStatistics;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class P2pEventHandlerImplTest extends BaseTest {

  @BeforeClass
  public static void init() throws Exception {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);
  }

  @Test
  public void testProcessInventoryMessage() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setMaxTps(10);
    parameter.setMaxBlockInvPerSecond(10);

    PeerStatistics peerStatistics = new PeerStatistics();

    PeerConnection peer = mock(PeerConnection.class);
    Mockito.when(peer.getPeerStatistics()).thenReturn(peerStatistics);

    P2pEventHandlerImpl p2pEventHandler = new P2pEventHandlerImpl();

    Method method = p2pEventHandler.getClass()
            .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
    method.setAccessible(true);

    int count = peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement
            .getCount(10);

    Assert.assertEquals(0, count);

    List<Sha256Hash> list = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    InventoryMessage msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.TRX);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10);

    Assert.assertEquals(10, count);

    list.clear();
    for (int i = 0; i < 100; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.TRX);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10);

    Assert.assertEquals(10, count);  // 100 hashes dropped: 10+100=110 > maxCountIn10s(100)

    list.clear();
    for (int i = 0; i < 100; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.TRX);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10);

    Assert.assertEquals(10, count);  // still dropped: window=10, 10+100=110 > 100

    list.clear();
    for (int i = 0; i < 200; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.BLOCK);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10);

    Assert.assertEquals(0, count);   // 200 hashes dropped: 0+200=200 > maxBlockInvIn10s(100)

    list.clear();
    for (int i = 0; i < 100; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.BLOCK);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10);

    Assert.assertEquals(100, count); // passes: window=0, 0+100=100, not > 100

  }

  @Test
  public void testCheckInvRateLimitTrxBoundary() throws Exception {
    // maxTps=10 → maxCountIn10s=100
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setMaxTps(10);
    parameter.setMaxBlockInvPerSecond(10);

    PeerStatistics peerStatistics = new PeerStatistics();
    PeerConnection peer = mock(PeerConnection.class);
    Mockito.when(peer.getPeerStatistics()).thenReturn(peerStatistics);

    P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
    Method method = handler.getClass()
        .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
    method.setAccessible(true);

    // Fill window to 91: send 91 TRX hashes → passes (0+91=91 ≤ 100)
    List<Sha256Hash> list91 = new ArrayList<>();
    for (int i = 0; i < 91; i++) {
      list91.add(new Sha256Hash(i, new byte[32]));
    }
    InventoryMessage msg91 = new InventoryMessage(list91, InventoryType.TRX);
    method.invoke(handler, peer, msg91.getSendBytes());
    Assert.assertEquals(91,
        peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10));

    // Send 9 more TRX hashes → passes (91+9=100, not > 100)
    List<Sha256Hash> list9 = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      list9.add(new Sha256Hash(i, new byte[32]));
    }
    InventoryMessage msg9 = new InventoryMessage(list9, InventoryType.TRX);
    method.invoke(handler, peer, msg9.getSendBytes());
    Assert.assertEquals(100,
        peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10));

    // Send 1 more TRX hash → DROPPED (100+1=101 > 100)
    List<Sha256Hash> list1 = new ArrayList<>();
    list1.add(new Sha256Hash(0, new byte[32]));
    InventoryMessage msg1 = new InventoryMessage(list1, InventoryType.TRX);
    method.invoke(handler, peer, msg1.getSendBytes());
    Assert.assertEquals(100,  // count unchanged: message was dropped
        peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10));
  }

  @Test
  public void testCheckInvRateLimitBlockBoundary() throws Exception {
    // maxBlockInvPerSecond=10 → maxBlockInvIn10s=100
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setMaxTps(1000);
    parameter.setMaxBlockInvPerSecond(10);

    PeerStatistics peerStatistics = new PeerStatistics();
    PeerConnection peer = mock(PeerConnection.class);
    Mockito.when(peer.getPeerStatistics()).thenReturn(peerStatistics);

    P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
    Method method = handler.getClass()
        .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
    method.setAccessible(true);

    // Send 101 BLOCK hashes → DROPPED (0+101=101 > 100)
    List<Sha256Hash> list101 = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      list101.add(new Sha256Hash(i, new byte[32]));
    }
    InventoryMessage msgBlock101 = new InventoryMessage(list101, InventoryType.BLOCK);
    method.invoke(handler, peer, msgBlock101.getSendBytes());
    Assert.assertEquals(0,
        peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10));

    // Send 100 BLOCK hashes → passes (0+100=100, not > 100)
    List<Sha256Hash> list100 = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      list100.add(new Sha256Hash(i, new byte[32]));
    }
    InventoryMessage msgBlock100 = new InventoryMessage(list100, InventoryType.BLOCK);
    method.invoke(handler, peer, msgBlock100.getSendBytes());
    Assert.assertEquals(100,
        peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10));

    // Send 1 more BLOCK hash → DROPPED (100+1=101 > 100)
    List<Sha256Hash> list1 = new ArrayList<>();
    list1.add(new Sha256Hash(0, new byte[32]));
    InventoryMessage msgBlock1 = new InventoryMessage(list1, InventoryType.BLOCK);
    method.invoke(handler, peer, msgBlock1.getSendBytes());
    Assert.assertEquals(100,  // count unchanged: message was dropped
        peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10));
  }

  @Test
  public void testUpdateLastInteractiveTime() throws Exception {
    PeerConnection peer = new PeerConnection();
    P2pEventHandlerImpl p2pEventHandler = new P2pEventHandlerImpl();

    Method method = p2pEventHandler.getClass()
        .getDeclaredMethod("updateLastInteractiveTime", PeerConnection.class, TronMessage.class);
    method.setAccessible(true);

    long t1 = System.currentTimeMillis();
    FetchInvDataMessage message = new FetchInvDataMessage(new ArrayList<>(), InventoryType.BLOCK);
    method.invoke(p2pEventHandler, peer, message);
    Assert.assertTrue(peer.getLastInteractiveTime() >= t1);
  }
}
