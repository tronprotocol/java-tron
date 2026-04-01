package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class NodeConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
    NodeConfig nc = NodeConfig.fromConfig(empty);
    assertEquals(18888, nc.getListenPort());
    assertEquals(2, nc.getConnectionTimeout());
    assertEquals(200, nc.getFetchBlockTimeout());
    assertEquals(30, nc.getMaxConnections());
    assertEquals(8, nc.getMinConnections());
    assertEquals(4, nc.getMaxFastForwardNum());
    assertFalse(nc.isOpenFullTcpDisconnect());
    assertFalse(nc.isDiscoveryEnable());
    assertFalse(nc.isDiscoveryPersist());
    assertEquals(0, nc.getChannelReadTimeout());
  }

  @Test
  public void testDotNotationFields() {
    Config config = ConfigFactory.parseString(
        "node { listen { port = 19999 }, connection { timeout = 5 },"
            + " fetchBlock { timeout = 300 }, solidity { threads = 4 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertEquals(19999, nc.getListenPort());
    assertEquals(5, nc.getConnectionTimeout());
    assertEquals(300, nc.getFetchBlockTimeout());
    assertEquals(4, nc.getSolidityThreads());
  }

  @Test
  public void testDiscoveryFields() {
    Config config = ConfigFactory.parseString(
        "node.discovery { enable = true, persist = true }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertTrue(nc.isDiscoveryEnable());
    assertTrue(nc.isDiscoveryPersist());
  }

  @Test
  public void testHttpSubBean() {
    Config config = ConfigFactory.parseString(
        "node { http { fullNodeEnable = false, fullNodePort = 9090,"
            + " PBFTEnable = false, PBFTPort = 9092 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertFalse(nc.getHttp().isFullNodeEnable());
    assertEquals(9090, nc.getHttp().getFullNodePort());
    assertFalse(nc.getHttp().isPBFTEnable());
    assertEquals(9092, nc.getHttp().getPBFTPort());
  }

  @Test
  public void testRpcSubBean() {
    Config config = ConfigFactory.parseString(
        "node { rpc { enable = false, port = 60051,"
            + " PBFTEnable = false, PBFTPort = 60071 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertFalse(nc.getRpc().isEnable());
    assertEquals(60051, nc.getRpc().getPort());
    assertFalse(nc.getRpc().isPBFTEnable());
    assertEquals(60071, nc.getRpc().getPBFTPort());
  }

  @Test
  public void testBackupSubBean() {
    Config config = ConfigFactory.parseString(
        "node { backup { priority = 5, port = 20001, keepAliveInterval = 5000 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertEquals(5, nc.getBackup().getPriority());
    assertEquals(20001, nc.getBackup().getPort());
    assertEquals(5000, nc.getBackup().getKeepAliveInterval());
  }

  @Test
  public void testIsOpenFullTcpDisconnect() {
    Config config = ConfigFactory.parseString(
        "node { isOpenFullTcpDisconnect = true }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertTrue(nc.isOpenFullTcpDisconnect());
  }
}
