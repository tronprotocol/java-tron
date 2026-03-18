package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.BaseMethodTest;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.args.Args;
import org.tron.p2p.connection.Channel;


public class PeerStatusCheckTest extends BaseMethodTest {

  private PeerStatusCheck service;

  @Override
  protected String[] extraArgs() {
    return new String[]{"--debug"};
  }

  @Override
  protected void afterInit() {
    service = context.getBean(PeerStatusCheck.class);
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
