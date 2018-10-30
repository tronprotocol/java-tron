package org.tron.core.services;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.entity.NodeInfo;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.node.BaseNetTest;
import org.tron.program.Version;

@Slf4j
public class NodeInfoServiceTest extends BaseNetTest {

  private static final String dbPath = "output-nodeImplTest-nodeinfo";
  private static final String dbDirectory = "db_nodeinfo_test";
  private static final String indexDirectory = "index_nodeinfo_test";
  private final static int port = 15899;

  private NodeInfoService nodeInfoService;
  private WitnessProductBlockService witnessProductBlockService;

  public NodeInfoServiceTest() {
    super(dbPath, dbDirectory, indexDirectory, port);
  }

  @Test
  public void test() {
    nodeInfoService = context.getBean(NodeInfoService.class);
    witnessProductBlockService = context.getBean(WitnessProductBlockService.class);
    BlockCapsule blockCapsule1 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        100, ByteString.EMPTY);
    BlockCapsule blockCapsule2 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        200, ByteString.EMPTY);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule1);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule2);
    NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
    Assert.assertEquals(nodeInfo.getConfigNodeInfo().getCodeVersion(), Version.getVersion());
    Assert.assertEquals(nodeInfo.getCheatWitnessInfoMap().size(), 1);
    logger.info("{}", JSON.toJSONString(nodeInfo));
  }
}
