package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.InetSocketAddress;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.adv.AdvService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class AdvServiceTest {
  private static String dbPath = "output-adv-service-test1";
  private static TronApplicationContext context;
  private static AdvService service;
  private static P2pEventHandlerImpl p2pEventHandler;
  private static ApplicationContext ctx;

  @BeforeClass
  public static void init() {
    dbPath = "output-adv-service-test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
            Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(AdvService.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");
  }

  @AfterClass
  public static void after() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
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
    Assert.assertTrue(flag);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

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
    Protocol.Transaction trx = Protocol.Transaction.newBuilder().build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg = new TransactionMessage(trx);
    service.broadcast(msg);
    Item item = new Item(msg.getMessageId(), InventoryType.TRX);
    Assert.assertNotNull(service.getMessage(item));
  }

}
