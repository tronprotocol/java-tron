package org.tron.core.net;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j
public class BaseNet {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static String dbDirectory = "net-database";
  private static String indexDirectory = "net-index";
  private static int port = 10000;

  protected static TronApplicationContext context;

  private static Application appT;
  private static TronNetDelegate tronNetDelegate;

  private static ExecutorService executorService = Executors.newFixedThreadPool(1);

  @BeforeClass
  public static void init() throws Exception {
    executorService.execute(() -> {
      logger.info("Full node running.");
      try {
        Args.setParam(
            new String[]{
                "--output-directory", temporaryFolder.newFolder().toString(),
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            "config.conf"
        );
      } catch (IOException e) {
        Assert.fail("create temp db directory failed");
      }
      CommonParameter parameter = Args.getInstance();
      parameter.setNodeListenPort(port);
      parameter.getSeedNode().getAddressList().clear();
      parameter.setNodeExternalIp(Constant.LOCAL_HOST);
      parameter.setRpcEnable(true);
      parameter.setRpcPort(PublicMethod.chooseRandomPort());
      parameter.setRpcSolidityEnable(false);
      parameter.setRpcPBFTEnable(false);
      parameter.setFullNodeHttpEnable(false);
      parameter.setSolidityNodeHttpEnable(false);
      parameter.setPBFTHttpEnable(false);
      context = new TronApplicationContext(DefaultConfig.class);
      appT = ApplicationFactory.create(context);
      appT.startup();
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        //ignore
      }
      tronNetDelegate = context.getBean(TronNetDelegate.class);
      appT.blockUntilShutdown();
    });
    int tryTimes = 0;
    do {
      Thread.sleep(3000); //coverage consumerInvToSpread,consumerInvToFetch in AdvService.init
    } while (++tryTimes < 100 && tronNetDelegate == null);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
  }
}
