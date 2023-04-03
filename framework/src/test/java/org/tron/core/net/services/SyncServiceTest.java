package org.tron.core.net.services;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.TronState;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;


public class SyncServiceTest {
  protected TronApplicationContext context;
  private SyncService service;
  private PeerConnection peer;
  private P2pEventHandlerImpl p2pEventHandler;
  private String dbPath = "output-sync-service-test";

  /**
   * init context.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(SyncService.class);
  }

  /**
   * destroy.
   */
  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void test() {
    try {
      ReflectUtils.setFieldValue(service, "fetchFlag", true);
      ReflectUtils.setFieldValue(service, "handleFlag", true);
      service.init();
      Assert.assertTrue((boolean) ReflectUtils.getFieldObject(service, "fetchFlag"));
      Assert.assertTrue((boolean) ReflectUtils.getFieldObject(service, "handleFlag"));
      peer = context.getBean(PeerConnection.class);
      Assert.assertNull(peer.getSyncChainRequested());
      Channel c1 = new Channel();
      InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
      Field field = c1.getClass().getDeclaredField("inetSocketAddress");
      field.setAccessible(true);
      field.set(c1, a1.getAddress());
      peer.setChannel(c1);
      service.startSync(peer);
      ReflectUtils.setFieldValue(peer, "tronState", TronState.SYNCING);
      service.startSync(peer);
    } catch (Exception e) {
      // no need to deal with
    }
    service.close();
  }
}
