package org.tron.core.net.messagehandler;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;

public class MessageHandlerTest {

  private static TronApplicationContext context;
  private PeerConnection peer;
  private static P2pEventHandlerImpl p2pEventHandler;
  private static ApplicationContext ctx;
  private static String dbPath = "output-message-handler-test";


  @BeforeClass
  public static void init() throws Exception {
    Args.setParam(new String[] {"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");

    TronNetService tronNetService = context.getBean(TronNetService.class);
    Parameter.p2pConfig = new P2pConfig();
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", Parameter.p2pConfig);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Before
  public void clearPeers() {
    try {
      Field field = PeerManager.class.getDeclaredField("peers");
      field.setAccessible(true);
      field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      //ignore
    }
  }

  @Test
  public void testPbft() {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    p2pEventHandler.onConnect(c1);
    Assert.assertEquals(1, PeerManager.getPeers().size());
    Assert.assertFalse(c1.isDisconnect());

    peer = PeerManager.getPeers().get(0);
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY);
    PbftMessage pbftMessage = PbftMessage.fullNodePrePrepareBlockMsg(blockCapsule, 0L);
    p2pEventHandler.onMessage(peer.getChannel(), pbftMessage.getSendBytes());

    InetSocketAddress a2 = new InetSocketAddress("127.0.0.1", 10002);
    Channel c2 = mock(Channel.class);
    Mockito.when(c2.getInetSocketAddress()).thenReturn(a2);
    Mockito.when(c2.getInetAddress()).thenReturn(a2.getAddress());
    p2pEventHandler.onMessage(c2, pbftMessage.getSendBytes());

    Assert.assertEquals(1, PeerManager.getPeers().size());
  }

  @Test
  public void testPing() {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);

    PingMessage pingMessage = new PingMessage();
    p2pEventHandler.onMessage(c1, pingMessage.getSendBytes());
    Assert.assertEquals(1, PeerManager.getPeers().size());
  }
}
