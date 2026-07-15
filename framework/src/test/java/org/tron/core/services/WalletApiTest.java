package org.tron.core.services;

import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.ClassLevelAppContextFixture;
import org.tron.common.TestConstants;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.TimeoutInterceptor;
import org.tron.core.config.args.Args;


@Slf4j
public class WalletApiTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

  private static TronApplicationContext context;
  private static final ClassLevelAppContextFixture APP_FIXTURE =
      new ClassLevelAppContextFixture();


  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[] {"-d", temporaryFolder.newFolder().toString(),
        "--p2p-disable", "true"}, TestConstants.TEST_CONF);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcEnable(true);
    context = APP_FIXTURE.createAndStart();
  }

  @Test
  public void listNodesTest() {
    String fullNode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getRpcPort());
    io.grpc.ManagedChannel channel = ManagedChannelBuilder.forTarget(fullNode)
        .usePlaintext()
        .intercept(new TimeoutInterceptor(5000))
        .build();
    try {
      WalletGrpc.WalletBlockingStub walletStub = WalletGrpc.newBlockingStub(channel);
      Assert.assertTrue(walletStub.listNodes(EmptyMessage.getDefaultInstance())
          .getNodesList().isEmpty());
    } finally {
      ClassLevelAppContextFixture.shutdownChannel(channel);
    }
  }

  @AfterClass
  public static void destroy() {
    APP_FIXTURE.close();
    Args.clearParam();
  }

}
