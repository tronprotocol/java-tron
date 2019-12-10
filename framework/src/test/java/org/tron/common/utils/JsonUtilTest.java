package org.tron.common.utils;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.discover.node.DBNode;
import org.tron.common.overlay.discover.node.DBNodeStats;
import org.tron.common.overlay.discover.node.Node;

public class JsonUtilTest {

  @Test
  public void test() {
    DBNode dbNode = new DBNode();
    DBNodeStats dbNodeStats = new DBNodeStats(Node.getNodeId(), "1.0.0.1", 1000, 100);
    List nodes = new ArrayList();
    nodes.add(dbNodeStats);
    dbNode.setNodes(nodes);

    String jsonString = JsonUtil.obj2Json(dbNode);

    DBNode dbNode2 = JsonUtil.json2Obj(jsonString, DBNode.class);

    dbNodeStats = dbNode2.getNodes().get(0);

    Assert.assertEquals(dbNodeStats.getHost(), "1.0.0.1");
    Assert.assertEquals(dbNodeStats.getPort(), 1000);
    Assert.assertEquals(dbNodeStats.getReputation(), 100);
  }
}
