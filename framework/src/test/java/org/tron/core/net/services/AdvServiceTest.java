package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import org.tron.common.overlay.message.Message;
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
import org.tron.core.net.service.fetchblock.FetchBlockService;
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
    Channel firstChannel = new Channel();
    Channel secondChannel = new Channel();
    ReflectUtils.setFieldValue(firstChannel, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(firstChannel, "inetAddress", address.getAddress());
    ReflectUtils.setFieldValue(secondChannel, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(secondChannel, "inetAddress", address.getAddress());

    List<Message> firstMessages = new ArrayList<>();
    List<Message> secondMessages = new ArrayList<>();
    PeerConnection first = new PeerConnection() {
      @Override
      public void sendMessage(Message message) {
        firstMessages.add(message);
      }
    };
    PeerConnection second = new PeerConnection() {
      @Override
      public void sendMessage(Message message) {
        secondMessages.add(message);
      }
    };
    first.setChannel(firstChannel);
    second.setChannel(secondChannel);
    Assert.assertEquals(firstChannel, secondChannel);
    Assert.assertNotEquals(first, second);
    first.setNeedSyncFromPeer(false);
    first.setNeedSyncFromUs(false);
    second.setNeedSyncFromPeer(false);
    second.setNeedSyncFromUs(false);

    TronNetDelegate delegate = mock(TronNetDelegate.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(Arrays.asList(first, second));
    AdvService isolated = new AdvService();
    ReflectUtils.setFieldValue(isolated, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(isolated, "fetchBlockService", mock(FetchBlockService.class));
    try {
      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      isolated.broadcast(new BlockMessage(block));
      Assert.assertEquals(1, firstMessages.size());
      Assert.assertEquals(1, secondMessages.size());
      Assert.assertTrue(firstMessages.get(0) instanceof InventoryMessage);
      Assert.assertTrue(secondMessages.get(0) instanceof InventoryMessage);
      Assert.assertEquals(Collections.singletonList(block.getBlockId()),
          ((InventoryMessage) firstMessages.get(0)).getHashList());
      Assert.assertEquals(Collections.singletonList(block.getBlockId()),
          ((InventoryMessage) secondMessages.get(0)).getHashList());

      Item item = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
      first.getAdvInvRequest().put(item, System.currentTimeMillis());
      second.getAdvInvReceive().put(item, System.currentTimeMillis());
      isolated.onDisconnect(first);
      Assert.assertTrue(second.getAdvInvRequest().containsKey(item));
      Assert.assertEquals(1, firstMessages.size());
      Assert.assertEquals(2, secondMessages.size());
      Assert.assertTrue(secondMessages.get(1) instanceof FetchInvDataMessage);
      Assert.assertEquals(Collections.singletonList(item.getHash()),
          ((FetchInvDataMessage) secondMessages.get(1)).getHashList());
    } finally {
      isolated.close();
    }
  }

}
