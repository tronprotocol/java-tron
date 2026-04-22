package org.tron.p2p.dns;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.dns.sync.Client;
import org.tron.p2p.dns.sync.RandomIterator;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.dns.update.PublishService;

public class DnsManagerTest {

  @Before
  public void initP2pConfig() {
    // Node.getPreferInetSocketAddress() reads Parameter.p2pConfig.getIp()/getIpv6().
    // Set a fixed non-empty IPv4 so the tests don't rely on external IP discovery
    // and don't NPE when another test leaves Parameter.p2pConfig as null.
    P2pConfig cfg = new P2pConfig();
    cfg.setIp("127.0.0.1");
    cfg.setIpv6(null);
    Parameter.p2pConfig = cfg;
  }

  private void setStaticField(String fieldName, Object value) throws Exception {
    Field field = DnsManager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  @Test
  public void testCloseWithNullFields() throws Exception {
    // Set all fields to null
    setStaticField("publishService", null);
    setStaticField("syncClient", null);
    setStaticField("randomIterator", null);

    // Should not throw NullPointerException
    DnsManager.close();
  }

  @Test
  public void testCloseCallsComponentClose() throws Exception {
    PublishService mockPublish = mock(PublishService.class);
    Client mockClient = mock(Client.class);
    RandomIterator mockIterator = mock(RandomIterator.class);

    setStaticField("publishService", mockPublish);
    setStaticField("syncClient", mockClient);
    setStaticField("randomIterator", mockIterator);

    DnsManager.close();

    verify(mockPublish).close();
    verify(mockClient).close();
    verify(mockIterator).close();
  }

  @Test
  public void testGetDnsNodesEmptyTrees() throws Exception {
    Client mockClient = mock(Client.class);
    Map<String, Tree> emptyTrees = new HashMap<>();
    when(mockClient.getTrees()).thenReturn(emptyTrees);

    setStaticField("syncClient", mockClient);
    setStaticField("localIpSet", new HashSet<String>());

    List<DnsNode> nodes = DnsManager.getDnsNodes();
    Assert.assertNotNull(nodes);
    Assert.assertTrue(nodes.isEmpty());
  }

  @Test
  public void testGetDnsNodesFiltersLocalIps() throws Exception {
    // Create a tree with known nodes
    DnsNode node1 = new DnsNode(null, "192.168.0.1", null, 10000);
    DnsNode node2 = new DnsNode(null, "10.0.0.1", null, 10000);
    List<DnsNode> nodeList = Arrays.asList(node1, node2);
    List<String> enrList = Tree.merge(nodeList, 5);

    Tree tree = new Tree();
    tree.makeTree(1, enrList, new ArrayList<String>(), null);

    Map<String, Tree> trees = new HashMap<>();
    trees.put("test-tree", tree);

    Client mockClient = mock(Client.class);
    when(mockClient.getTrees()).thenReturn(trees);

    Set<String> localIps = new HashSet<>();
    localIps.add("192.168.0.1");

    setStaticField("syncClient", mockClient);
    setStaticField("localIpSet", localIps);

    List<DnsNode> result = DnsManager.getDnsNodes();
    // 192.168.0.1 should be filtered out
    for (DnsNode node : result) {
      if (node.getPreferInetSocketAddress() != null) {
        String addr = node.getPreferInetSocketAddress().getAddress().getHostAddress();
        Assert.assertNotEquals("192.168.0.1", addr);
      }
    }
  }

  @Test
  public void testGetDnsNodesReturnsConnectableNodes() throws Exception {
    DnsNode node1 = new DnsNode(null, "8.8.8.8", null, 10000);
    List<DnsNode> nodeList = Arrays.asList(node1);
    List<String> enrList = Tree.merge(nodeList, 5);

    Tree tree = new Tree();
    tree.makeTree(1, enrList, new ArrayList<String>(), null);

    Map<String, Tree> trees = new HashMap<>();
    trees.put("test-tree", tree);

    Client mockClient = mock(Client.class);
    when(mockClient.getTrees()).thenReturn(trees);

    setStaticField("syncClient", mockClient);
    setStaticField("localIpSet", new HashSet<String>());

    List<DnsNode> result = DnsManager.getDnsNodes();
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGetRandomNodes() throws Exception {
    RandomIterator mockIterator = mock(RandomIterator.class);
    when(mockIterator.next()).thenReturn(null);

    setStaticField("randomIterator", mockIterator);

    org.tron.p2p.discover.Node node = DnsManager.getRandomNodes();
    Assert.assertNull(node);
    verify(mockIterator).next();
  }
}
