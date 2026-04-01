package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class NodeConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    NodeConfig nc = NodeConfig.fromConfig(empty);
    assertEquals(18888, nc.getListenPort());
    assertEquals(2, nc.getConnectionTimeout());
    assertEquals(200, nc.getFetchBlockTimeout());
    assertEquals(30, nc.getMaxConnections());
    assertEquals(8, nc.getMinConnections());
    assertEquals(4, nc.getMaxFastForwardNum());
    assertFalse(nc.isOpenFullTcpDisconnect());
    // reference.conf has node.discovery.enable=true, persist=true
    assertTrue(nc.isDiscoveryEnable());
    assertTrue(nc.isDiscoveryPersist());
    assertEquals(0, nc.getChannelReadTimeout());
  }

  @Test
  public void testDotNotationFields() {
    Config config = withRef(
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
    Config config = withRef(
        "node.discovery { enable = true, persist = true }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertTrue(nc.isDiscoveryEnable());
    assertTrue(nc.isDiscoveryPersist());
  }

  @Test
  public void testHttpSubBean() {
    Config config = withRef(
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
    Config config = withRef(
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
    Config config = withRef(
        "node { backup { priority = 5, port = 20001, keepAliveInterval = 5000 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertEquals(5, nc.getBackup().getPriority());
    assertEquals(20001, nc.getBackup().getPort());
    assertEquals(5000, nc.getBackup().getKeepAliveInterval());
  }

  @Test
  public void testIsOpenFullTcpDisconnect() {
    Config config = withRef(
        "node { isOpenFullTcpDisconnect = true }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertTrue(nc.isOpenFullTcpDisconnect());
  }

  @Test
  public void testRpcDefaultsFromReference() {
    Config empty = withRef();
    NodeConfig nc = NodeConfig.fromConfig(empty);
    NodeConfig.RpcConfig rpc = nc.getRpc();

    // reference.conf provides actual final defaults, no sentinel conversion needed
    assertEquals(2147483647, rpc.getMaxConcurrentCallsPerConnection());
    assertEquals(1048576, rpc.getFlowControlWindow());
    assertEquals(9223372036854775807L, rpc.getMaxConnectionIdleInMillis());
    assertEquals(9223372036854775807L, rpc.getMaxConnectionAgeInMillis());
    assertEquals(4194304, rpc.getMaxMessageSize());
    assertEquals(8192, rpc.getMaxHeaderListSize());
    assertEquals(1, rpc.getMinEffectiveConnection());
    // thread=0 in reference.conf triggers auto-detect in postProcess
    assertTrue(rpc.getThread() > 0);
  }

  @Test
  public void testRpcUserOverrideZeroNotConverted() {
    // Users can explicitly set 0 to disable connection checks (e.g. system-test)
    Config config = withRef(
        "node { rpc { minEffectiveConnection = 0 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertEquals(0, nc.getRpc().getMinEffectiveConnection());
  }

  @Test
  public void testRpcUserOverrideExplicitValues() {
    Config config = withRef(
        "node { rpc { thread = 32,"
            + " maxConcurrentCallsPerConnection = 50,"
            + " flowControlWindow = 2097152,"
            + " maxMessageSize = 8388608,"
            + " maxHeaderListSize = 16384 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    NodeConfig.RpcConfig rpc = nc.getRpc();
    assertEquals(32, rpc.getThread());
    assertEquals(50, rpc.getMaxConcurrentCallsPerConnection());
    assertEquals(2097152, rpc.getFlowControlWindow());
    assertEquals(8388608, rpc.getMaxMessageSize());
    assertEquals(16384, rpc.getMaxHeaderListSize());
  }
}
