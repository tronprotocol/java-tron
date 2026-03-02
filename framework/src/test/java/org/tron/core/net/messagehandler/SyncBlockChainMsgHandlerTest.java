package org.tron.core.net.messagehandler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.sync.BlockInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.connection.Channel;

public class SyncBlockChainMsgHandlerTest {

  private TronApplicationContext context;
  private SyncBlockChainMsgHandler handler;
  private PeerConnection peer;
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void init() throws Exception {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    handler = context.getBean(SyncBlockChainMsgHandler.class);
    peer = context.getBean(PeerConnection.class);
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.1", 100);
    Field field = c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1);

    field = c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());

    peer.setChannel(c1);
  }

  @Test
  public void testProcessMessage() throws Exception {
    try {
      handler.processMessage(peer, new SyncBlockChainMessage(new ArrayList<>()));
    } catch (P2pException e) {
      Assert.assertEquals("SyncBlockChain blockIds is empty", e.getMessage());
    }

    List<BlockCapsule.BlockId> blockIds = new ArrayList<>();
    blockIds.add(new BlockCapsule.BlockId());
    SyncBlockChainMessage message = new SyncBlockChainMessage(blockIds);
    Method method = handler.getClass().getDeclaredMethod(
            "check", PeerConnection.class, SyncBlockChainMessage.class);
    method.setAccessible(true);
    boolean f = (boolean)method.invoke(handler, peer, message);
    Assert.assertNotNull(message.getAnswerMessage());
    Assert.assertNotNull(message.toString());
    Assert.assertNotNull(((BlockInventoryMessage) message).getAnswerMessage());
    Assert.assertFalse(f);

    Method method1 = handler.getClass().getDeclaredMethod(
        "getLostBlockIds", List.class, BlockId.class);
    method1.setAccessible(true);
    try {
      method1.invoke(handler, blockIds, new BlockCapsule.BlockId());
    } catch (InvocationTargetException e) {
      Assert.assertEquals("unForkId is null", e.getTargetException().getMessage());
    }

    Method method2 = handler.getClass().getDeclaredMethod(
        "getBlockIds", Long.class, BlockId.class);
    method2.setAccessible(true);
    List<BlockId> list = (List<BlockId>) method2.invoke(handler, 0L, new BlockCapsule.BlockId());
    Assert.assertEquals(1, list.size());
  }

  @After
  public void destroy() {
    Args.clearParam();
  }

}
