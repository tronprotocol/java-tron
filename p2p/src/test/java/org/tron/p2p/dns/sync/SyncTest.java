package org.tron.p2p.dns.sync;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.p2p.dns.TreeTest;
import org.tron.p2p.dns.lookup.LookUpTxt;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.LinkEntry;
import org.tron.p2p.dns.tree.NodesEntry;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.TXTRecord;

public class SyncTest {

  private static final String DOMAIN = "nodes.example.invalid";

  @Test
  public void testSync() throws Exception {
    Tree published = TreeTest.signedTree(new Tree(), "192.0.2.1", "192.0.2.2");
    String url = LinkEntry.buildRepresent(published.getBase32PublicKey(), DOMAIN);
    Client syncClient = new Client();
    try (MockedStatic<LookUpTxt> lookup = Mockito.mockStatic(LookUpTxt.class)) {
      lookup.when(() -> LookUpTxt.lookUpTxt(DOMAIN)).thenReturn(new TXTRecord(
          Name.fromString(DOMAIN + "."), DClass.IN, 60, published.getRootEntry().toFormat()));
      for (Map.Entry<String, Entry> entry : published.getEntries().entrySet()) {
        lookup.when(() -> LookUpTxt.lookUpTxt(entry.getKey(), DOMAIN)).thenReturn(new TXTRecord(
            Name.fromString(entry.getKey() + "." + DOMAIN + "."), DClass.IN, 60,
            entry.getValue().toString()));
      }
      lookup.when(() -> LookUpTxt.joinTXTRecord(Mockito.any(TXTRecord.class)))
          .thenCallRealMethod();
      Tree received = new Tree();
      syncClient.syncTree(url, new ClientTree(syncClient), received);
      Assert.assertEquals(published.getRootEntry().toFormat(), received.getRootEntry().toFormat());
      Assert.assertEquals(published.getEntries().keySet(), received.getEntries().keySet());
      Assert.assertEquals(published.getDnsNodes(), received.getDnsNodes());
    } finally {
      syncClient.close();
    }
  }

  @Test
  public void mismatchedHashIsRejectedBeforeNodesPayloadIsDecoded() {
    assertHashMismatch(Algorithm.encode32AndTruncate("expected entry"), "nodes:!");
  }

  @Test
  public void hashMustMatchTheOriginalTxtRatherThanTheParsedBranch() {
    String branch = "tree-branch:" + Algorithm.encode32AndTruncate("child");
    // Branch parsing drops trailing empty children. It must not hide modified wire content.
    assertHashMismatch(Algorithm.encode32AndTruncate(branch), branch + ",");
  }

  @Test
  public void matchingHashesStillParseNodesBranchesAndLinks() throws Exception {
    Tree tree = TreeTest.signedTree(new Tree(), "192.0.2.1", "192.0.2.2");
    for (Entry expected : tree.getEntries().values()) {
      String txt = expected.toString();
      Entry actual = Client.parseEntry(DOMAIN, Algorithm.encode32AndTruncate(txt), txt);
      Assert.assertEquals(expected.getClass(), actual.getClass());
      Assert.assertEquals(txt, actual.toString());
      if (actual instanceof NodesEntry) {
        Assert.assertEquals(((NodesEntry) expected).getNodes(), ((NodesEntry) actual).getNodes());
      }
    }
  }

  private static void assertHashMismatch(String hash, String txt) {
    DnsException error = Assert.assertThrows(DnsException.class,
        () -> Client.parseEntry(DOMAIN, hash, txt));
    Assert.assertEquals(TypeEnum.HASH_MISS_MATCH, error.getType());
  }
}
