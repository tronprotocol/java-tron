package org.tron.p2p.dns;


import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.sync.LinkCache;

public class LinkCacheTest {

  @Test
  public void testLinkCache() {
    LinkCache lc = new LinkCache();

    lc.addLink("1", "2");
    Assert.assertTrue(lc.isChanged());

    lc.setChanged(false);
    lc.addLink("1", "2");
    Assert.assertFalse(lc.isChanged());

    lc.addLink("2", "3");
    lc.addLink("3", "1");
    lc.addLink("2", "4");

    for (String key : lc.getBackrefs().keySet()) {
      System.out.println(key + "：" + StringUtils.join(lc.getBackrefs().get(key), ","));
    }
    Assert.assertTrue(lc.isContainInOtherLink("3"));
    Assert.assertFalse(lc.isContainInOtherLink("6"));

    lc.resetLinks("1", null);
    Assert.assertTrue(lc.isChanged());
    Assert.assertEquals(0, lc.getBackrefs().size());
  }
}
