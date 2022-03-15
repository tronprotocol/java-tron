package org.tron.core.services.stop;

import java.io.File;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;

@Slf4j
public abstract class ConditionallyStopTest {


  protected  String dbPath;
  protected TronNetDelegate tronNetDelegate;
  protected long currentHeader = -1;
  private TronApplicationContext context;

  protected abstract void initParameter(CommonParameter parameter);

  protected abstract void check();

  protected abstract void initDbPath();


  @Before
  public void init() throws Exception {

    initDbPath();
    FileUtil.deleteDir(new File(dbPath));
    logger.info("Full node running.");
    Args.setParam(
        new String[] {
            "--output-directory", dbPath,
            "--storage-db-directory", "database",
            "--storage-index-directory", "index"
        },
        "config.conf"
    );
    CommonParameter parameter = Args.getInstance();
    parameter.setNodeListenPort(10000);
    initParameter(parameter);
    context = new TronApplicationContext(DefaultConfig.class);
    Application appT = ApplicationFactory.create(context);
    appT.initServices(parameter);
    appT.startServices();
    appT.startup();
    tronNetDelegate = context.getBean(TronNetDelegate.class);
    tronNetDelegate.setTest(true);
    currentHeader = tronNetDelegate.getDbManager().getDynamicPropertiesStore()
        .getLatestBlockHeaderNumberFromDB();
  }

  @After
  public void destroy() {
    Args.clearParam();
    Collection<PeerConnection> peerConnections = ReflectUtils
        .invokeMethod(tronNetDelegate, "getActivePeer");
    for (PeerConnection peer : peerConnections) {
      peer.close();
    }
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testStop() throws InterruptedException {
    while (!tronNetDelegate.isHitDown()) {
      Thread.sleep(1);
    }
    Assert.assertTrue(tronNetDelegate.isHitDown());
    check();
  }

}
