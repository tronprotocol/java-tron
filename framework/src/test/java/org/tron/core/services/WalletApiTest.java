package org.tron.core.services;

import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.client.Configuration;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;


@Slf4j
public class WalletApiTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static TronApplicationContext context;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private RpcApiService rpcApiService;
  private Application appT;

  @Before
  public void init() throws IOException {
    Args.setParam(new String[]{ "-d", temporaryFolder.newFolder().toString(),
        "--p2p-disable", "true"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    appT.startup();
  }

  @Test
  public void listNodesTest() {
    WalletGrpc.WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext()
            .build());
    Assert.assertTrue(walletStub.listNodes(EmptyMessage.getDefaultInstance())
        .getNodesList().size() == 0);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

}
