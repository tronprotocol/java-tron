package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.args.Args;
import org.tron.p2p.connection.Channel;


public class PeerStatusCheckTest {

  protected TronApplicationContext context;
  private PeerStatusCheck service;
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void init() throws IOException {
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(PeerStatusCheck.class);
  }

  /**
   * destroy.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void testCheck() {
    int maxConnection = 30;
    Assert.assertEquals(maxConnection, Args.getInstance().getMaxConnections());
    Assert.assertEquals(0, PeerManager.getPeers().size());

    for (int i = 0; i < maxConnection; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(context, c1);
    }

    PeerManager.getPeers().get(0).getSyncBlockRequested()
        .put(new BlockId(), System.currentTimeMillis() - NetConstants.SYNC_TIME_OUT - 1000);
    ReflectUtils.invokeMethod(service, "statusCheck");

    Assert.assertEquals(maxConnection - 1L, PeerManager.getPeers().size());
  }
}
