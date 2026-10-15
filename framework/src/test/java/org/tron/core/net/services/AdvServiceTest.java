package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.TestConstants;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.adv.AdvService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class AdvServiceTest {
  private static TronApplicationContext context;
  private static AdvService service;
  private static P2pEventHandlerImpl p2pEventHandler;
  private static ApplicationContext ctx;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, TestConstants.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(AdvService.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");
  }

  @AfterClass
  public static void after() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void test() {
    testAddInv();
    testBroadcast();
    testTrxBroadcast();
  }

  private void testAddInv() {
    boolean flag;
    Item itemTrx = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
    flag = service.addInv(itemTrx);
    Assert.assertTrue(flag);
    flag = service.addInv(itemTrx);
    Assert.assertFalse(flag);

    Item itemBlock = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1000L);
    itemBlock = new Item(blockId, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertTrue(flag);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 10000L);
    itemBlock = new Item(blockId, InventoryType.BLOCK);
    service.addInvToCache(itemBlock);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);
  }

  private void testBroadcast() {
    InetSocketAddress inetSocketAddress =
            new InetSocketAddress("127.0.0.2", 10001);

    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(inetSocketAddress);
    Mockito.when(c1.getInetAddress()).thenReturn(inetSocketAddress.getAddress());

    PeerConnection peer = PeerManager.add(ctx, c1);
    peer.setChannel(c1);
    peer.setNeedSyncFromUs(false);
    peer.setNeedSyncFromPeer(false);

    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    BlockMessage msg = new BlockMessage(blockCapsule);
    service.broadcast(msg);
    Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
    Assert.assertNotNull(service.getMessage(item));
    Assert.assertNotNull(peer.getAdvInvSpread().getIfPresent(item));
  }

  private void testTrxBroadcast() {
    Protocol.Transaction trx = Protocol.Transaction.newBuilder()
        .setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setRefBlockNum(1)
            .setExpiration(System.currentTimeMillis() + 3000).build()).build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg = new TransactionMessage(trx);
    service.broadcast(msg);
    Item item = new Item(msg.getMessageId(), InventoryType.TRX);
    Assert.assertNotNull(service.getMessage(item));

    Protocol.Transaction expiredTrx = Protocol.Transaction.newBuilder()
        .setRawData(
            Protocol.Transaction.raw.newBuilder()
            .setRefBlockNum(1)
            .setExpiration(System.currentTimeMillis() - 1).build())
        .build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg1 = new TransactionMessage(expiredTrx);
    service.broadcast(msg);
    Item item1 = new Item(msg1.getMessageId(), InventoryType.TRX);
    Assert.assertNull(service.getMessage(item1));
  }

  @Test
  public void sameAddressPeersReceiveBroadcastAndDisconnectRetryIndependently() {
    InetSocketAddress address = new InetSocketAddress("127.0.0.2", 10001);
    Channel firstChannel = mock(Channel.class);
    Channel secondChannel = mock(Channel.class);
    Mockito.when(firstChannel.getInetSocketAddress()).thenReturn(address);
    Mockito.when(firstChannel.getInetAddress()).thenReturn(address.getAddress());
    Mockito.when(secondChannel.getInetSocketAddress()).thenReturn(address);
    Mockito.when(secondChannel.getInetAddress()).thenReturn(address.getAddress());

    PeerConnection first = Mockito.spy(new PeerConnection());
    PeerConnection second = Mockito.spy(new PeerConnection());
    first.setChannel(firstChannel);
    second.setChannel(secondChannel);
    first.setNeedSyncFromPeer(false);
    first.setNeedSyncFromUs(false);
    second.setNeedSyncFromPeer(false);
    second.setNeedSyncFromUs(false);
    Mockito.doNothing().when(first).sendMessage(Mockito.any());
    Mockito.doNothing().when(second).sendMessage(Mockito.any());

    TronNetDelegate delegate = mock(TronNetDelegate.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(Arrays.asList(first, second));
    AdvService isolated = new AdvService();
    ReflectUtils.setFieldValue(isolated, "tronNetDelegate", delegate);
    try {
      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      isolated.broadcast(new BlockMessage(block));
      Mockito.verify(first).sendMessage(Mockito.isA(InventoryMessage.class));
      Mockito.verify(second).sendMessage(Mockito.isA(InventoryMessage.class));

      Item item = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
      first.getAdvInvRequest().put(item, System.currentTimeMillis());
      second.getAdvInvReceive().put(item, System.currentTimeMillis());
      isolated.onDisconnect(first);
      Assert.assertTrue(second.getAdvInvRequest().containsKey(item));
      Mockito.verify(second).sendMessage(Mockito.isA(FetchInvDataMessage.class));
    } finally {
      isolated.close();
    }
  }

}
