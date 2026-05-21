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
    assertEquals(500, nc.getFetchBlockTimeout());
    assertEquals(30, nc.getMaxConnections());
    assertEquals(8, nc.getMinConnections());
    assertEquals(4, nc.getMaxFastForwardNum());
    assertFalse(nc.isOpenFullTcpDisconnect());
    // reference.conf matches code default: discovery disabled when not configured
    assertFalse(nc.isDiscoveryEnable());
    assertFalse(nc.isDiscoveryPersist());
  }

  @Test
  public void testDotNotationFields() {
    Config config = withRef(
        "node { listen { port = 19999 }, connection { timeout = 5 },"
            + " fetchBlock { timeout = 300 }, solidity { threads = 4 } }");
    NodeConfig nc = NodeConfig.fromConfig(config);
    assertEquals(19999, nc.getListenPort());
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

  // ===========================================================================
  // Boundary tests for postProcess() clamps
  // Pin every clamp in NodeConfig.postProcess() so future refactors cannot
  // drop them undetected (regression seen in PR #6615 with CommitteeConfig).
  // ===========================================================================

  // ----- blockProducedTimeOut: clamped to [30, 100] -----

  @Test
  public void testBlockProducedTimeOutClampedBelowMin() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { blockProducedTimeOut = 10 }"));
    assertEquals(30, nc.getBlockProducedTimeOut());
  }

  @Test
  public void testBlockProducedTimeOutClampedAboveMax() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { blockProducedTimeOut = 200 }"));
    assertEquals(100, nc.getBlockProducedTimeOut());
  }

  @Test
  public void testBlockProducedTimeOutBoundaryValues() {
    assertEquals(30, NodeConfig.fromConfig(
        withRef("node { blockProducedTimeOut = 30 }")).getBlockProducedTimeOut());
    assertEquals(100, NodeConfig.fromConfig(
        withRef("node { blockProducedTimeOut = 100 }")).getBlockProducedTimeOut());
    assertEquals(75, NodeConfig.fromConfig(
        withRef("node { blockProducedTimeOut = 75 }")).getBlockProducedTimeOut());
  }

  // ----- inactiveThreshold: minimum 1 -----

  @Test
  public void testInactiveThresholdClampedBelowMin() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { inactiveThreshold = 0 }"));
    assertEquals(1, nc.getInactiveThreshold());
  }

  @Test
  public void testInactiveThresholdClampedNegative() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { inactiveThreshold = -100 }"));
    assertEquals(1, nc.getInactiveThreshold());
  }

  @Test
  public void testInactiveThresholdInRangeUnchanged() {
    assertEquals(1, NodeConfig.fromConfig(
        withRef("node { inactiveThreshold = 1 }")).getInactiveThreshold());
    assertEquals(600, NodeConfig.fromConfig(
        withRef("node { inactiveThreshold = 600 }")).getInactiveThreshold());
    assertEquals(1000, NodeConfig.fromConfig(
        withRef("node { inactiveThreshold = 1000 }")).getInactiveThreshold());
  }

  // ----- maxFastForwardNum: clamped to [1, MAX_ACTIVE_WITNESS_NUM=27] -----

  @Test
  public void testMaxFastForwardNumClampedBelowMin() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { maxFastForwardNum = 0 }"));
    assertEquals(1, nc.getMaxFastForwardNum());
  }

  @Test
  public void testMaxFastForwardNumClampedAboveMax() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { maxFastForwardNum = 100 }"));
    assertEquals(27, nc.getMaxFastForwardNum());
  }

  @Test
  public void testMaxFastForwardNumBoundaryValues() {
    assertEquals(1, NodeConfig.fromConfig(
        withRef("node { maxFastForwardNum = 1 }")).getMaxFastForwardNum());
    assertEquals(27, NodeConfig.fromConfig(
        withRef("node { maxFastForwardNum = 27 }")).getMaxFastForwardNum());
    assertEquals(4, NodeConfig.fromConfig(
        withRef("node { maxFastForwardNum = 4 }")).getMaxFastForwardNum());
  }

  // ----- validContractProto.threads: 0 = auto (availableProcessors) -----

  @Test
  public void testValidContractProtoThreadsDefaultAutoExpands() {
    // Default in reference.conf is 0; postProcess must expand to availableProcessors.
    // Matches develop Args.java:743-746 runtime fallback.
    NodeConfig nc = NodeConfig.fromConfig(withRef());
    assertEquals(Runtime.getRuntime().availableProcessors(),
        nc.getValidContractProtoThreads());
  }

  @Test
  public void testValidContractProtoThreadsExplicitPreserved() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { validContractProto { threads = 3 } }"));
    assertEquals(3, nc.getValidContractProtoThreads());
  }

  // ----- trustNode: empty reference.conf default means trustNode stays unset -----

  @Test
  public void testTrustNodeNotDefaultedByReferenceConf() {
    // reference.conf intentionally omits `node.trustNode` so that empty configs
    // preserve develop's behavior (trustNodeAddr stays null in the Args bridge).
    NodeConfig nc = NodeConfig.fromConfig(withRef());
    assertTrue(nc.getTrustNode() == null || nc.getTrustNode().isEmpty());
  }

  // ----- maxConnectionsWithSameIp alias: reference.conf must not poison merge -----

  @Test
  public void testMaxConnectionsWithSameIpNotOverriddenByReferenceConfAlias() {
    // reference.conf must NOT ship `maxActiveNodesWithSameIp`, otherwise the alias-
    // fallback branch would silently clobber the user's modern key. Regression guard
    // for review #2 (317787106, 2026-04-16).
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { maxConnectionsWithSameIp = 10 }"));
    assertEquals(10, nc.getMaxConnectionsWithSameIp());
  }

  @Test
  public void testMaxActiveNodesWithSameIpLegacyAliasStillWorks() {
    // Back-compat: users who still write the legacy key in their config.conf
    // must get their value routed to maxConnectionsWithSameIp.
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { maxActiveNodesWithSameIp = 5 }"));
    assertEquals(5, nc.getMaxConnectionsWithSameIp());
  }

  @Test
  public void testLegacyAliasTakesPriorityOverModernKey() {
    // Matches develop Args.java:392-399: if the legacy key is present, it wins.
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node { maxActiveNodesWithSameIp = 5, maxConnectionsWithSameIp = 10 }"));
    assertEquals(5, nc.getMaxConnectionsWithSameIp());
  }

  @Test
  public void testShieldedApiDefaultsToFalseWhenNeitherKeySet() {
    NodeConfig nc = NodeConfig.fromConfig(withRef());
    assertFalse(nc.isAllowShieldedTransactionApi());
  }

  @Test
  public void testShieldedApiModernKeyRespected() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node.allowShieldedTransactionApi = true"));
    assertTrue(nc.isAllowShieldedTransactionApi());
  }

  @Test
  public void testShieldedApiLegacyKeyRespected() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node.fullNodeAllowShieldedTransaction = true"));
    assertTrue(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef("node.fullNodeAllowShieldedTransaction = false"));
    assertFalse(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef("node.allowShieldedTransactionApi = true"));
    assertTrue(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef("node.allowShieldedTransactionApi = false"));
    assertFalse(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef(""));
    assertFalse(nc.isAllowShieldedTransactionApi());
  }

  @Test
  public void testShieldedApiModernKeyTakesPriorityOverLegacy() {
    // When both keys are set, the modern key wins; the legacy key is only used as fallback
    // when modern is absent.
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node {\n"
            + "  allowShieldedTransactionApi = true\n"
            + "  fullNodeAllowShieldedTransaction = true\n"
            + "}"));
    assertTrue(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef("node {\n"
            + "  allowShieldedTransactionApi = true\n"
            + "  fullNodeAllowShieldedTransaction = false\n"
            + "}"));
    assertTrue(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef("node {\n"
            + "  allowShieldedTransactionApi = false\n"
            + "  fullNodeAllowShieldedTransaction = true\n"
            + "}"));
    assertFalse(nc.isAllowShieldedTransactionApi());
    nc = NodeConfig.fromConfig(
        withRef("node {\n"
            + "  allowShieldedTransactionApi = false\n"
            + "  fullNodeAllowShieldedTransaction = false\n"
            + "}"));
    assertFalse(nc.isAllowShieldedTransactionApi());
  }

  // ----- discovery.external.ip: null / "null" sentinel handling -----

  @Test
  public void testExternalIpAbsentDefaultsToEmpty() {
    NodeConfig nc = NodeConfig.fromConfig(withRef());
    assertEquals("", nc.getDiscoveryExternalIp());
  }

  @Test
  public void testExternalIpHoconNullTreatedAsEmpty() {
    // HOCON `null` makes hasPath() return false; getString falls back to "".
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node.discovery.external.ip = null"));
    assertEquals("", nc.getDiscoveryExternalIp());
  }

  @Test
  public void testExternalIpStringNullSentinelConvertedToEmpty() {
    // String literal "null" (case-insensitive) is an explicit sentinel that must map to "".
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node.discovery.external.ip = \"null\""));
    assertEquals("", nc.getDiscoveryExternalIp());

    nc = NodeConfig.fromConfig(
        withRef("node.discovery.external.ip = \"NULL\""));
    assertEquals("", nc.getDiscoveryExternalIp());
  }

  @Test
  public void testExternalIpValidValuePreserved() {
    NodeConfig nc = NodeConfig.fromConfig(
        withRef("node.discovery.external.ip = \"1.2.3.4\""));
    assertEquals("1.2.3.4", nc.getDiscoveryExternalIp());
  }


}
