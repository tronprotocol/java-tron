package org.tron.core.capsule.utils;

import org.junit.Assert;
import org.junit.Test;

public class RLPListTest {

  @Test
  public void testRecursivePrint() {
    RLPItem element = new RLPItem("rlpItem".getBytes());
    Assert.assertEquals(new String(element.getRLPData()), "rlpItem");
    RLPList.recursivePrint(element);
    RLPList rlpList = new RLPList();
    rlpList.add(new RLPItem("rlpItem0".getBytes()));
    RLPList.recursivePrint(rlpList);
    Assert.assertThrows(RuntimeException.class, () -> RLPList.recursivePrint(null));
  }

  @Test
  public void testGetRLPData() {
    RLPList rlpList = new RLPList();
    rlpList.setRLPData("rlpData".getBytes());
    Assert.assertEquals(new String(rlpList.getRLPData()), "rlpData");
  }

}
