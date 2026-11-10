package org.tron.p2p.dns.sync;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.dns.tree.BranchEntry;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.LinkEntry;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;

public class SubtreeSyncTest {

  private final StubClient client = new StubClient();
  private final LinkEntry location = new LinkEntry("", "example.invalid", "");

  @After
  public void tearDown() {
    client.close();
  }

  @Test
  public void repeatedBranchesAreResolvedOnce() throws Exception {
    String root = branch();
    for (int i = 0; i < 12; i++) {
      root = branch(root, root);
    }

    SubtreeSync sync = new SubtreeSync(client, location, root, false);
    Map<String, Entry> entries = new HashMap<>();
    sync.resolveAll(entries);

    Assert.assertTrue(sync.done());
    Assert.assertEquals(13, entries.size());
    Assert.assertEquals(13, client.calls);
  }

  @Test
  public void sharedDescendantsCanBeResolvedAgainInANewSync() throws Exception {
    String root = sharedSubtree();
    Map<String, Entry> entries = new HashMap<>();
    new SubtreeSync(client, location, root, false).resolveAll(entries);
    Assert.assertEquals(4, entries.size());
    Assert.assertEquals(4, client.calls);

    new SubtreeSync(client, location, root, false).resolveAll(entries);
    Assert.assertEquals(8, client.calls);
  }

  @Test
  public void resolvingOutOfQueueOrderDoesNotRepeatSharedDescendants() throws Exception {
    SubtreeSync sync = new SubtreeSync(client, location, sharedSubtree(), false);
    // ClientTree's random traversal resolves an indexed entry before removing it.
    while (!sync.done()) {
      int index = sync.missing.size() - 1;
      sync.resolveNext(sync.missing.get(index));
      sync.missing.remove(index);
    }

    Assert.assertEquals(4, client.calls);
  }

  @Test
  public void failedResolutionCanBeRetried() throws Exception {
    String leaf = branch();
    String root = branch(leaf, leaf);
    SubtreeSync sync = new SubtreeSync(client, location, root, false);
    Map<String, Entry> entries = new HashMap<>();
    client.failOnce = leaf;

    try {
      sync.resolveAll(entries);
      Assert.fail("Expected a resolution failure");
    } catch (DnsException expected) {
      Assert.assertEquals(TypeEnum.OTHER_ERROR, expected.getType());
    }
    Assert.assertEquals(leaf, sync.missing.peek());

    sync.resolveAll(entries);
    Assert.assertTrue(sync.done());
    Assert.assertEquals(2, entries.size());
    Assert.assertEquals(3, client.calls);
  }

  private String sharedSubtree() {
    String leaf = branch();
    String left = branch(leaf);
    String right = branch(leaf, left);
    return branch(left, right);
  }

  private String branch(String... children) {
    BranchEntry entry = new BranchEntry(children);
    String hash = Algorithm.encode32AndTruncate(entry.toString());
    client.entries.put(hash, entry);
    return hash;
  }

  private static class StubClient extends Client {
    private final Map<String, Entry> entries = new HashMap<>();
    private int calls;
    private String failOnce;

    @Override
    public Entry resolveEntry(String domain, String hash) throws DnsException {
      calls++;
      if (hash.equals(failOnce)) {
        failOnce = null;
        throw new DnsException(TypeEnum.OTHER_ERROR, "temporary resolution failure");
      }
      return entries.get(hash);
    }
  }
}
