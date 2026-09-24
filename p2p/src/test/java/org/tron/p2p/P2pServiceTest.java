package org.tron.p2p;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.discover.Node;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.stats.P2pStats;
import org.tron.p2p.utils.TestPort;

/**
 * P2pService is the library's public entry point: everything java-tron calls
 * goes through it. Exercised against a started service on a free port with
 * discovery off, so no traffic leaves the host.
 */
public class P2pServiceTest {

  private P2pConfig saved;
  private P2pService service;
  private List<P2pEventHandler> savedHandlerList;
  private Map<Byte, P2pEventHandler> savedHandlerMap;

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setIp("127.0.0.1");
    // A fixed port would collide with the other p2p tests sharing this task.
    config.setPort(TestPort.choose());
    config.setDiscoverEnable(false);
    config.setDisconnectionPolicyEnable(false);

    // register() writes into process-wide registries that nothing else resets,
    // so snapshot them and put the originals back afterwards.
    savedHandlerList = new ArrayList<>(Parameter.handlerList);
    savedHandlerMap = new HashMap<>(Parameter.handlerMap);

    service = new P2pService();
    service.start(config);
  }

  @After
  public void tearDown() {
    service.close();
    ChannelManager.isShutdown = false;
    Parameter.handlerList = savedHandlerList;
    Parameter.handlerMap = savedHandlerMap;
    Parameter.p2pConfig = saved;
  }

  @Test
  public void startPublishesTheConfig() {
    Assert.assertEquals("127.0.0.1", Parameter.p2pConfig.getIp());
    Assert.assertEquals(Parameter.version, service.getVersion());
  }

  @Test
  public void nodeListsAreQueryableAndNeverNull() {
    List<Node> table = service.getTableNodes();
    List<Node> connectable = service.getConnectableNodes();
    List<Node> all = service.getAllNodes();

    Assert.assertNotNull(table);
    Assert.assertNotNull(connectable);
    Assert.assertNotNull(all);
    // getAllNodes unions the discovery table with the DNS nodes, so it can never
    // be smaller than the table alone.
    Assert.assertTrue(all.size() >= table.size());
  }

  @Test
  public void statsAreExposed() {
    P2pStats stats = service.getP2pStats();
    Assert.assertNotNull(stats);
    Assert.assertTrue(stats.getTcpInPackets() >= 0);
    Assert.assertTrue(stats.getUdpOutSize() >= 0);
  }

  @Test
  public void registeringTheSameMessageTypeTwiceIsRejected() throws Exception {
    P2pEventHandler first = new P2pEventHandler() {
      @Override
      public void onMessage(Channel channel, byte[] data) {
      }
    };
    first.messageTypes = new java.util.HashSet<>(java.util.Collections.singletonList((byte) 0x7A));
    service.register(first);

    P2pEventHandler clash = new P2pEventHandler() {
      @Override
      public void onMessage(Channel channel, byte[] data) {
      }
    };
    clash.messageTypes = new java.util.HashSet<>(java.util.Collections.singletonList((byte) 0x7A));
    try {
      service.register(clash);
      Assert.fail("expected a P2pException for the duplicate type");
    } catch (P2pException expected) {
      Assert.assertEquals(P2pException.TypeEnum.TYPE_ALREADY_REGISTERED, expected.getType());
    }
  }

  @Test
  public void closeIsIdempotent() {
    service.close();
    // The second call must return through the isShutdown guard rather than
    // tearing the managers down twice.
    service.close();
  }
}
