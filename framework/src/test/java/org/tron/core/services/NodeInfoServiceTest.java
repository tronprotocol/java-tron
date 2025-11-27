package org.tron.core.services;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import java.net.InetSocketAddress;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.tron.common.BaseTest;
import org.tron.common.entity.NodeInfo;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetService;
import org.tron.core.net.peer.PeerManager;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.connection.Channel;
import org.tron.program.Version;


@Slf4j
public class NodeInfoServiceTest extends BaseTest {

  @Resource
  protected NodeInfoService nodeInfoService;
  @Resource
  protected WitnessProductBlockService witnessProductBlockService;
  @Resource
  private P2pEventHandlerImpl p2pEventHandler;
  @Resource
  private TronNetService tronNetService;


  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        Constant.TEST_CONF);
  }

  @After
  public void clearPeers() {
    closePeer();
  }

  @Test
  public void test() {
    BlockCapsule blockCapsule1 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        100, ByteString.EMPTY);
    BlockCapsule blockCapsule2 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        200, ByteString.EMPTY);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule1);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule2);

    addPeer();

    //test setConnectInfo
    NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
    Assert.assertEquals(nodeInfo.getConfigNodeInfo().getCodeVersion(), Version.getVersion());
    Assert.assertEquals(1, nodeInfo.getCheatWitnessInfoMap().size());
    logger.info("{}", JSON.toJSONString(nodeInfo));
  }

  private void addPeer() {
    int port = PublicMethod.chooseRandomPort();
    P2pConfig p2pConfig = new P2pConfig();
    p2pConfig.setIp("127.0.0.1");
    p2pConfig.setPort(port);
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", p2pConfig);
    TronNetService.getP2pService().start(p2pConfig);

    ApplicationContext ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler,
        "ctx");
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());

    PeerManager.add(ctx, c1);
  }
}
