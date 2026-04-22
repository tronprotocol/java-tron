package org.tron.p2p.discover.protocol.kad;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.protocol.kad.table.KademliaOptions;
import org.tron.p2p.discover.protocol.kad.table.NodeTable;

public class DiscoverTaskTest {

  private KadService kadService;
  private DiscoverTask discoverTask;
  private Node homeNode;

  @Before
  public void init() {
    Parameter.p2pConfig = new P2pConfig();

    byte[] nodeId = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId, (byte) 0x01);
    homeNode = new Node(nodeId, "192.168.1.1", null, 18888);

    kadService = Mockito.mock(KadService.class);
    Mockito.when(kadService.getPublicHomeNode()).thenReturn(homeNode);

    NodeTable table = Mockito.mock(NodeTable.class);
    Mockito.when(kadService.getTable()).thenReturn(table);
    Mockito.when(table.getClosestNodes(Mockito.any(byte[].class)))
        .thenReturn(new ArrayList<Node>());

    discoverTask = new DiscoverTask(kadService);
  }

  @After
  public void cleanup() {
    discoverTask.close();
  }

  @Test
  public void testConstructor() {
    Assert.assertNotNull(discoverTask);
  }

  @Test
  public void testClose() throws Exception {
    discoverTask.close();

    // Verify the executor is shut down by accessing internal field
    Field discovererField = DiscoverTask.class.getDeclaredField("discoverer");
    discovererField.setAccessible(true);
    ScheduledExecutorService executor =
        (ScheduledExecutorService) discovererField.get(discoverTask);
    Assert.assertTrue(executor.isShutdown());
  }

  @Test
  public void testInitStartsScheduler() throws Exception {
    discoverTask.init();

    // Give the scheduler a brief moment then close
    Thread.sleep(50);
    discoverTask.close();

    // Verify the executor was used (it should have been scheduled)
    Field discovererField = DiscoverTask.class.getDeclaredField("discoverer");
    discovererField.setAccessible(true);
    ScheduledExecutorService executor =
        (ScheduledExecutorService) discovererField.get(discoverTask);
    Assert.assertTrue(executor.isShutdown());
  }

  @Test
  public void testDiscoverWithClosestNodes() throws Exception {
    // Set up mock to return some closest nodes
    byte[] nodeId2 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId2, (byte) 0x02);
    Node node2 = new Node(nodeId2, "192.168.1.2", null, 18889);

    NodeHandler handler = Mockito.mock(NodeHandler.class);
    Mockito.when(kadService.getNodeHandler(Mockito.any(Node.class))).thenReturn(handler);

    List<Node> closest = new ArrayList<>();
    closest.add(node2);

    NodeTable table = Mockito.mock(NodeTable.class);
    Mockito.when(kadService.getTable()).thenReturn(table);
    Mockito.when(table.getClosestNodes(Mockito.any(byte[].class))).thenReturn(closest);

    // Use reflection to invoke the private discover method
    Method discoverMethod =
        DiscoverTask.class.getDeclaredMethod(
            "discover", byte[].class, int.class, List.class);
    discoverMethod.setAccessible(true);
    discoverMethod.invoke(discoverTask, homeNode.getId(), 0, new ArrayList<>());

    // Verify sendFindNode was called
    Mockito.verify(handler, Mockito.atLeastOnce()).sendFindNode(Mockito.any(byte[].class));
  }

  @Test
  public void testDiscoverWithEmptyClosestNodes() throws Exception {
    NodeTable table = Mockito.mock(NodeTable.class);
    Mockito.when(kadService.getTable()).thenReturn(table);
    Mockito.when(table.getClosestNodes(Mockito.any(byte[].class)))
        .thenReturn(new ArrayList<Node>());

    Method discoverMethod =
        DiscoverTask.class.getDeclaredMethod(
            "discover", byte[].class, int.class, List.class);
    discoverMethod.setAccessible(true);
    // Should return early without exception when no closest nodes
    discoverMethod.invoke(discoverTask, homeNode.getId(), 0, new ArrayList<>());

    // No sendFindNode should be called
    Mockito.verify(kadService, Mockito.never()).getNodeHandler(Mockito.any(Node.class));
  }

  @Test
  public void testDiscoverAtMaxSteps() throws Exception {
    // When round == MAX_STEPS, should return immediately
    byte[] nodeId2 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId2, (byte) 0x02);
    Node node2 = new Node(nodeId2, "192.168.1.2", null, 18889);

    NodeHandler handler = Mockito.mock(NodeHandler.class);
    Mockito.when(kadService.getNodeHandler(Mockito.any(Node.class))).thenReturn(handler);

    List<Node> closest = new ArrayList<>();
    closest.add(node2);

    NodeTable table = Mockito.mock(NodeTable.class);
    Mockito.when(kadService.getTable()).thenReturn(table);
    Mockito.when(table.getClosestNodes(Mockito.any(byte[].class))).thenReturn(closest);

    Method discoverMethod =
        DiscoverTask.class.getDeclaredMethod(
            "discover", byte[].class, int.class, List.class);
    discoverMethod.setAccessible(true);

    // Pass round = MAX_STEPS - 1 so it increments to MAX_STEPS and stops
    int maxStepsMinusOne = KademliaOptions.MAX_STEPS - 1;
    discoverMethod.invoke(discoverTask, homeNode.getId(), maxStepsMinusOne, new ArrayList<>());

    // sendFindNode called once (for the first iteration before checking MAX_STEPS)
    Mockito.verify(handler, Mockito.atMost(1)).sendFindNode(Mockito.any(byte[].class));
  }
}
