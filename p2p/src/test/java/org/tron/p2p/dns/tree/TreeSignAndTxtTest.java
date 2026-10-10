package org.tron.p2p.dns.tree;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.dns.update.AliClient;

/**
 * The publish side of Tree: signing the root, and rendering the tree as the TXT
 * record map that goes to the DNS provider. Both Aliyun (bare "@" root) and
 * Route53 (fully qualified names) shapes are covered.
 */
public class TreeSignAndTxtTest {

  private static final String PRIVATE_KEY =
      "b71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291";
  private static final String DOMAIN = "nodes.example.org";

  private static List<String> enrsFor(String... ips) throws UnknownHostException {
    List<String> enrs = new ArrayList<>();
    for (String ip : ips) {
      enrs.add(Entry.nodesPrefix
          + DnsNode.compress(Collections.singletonList(new DnsNode(null, ip, null, 10000))));
    }
    return enrs;
  }

  private static Tree signedTree(int seq) throws Exception {
    Tree tree = new Tree();
    tree.makeTree(seq, enrsFor("192.168.0.1", "192.168.0.2", "192.168.0.3"),
        new ArrayList<String>(), PRIVATE_KEY);
    return tree;
  }

  @Test
  public void makeTreeSignsTheRootAndExposesThePublicKey() throws Exception {
    Tree tree = signedTree(7);

    Assert.assertEquals(7, tree.getSeq());
    Assert.assertNotNull(tree.getBase32PublicKey());
    Assert.assertFalse(tree.getBase32PublicKey().isEmpty());
    Assert.assertFalse(tree.getNodesEntry().isEmpty());
    Assert.assertTrue(tree.getLinksEntry().isEmpty());
  }

  @Test
  public void seqIsMutableAndResigningSucceeds() throws Exception {
    Tree tree = signedTree(1);
    tree.setSeq(42);
    Assert.assertEquals(42, tree.getSeq());
    // deploy() bumps the sequence and re-signs; that must not throw.
    tree.sign();
    Assert.assertEquals(42, tree.getSeq());
  }

  @Test
  public void toTxtQualifiesEveryHashWithTheRootDomain() throws Exception {
    Tree tree = signedTree(1);
    Map<String, String> records = tree.toTXT(DOMAIN);

    Assert.assertTrue("the root record is keyed by the domain itself",
        records.containsKey(DOMAIN));
    Assert.assertTrue(records.get(DOMAIN).startsWith(Entry.rootPrefix));

    int subdomains = 0;
    for (Map.Entry<String, String> record : records.entrySet()) {
      if (record.getKey().equals(DOMAIN)) {
        continue;
      }
      subdomains++;
      Assert.assertTrue(record.getKey() + " should sit under the domain",
          record.getKey().endsWith("." + DOMAIN));
      Assert.assertEquals("keys are lower-cased",
          record.getKey().toLowerCase(java.util.Locale.ROOT), record.getKey());
    }
    Assert.assertTrue(subdomains > 0);
  }

  @Test
  public void toTxtWithoutARootDomainUsesTheAliyunRootSymbol() throws Exception {
    Tree tree = signedTree(1);
    Map<String, String> records = tree.toTXT(null);

    Assert.assertTrue(records.containsKey(AliClient.aliyunRoot));
    Assert.assertTrue(records.get(AliClient.aliyunRoot).startsWith(Entry.rootPrefix));
    for (String key : records.keySet()) {
      Assert.assertFalse("bare hashes only, no domain suffix", key.endsWith("." + DOMAIN));
    }
  }

  @Test
  public void entryAccessorsAgreeWithEachOther() throws Exception {
    Tree tree = signedTree(1);

    Assert.assertEquals(tree.getNodesEntry().size(), tree.getNodesMap().size());
    Assert.assertEquals(tree.getLinksEntry().size(), tree.getLinksMap().size());
    Assert.assertFalse(tree.getDnsNodes().isEmpty());
    // Every entry is either a node set, a link, or a branch.
    Assert.assertEquals(tree.getEntries().size(),
        tree.getNodesEntry().size() + tree.getLinksEntry().size()
            + tree.getBranchesEntry().size());
  }

  @Test
  public void mergeGroupsByNetworkAndRespectsTheBatchSize() throws Exception {
    List<DnsNode> nodes = Arrays.asList(
        new DnsNode(null, "192.168.0.1", null, 10000),
        new DnsNode(null, "192.168.0.2", null, 10000),
        new DnsNode(null, "10.0.0.1", null, 10000));

    // Nodes in different /8 networks are never merged into one entry.
    List<String> merged = Tree.merge(new ArrayList<>(nodes), 10);
    Assert.assertEquals(2, merged.size());
    for (String entry : merged) {
      Assert.assertTrue(entry.startsWith(Entry.nodesPrefix));
    }

    // A batch size of one puts every node in its own entry.
    Assert.assertEquals(3, Tree.merge(new ArrayList<>(nodes), 1).size());
  }

  @Test
  public void mergeOfNothingProducesNothing() {
    Assert.assertTrue(Tree.merge(new ArrayList<DnsNode>(), 10).isEmpty());
  }

  @Test
  public void signingWithoutAPrivateKeyIsSilentlySkipped() throws Exception {
    Tree tree = new Tree();
    tree.makeTree(1, enrsFor("192.168.0.1"), new ArrayList<String>(), null);
    tree.sign();

    // sign() returns early on an empty key rather than refusing, so the tree is
    // left unsigned and with no public key. The publisher does not check either,
    // which is how an unsigned "tree://null@..." can reach DNS. Pinned here as
    // current behaviour, not as an endorsement.
    Assert.assertNull(tree.getBase32PublicKey());
    Assert.assertTrue(tree.toTXT(DOMAIN).get(DOMAIN).startsWith(Entry.rootPrefix));
  }
}
