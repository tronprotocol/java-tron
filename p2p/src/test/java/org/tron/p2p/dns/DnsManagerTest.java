package org.tron.p2p.dns;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.dns.sync.Client;
import org.tron.p2p.dns.tree.Tree;

/**
 * Covers DnsManager.getDnsNodes, which turns synced DNS trees into the connectable
 * node list. Two filters matter: a node with no usable address for this host must be
 * dropped, and this host's own addresses must not be handed back as peers to dial.
 */
public class DnsManagerTest {

  private Client syncClient;
  private Object priorSyncClient;
  private Object priorLocalIpSet;

  private static Object getStatic(String name) throws Exception {
    Field field = DnsManager.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(null);
  }

  private static void setStatic(String name, Object value) throws Exception {
    Field field = DnsManager.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }

  @Before
  public void setUp() throws Exception {
    // getPreferInetSocketAddress() consults the local config to decide whether v4 or
    // v6 is usable, so it must be initialised before any node is evaluated
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("1.1.1.1");
    Parameter.p2pConfig.setIpv6(null);

    // DnsManager's collaborators are process-wide statics and framework's test task
    // reuses a JVM across up to 100 classes (forkEvery = 100), so they are restored in
    // tearDown rather than left pointing at a mock from a finished test class
    priorSyncClient = getStatic("syncClient");
    priorLocalIpSet = getStatic("localIpSet");

    syncClient = mock(Client.class);
    setStatic("syncClient", syncClient);
    setStatic("localIpSet", new HashSet<String>());
  }

  @After
  public void tearDown() throws Exception {
    setStatic("syncClient", priorSyncClient);
    setStatic("localIpSet", priorLocalIpSet);
  }

  private void serveTree(List<DnsNode> nodes) {
    Tree tree = mock(Tree.class);
    when(tree.getDnsNodes()).thenReturn(nodes);
    Map<String, Tree> trees = new HashMap<>();
    trees.put("tree://example.org", tree);
    when(syncClient.getTrees()).thenReturn(trees);
  }

  @Test
  public void returnsEmptyWhenNoTreesAreSynced() {
    when(syncClient.getTrees()).thenReturn(new HashMap<>());
    Assert.assertTrue(DnsManager.getDnsNodes().isEmpty());
  }

  @Test
  public void returnsConnectableV4Nodes() throws Exception {
    List<DnsNode> nodes = new ArrayList<>();
    nodes.add(new DnsNode(null, "2.2.2.2", null, 18888));
    nodes.add(new DnsNode(null, "3.3.3.3", null, 18888));
    serveTree(nodes);

    List<DnsNode> result = DnsManager.getDnsNodes();
    Assert.assertEquals(2, result.size());
  }

  @Test
  public void dropsNodesWithNoAddressUsableByThisHost() throws Exception {
    // this host has no IPv6 configured, so a v6-only peer is not connectable
    List<DnsNode> nodes = new ArrayList<>();
    nodes.add(new DnsNode(null, "2.2.2.2", null, 18888));
    nodes.add(new DnsNode(null, null, "2001:db8::1", 18888));
    serveTree(nodes);

    List<DnsNode> result = DnsManager.getDnsNodes();
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("2.2.2.2", result.get(0).getHostV4());
  }

  @Test
  public void dropsThisHostsOwnAddresses() throws Exception {
    Set<String> local = new HashSet<>();
    local.add("2.2.2.2");
    setStatic("localIpSet", local);

    List<DnsNode> nodes = new ArrayList<>();
    nodes.add(new DnsNode(null, "2.2.2.2", null, 18888));
    nodes.add(new DnsNode(null, "3.3.3.3", null, 18888));
    serveTree(nodes);

    List<DnsNode> result = DnsManager.getDnsNodes();
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("3.3.3.3", result.get(0).getHostV4());
  }

  @Test
  public void deduplicatesAcrossTrees() throws Exception {
    // the same peer advertised by two trees must be dialled once
    List<DnsNode> first = new ArrayList<>();
    first.add(new DnsNode(null, "2.2.2.2", null, 18888));
    List<DnsNode> second = new ArrayList<>();
    second.add(new DnsNode(null, "2.2.2.2", null, 18888));

    Tree treeA = mock(Tree.class);
    when(treeA.getDnsNodes()).thenReturn(first);
    Tree treeB = mock(Tree.class);
    when(treeB.getDnsNodes()).thenReturn(second);
    Map<String, Tree> trees = new HashMap<>();
    trees.put("tree://a.example.org", treeA);
    trees.put("tree://b.example.org", treeB);
    when(syncClient.getTrees()).thenReturn(trees);

    Assert.assertEquals(1, DnsManager.getDnsNodes().size());
  }
}
