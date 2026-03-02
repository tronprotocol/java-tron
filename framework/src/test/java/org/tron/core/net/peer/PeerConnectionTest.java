package org.tron.core.net.peer;

import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Pair;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.message.keepalive.PongMessage;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

public class PeerConnectionTest {

  @Test
  public void testVariableDefaultValue() {
    PeerConnection peerConnection = new PeerConnection();
    Assert.assertTrue(!peerConnection.isBadPeer());
    Assert.assertTrue(!peerConnection.isFetchAble());
    Assert.assertTrue(peerConnection.isIdle());
    Assert.assertTrue(!peerConnection.isRelayPeer());
    Assert.assertTrue(peerConnection.isNeedSyncFromPeer());
    Assert.assertTrue(peerConnection.isNeedSyncFromUs());
    Assert.assertTrue(!peerConnection.isSyncFinish());
  }

  @Test
  public void testOnDisconnect() {
    PeerConnection peerConnection = new PeerConnection();

    SyncService syncService = mock(SyncService.class);
    ReflectUtils.setFieldValue(peerConnection, "syncService", syncService);

    AdvService advService = mock(AdvService.class);
    ReflectUtils.setFieldValue(peerConnection, "advService", advService);

    Item item = new Item(Sha256Hash.ZERO_HASH, Protocol.Inventory.InventoryType.TRX);
    Long time = System.currentTimeMillis();
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    peerConnection.getAdvInvReceive().put(item, time);
    peerConnection.getAdvInvSpread().put(item, time);
    peerConnection.getSyncBlockIdCache().put(item.getHash(), time);
    peerConnection.getSyncBlockToFetch().add(blockId);
    peerConnection.getSyncBlockRequested().put(blockId, time);
    peerConnection.getSyncBlockInProcess().add(blockId);

    peerConnection.onDisconnect();

    Assert.assertEquals(0, peerConnection.getAdvInvReceive().size());
    Assert.assertEquals(0, peerConnection.getAdvInvSpread().size());
    Assert.assertEquals(0, peerConnection.getSyncBlockIdCache().size());
    Assert.assertEquals(0, peerConnection.getSyncBlockToFetch().size());
    Assert.assertEquals(0, peerConnection.getSyncBlockRequested().size());
    Assert.assertEquals(0, peerConnection.getSyncBlockInProcess().size());
  }

  @Test
  public void testIsIdle() {
    PeerConnection peerConnection = new PeerConnection();
    boolean f = peerConnection.isIdle();
    Assert.assertTrue(f);

    Item item = new Item(Sha256Hash.ZERO_HASH, Protocol.Inventory.InventoryType.TRX);
    Long time = System.currentTimeMillis();
    peerConnection.getAdvInvRequest().put(item, time);
    f = peerConnection.isIdle();
    Assert.assertTrue(!f);

    peerConnection.getAdvInvRequest().clear();
    f = peerConnection.isIdle();
    Assert.assertTrue(f);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    peerConnection.getSyncBlockRequested().put(blockId, time);
    f = peerConnection.isIdle();
    Assert.assertTrue(!f);

    peerConnection.getSyncBlockRequested().clear();
    f = peerConnection.isIdle();
    Assert.assertTrue(f);

    peerConnection.setSyncChainRequested(new Pair<>(new LinkedList<>(), time));
    f = peerConnection.isIdle();
    Assert.assertTrue(!f);
  }

  @Test
  public void testIsSyncIdle() {
    PeerConnection peerConnection = new PeerConnection();
    boolean f = peerConnection.isSyncIdle();
    Assert.assertTrue(f);

    Item item = new Item(Sha256Hash.ZERO_HASH, Protocol.Inventory.InventoryType.TRX);
    Long time = System.currentTimeMillis();
    peerConnection.getAdvInvRequest().put(item, time);
    f = peerConnection.isSyncIdle();
    Assert.assertTrue(f);

    peerConnection.getAdvInvRequest().clear();
    f = peerConnection.isSyncIdle();
    Assert.assertTrue(f);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    peerConnection.getSyncBlockRequested().put(blockId, time);
    f = peerConnection.isSyncIdle();
    Assert.assertTrue(!f);

    peerConnection.getSyncBlockRequested().clear();
    f = peerConnection.isSyncIdle();
    Assert.assertTrue(f);

    peerConnection.setSyncChainRequested(new Pair<>(new LinkedList<>(), time));
    f = peerConnection.isSyncIdle();
    Assert.assertTrue(!f);
  }

