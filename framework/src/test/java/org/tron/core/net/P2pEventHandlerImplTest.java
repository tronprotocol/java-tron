package org.tron.core.net;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.statistics.PeerStatistics;
import org.tron.protos.Protocol;

public class P2pEventHandlerImplTest {

  @Test
  public void testProcessInventoryMessage() throws Exception {
    String[] a = new String[0];
    Args.setParam(a, Constant.TESTNET_CONF);
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setMaxTps(10);

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

    Assert.assertEquals(110, count);

    list.clear();
    for (int i = 0; i < 100; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.TRX);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10);

    Assert.assertEquals(110, count);

    list.clear();
    for (int i = 0; i < 200; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.BLOCK);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10);

    Assert.assertEquals(200, count);

    list.clear();
    for (int i = 0; i < 100; i++) {
      list.add(new Sha256Hash(i, new byte[32]));
    }

    msg = new InventoryMessage(list, Protocol.Inventory.InventoryType.BLOCK);

    method.invoke(p2pEventHandler, peer, msg.getSendBytes());

    count = peer.getPeerStatistics().messageStatistics.tronInBlockInventoryElement.getCount(10);

    Assert.assertEquals(300, count);

  }
}
