package org.tron.core.net.messagehandler;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.connection.Channel;

public class SyncBlockChainMsgHandlerTest {

  private TronApplicationContext context;
  private SyncBlockChainMsgHandler handler;
  private PeerConnection peer;
  private String dbPath = "output-sync-chain-test";

  @Before
  public void init() throws Exception {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
            Constant.TEST_CONF);
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
      Assert.assertTrue(e.getMessage().equals("SyncBlockChain blockIds is empty"));
    }

    List<BlockCapsule.BlockId> blockIds = new ArrayList<>();
    blockIds.add(new BlockCapsule.BlockId());
    SyncBlockChainMessage message = new SyncBlockChainMessage(blockIds);
    Method method = handler.getClass().getDeclaredMethod(
            "check", PeerConnection.class, SyncBlockChainMessage.class);
    method.setAccessible(true);
    boolean f = (boolean)method.invoke(handler, peer, message);
    Assert.assertTrue(!f);
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

}
