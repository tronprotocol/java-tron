package org.tron.core.net.service.nodepersist;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.JsonUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;


public class NodePersistServiceTest extends BaseTest {

  @Resource
  private NodePersistService nodePersistService;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        Constant.TEST_CONF);
  }

  @Test
  public void testDbRead() {
    byte[] DB_KEY_PEERS = "peers".getBytes();
    DBNode dbNode = new DBNode("localhost", 3306);
    List<DBNode> dbNodeList = new ArrayList<>();
    dbNodeList.add(dbNode);
    DBNodes dbNodes = new DBNodes();
    dbNodes.setNodes(dbNodeList);
    chainBaseManager.getCommonStore().put(DB_KEY_PEERS, new BytesCapsule(
        JsonUtil.obj2Json(dbNodes).getBytes()));

    Assert.assertEquals(1, nodePersistService.dbRead().size());
  }
}
