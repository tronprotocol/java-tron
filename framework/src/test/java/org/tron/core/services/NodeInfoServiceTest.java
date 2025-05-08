package org.tron.core.services;

import static org.mockito.Mockito.mock;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.entity.NodeInfo;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.p2p.connection.Channel;
import org.tron.program.Version;


@Slf4j
public class NodeInfoServiceTest {

  private NodeInfoService nodeInfoService;
  private WitnessProductBlockService witnessProductBlockService;
  private P2pEventHandlerImpl p2pEventHandler;

  public NodeInfoServiceTest(TronApplicationContext context) {
    nodeInfoService = context.getBean("nodeInfoService", NodeInfoService.class);
    witnessProductBlockService = context.getBean(WitnessProductBlockService.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
  }

  public void test() {
    BlockCapsule blockCapsule1 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        100, ByteString.EMPTY);
    BlockCapsule blockCapsule2 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        200, ByteString.EMPTY);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule1);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule2);

    //add peer
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    p2pEventHandler.onConnect(c1);

    //test setConnectInfo
    NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
    Assert.assertEquals(nodeInfo.getConfigNodeInfo().getCodeVersion(), Version.getVersion());
    Assert.assertEquals(nodeInfo.getCheatWitnessInfoMap().size(), 1);
    logger.info("{}", JSON.toJSONString(nodeInfo));
  }

}
