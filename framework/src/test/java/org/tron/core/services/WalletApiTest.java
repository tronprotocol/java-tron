package org.tron.core.services;

import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;


@Slf4j
public class WalletApiTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static TronApplicationContext context;
  private static Application appT;


  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{ "-d", temporaryFolder.newFolder().toString(),
        "--p2p-disable", "true"}, Constant.TEST_CONF);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcEnable(true);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    appT.startup();
  }

  @Test
  public void listNodesTest() {
    String fullNode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getRpcPort());
    WalletGrpc.WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullNode)
            .usePlaintext()
            .build());
    Assert.assertTrue(walletStub.listNodes(EmptyMessage.getDefaultInstance())
        .getNodesList().isEmpty());
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

}
