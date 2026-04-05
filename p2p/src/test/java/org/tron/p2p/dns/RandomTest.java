package org.tron.p2p.dns;


import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.dns.sync.Client;
import org.tron.p2p.dns.sync.RandomIterator;

public class RandomTest {

  @Test
  public void testRandomIterator() {
    Parameter.p2pConfig = new P2pConfig();
    List<String> treeUrls = new ArrayList<>();
    treeUrls.add(
        "tree://AKMQMNAJJBL73LXWPXDI4I5ZWWIZ4AWO34DWQ636QOBBXNFXH3LQS@nile.trondisco.net");
    //treeUrls.add(
    //    "tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@shasta.nftderby1.net");
    Parameter.p2pConfig.setTreeUrls(treeUrls);

    Client syncClient = new Client();

    RandomIterator randomIterator = syncClient.newIterator();
    int count = 0;
    while (count < 20) {
      DnsNode dnsNode = randomIterator.next();
      Assert.assertNotNull(dnsNode);
      Assert.assertNull(dnsNode.getId());
      count += 1;
      System.out.println("get Node success:" + dnsNode.format());
    }
  }
}