  @Test
  public void testOnConnect() {
    PeerConnection peerConnection = new PeerConnection();
    SyncService syncService = mock(SyncService.class);
    ReflectUtils.setFieldValue(peerConnection, "syncService", syncService);

    HelloMessage m1 = mock(HelloMessage.class);
    BlockCapsule.BlockId b1 = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1);
    Mockito.when(m1.getHeadBlockId()).thenReturn(b1);

    HelloMessage m2 = mock(HelloMessage.class);
    BlockCapsule.BlockId b2 = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 2);
    Mockito.when(m2.getHeadBlockId()).thenReturn(b2);

    Assert.assertTrue(peerConnection.isNeedSyncFromUs());
    Assert.assertTrue(peerConnection.isNeedSyncFromPeer());

    ReflectUtils.setFieldValue(peerConnection, "helloMessageReceive", m1);
    ReflectUtils.setFieldValue(peerConnection, "helloMessageSend", m2);
    peerConnection.onConnect();
    Assert.assertTrue(peerConnection.isNeedSyncFromUs());
    Assert.assertTrue(!peerConnection.isNeedSyncFromPeer());

    peerConnection.setNeedSyncFromPeer(true);
    peerConnection.setNeedSyncFromUs(true);
    ReflectUtils.setFieldValue(peerConnection, "helloMessageReceive", m2);
    ReflectUtils.setFieldValue(peerConnection, "helloMessageSend", m1);
    peerConnection.onConnect();
    Assert.assertTrue(!peerConnection.isNeedSyncFromUs());
    Assert.assertTrue(peerConnection.isNeedSyncFromPeer());

    peerConnection.setNeedSyncFromPeer(true);
    peerConnection.setNeedSyncFromUs(true);
    ReflectUtils.setFieldValue(peerConnection, "helloMessageReceive", m1);
    ReflectUtils.setFieldValue(peerConnection, "helloMessageSend", m1);
    peerConnection.onConnect();
    Assert.assertTrue(!peerConnection.isNeedSyncFromUs());
    Assert.assertTrue(!peerConnection.isNeedSyncFromPeer());
  }

  @Test
  public void testSetChannel() {
    PeerConnection peerConnection = new PeerConnection();

    InetSocketAddress inetSocketAddress =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());

    List<InetSocketAddress> relayNodes = new ArrayList<>();
    ReflectUtils.setFieldValue(peerConnection, "relayNodes", relayNodes);

    peerConnection.setChannel(c1);
    Assert.assertTrue(!peerConnection.isRelayPeer());

    relayNodes.add(inetSocketAddress);
    peerConnection.setChannel(c1);
    Assert.assertTrue(peerConnection.isRelayPeer());
  }

  @Test
  public void testIsSyncFinish() {
    PeerConnection peerConnection = new PeerConnection();
    boolean f = peerConnection.isSyncFinish();
    Assert.assertTrue(!f);

    peerConnection.setNeedSyncFromUs(false);
    f = peerConnection.isSyncFinish();
    Assert.assertTrue(!f);

    peerConnection.setNeedSyncFromPeer(false);
    f = peerConnection.isSyncFinish();
    Assert.assertTrue(f);
  }

  @Test
  public void testCheckAndPutAdvInvRequest() {
    PeerConnection peerConnection = new PeerConnection();
    Item item = new Item(Sha256Hash.ZERO_HASH, Protocol.Inventory.InventoryType.TRX);
    Long time = System.currentTimeMillis();
    boolean f = peerConnection.checkAndPutAdvInvRequest(item, time);
    Assert.assertTrue(f);

    f = peerConnection.checkAndPutAdvInvRequest(item, time);
    Assert.assertTrue(!f);
  }

  @Test
  public void testEquals() {
    List<InetSocketAddress> relayNodes = new ArrayList<>();

    PeerConnection p1 = new PeerConnection();
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());
    ReflectUtils.setFieldValue(p1, "relayNodes", relayNodes);
    p1.setChannel(c1);

    PeerConnection p2 = new PeerConnection();
    InetSocketAddress inetSocketAddress2 =
        new InetSocketAddress("127.0.0.2", 10002);
    Channel c2 = new Channel();
    ReflectUtils.setFieldValue(c2, "inetSocketAddress", inetSocketAddress2);
    ReflectUtils.setFieldValue(c2, "inetAddress", inetSocketAddress2.getAddress());
    ReflectUtils.setFieldValue(p2, "relayNodes", relayNodes);
    p2.setChannel(c2);

    PeerConnection p3 = new PeerConnection();
    InetSocketAddress inetSocketAddress3 =
        new InetSocketAddress("127.0.0.2", 10002);
    Channel c3 = new Channel();
    ReflectUtils.setFieldValue(c3, "inetSocketAddress", inetSocketAddress3);
    ReflectUtils.setFieldValue(c3, "inetAddress", inetSocketAddress3.getAddress());
    ReflectUtils.setFieldValue(p3, "relayNodes", relayNodes);
    p3.setChannel(c3);

    Assert.assertTrue(p1.equals(p1));
    Assert.assertTrue(!p1.equals(p2));
    Assert.assertTrue(p2.equals(p3));
  }

  @Test
  public void testHashCode() {
    List<InetSocketAddress> relayNodes = new ArrayList<>();

    PeerConnection p1 = new PeerConnection();
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());
    ReflectUtils.setFieldValue(p1, "relayNodes", relayNodes);
    p1.setChannel(c1);

    PeerConnection p2 = new PeerConnection();
    InetSocketAddress inetSocketAddress2 =
        new InetSocketAddress("127.0.0.2", 10002);
    Channel c2 = new Channel();
    ReflectUtils.setFieldValue(c2, "inetSocketAddress", inetSocketAddress2);
    ReflectUtils.setFieldValue(c2, "inetAddress", inetSocketAddress2.getAddress());
    ReflectUtils.setFieldValue(p2, "relayNodes", relayNodes);
    p2.setChannel(c2);

    PeerConnection p3 = new PeerConnection();
    InetSocketAddress inetSocketAddress3 =
        new InetSocketAddress("127.0.0.2", 10002);
    Channel c3 = new Channel();
    ReflectUtils.setFieldValue(c3, "inetSocketAddress", inetSocketAddress3);
    ReflectUtils.setFieldValue(c3, "inetAddress", inetSocketAddress3.getAddress());
    ReflectUtils.setFieldValue(p3, "relayNodes", relayNodes);
    p3.setChannel(c3);

    Assert.assertTrue(p1.hashCode() != p2.hashCode());
    Assert.assertTrue(p2.hashCode() == p3.hashCode());
  }

  @Test
  public void testNeedToLog() throws Exception {
    Message msg = new PingMessage();
    boolean f = PeerConnection.needToLog(msg);
    Assert.assertTrue(!f);

    msg = new PongMessage();
    f = PeerConnection.needToLog(msg);
    Assert.assertTrue(!f);

    msg = new InventoryMessage(new ArrayList<>(), Protocol.Inventory.InventoryType.TRX);
    f = PeerConnection.needToLog(msg);
    Assert.assertTrue(!f);

    msg = new InventoryMessage(new ArrayList<>(), Protocol.Inventory.InventoryType.BLOCK);
    f = PeerConnection.needToLog(msg);
    Assert.assertTrue(f);
  }

}
