package org.tron.core.services;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.entity.NodeInfo;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.program.Version;


@Slf4j
public class NodeInfoServiceTest extends BaseTest {

  @Resource
  protected NodeInfoService nodeInfoService;
  @Resource
  protected WitnessProductBlockService witnessProductBlockService;


  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        Constant.TEST_CONF);
  }

  @Test
  public void test() {
    BlockCapsule blockCapsule1 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        100, ByteString.EMPTY);
    BlockCapsule blockCapsule2 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        200, ByteString.EMPTY);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule1);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule2);

    //test setConnectInfo
    NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
    Assert.assertEquals(nodeInfo.getConfigNodeInfo().getCodeVersion(), Version.getVersion());
    Assert.assertEquals(1, nodeInfo.getCheatWitnessInfoMap().size());
    logger.info("{}", JSON.toJSONString(nodeInfo));
  }

}
