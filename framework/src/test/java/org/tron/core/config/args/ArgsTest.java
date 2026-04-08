/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.config.args;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.tron.common.TestConstants;
import org.tron.common.args.GenesisBlock;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.Configuration;

@Slf4j
public class ArgsTest {

  private final String privateKey = PublicMethod.getRandomPrivateKey();
  private String address;
  private LocalWitnesses localWitnesses;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void destroy() {
    Args.clearParam();
  }

  @Test
  public void get() {
    Args.setParam(new String[] {"-c", TestConstants.TEST_CONF, "--keystore-factory"},
        TestConstants.NET_CONF);

    CommonParameter parameter = Args.getInstance();

    Args.logConfig();

    localWitnesses = new LocalWitnesses();
    localWitnesses.setPrivateKeys(Arrays.asList(privateKey));
    localWitnesses.initWitnessAccountAddress(null, true);
    Args.setLocalWitnesses(localWitnesses);
    address = ByteArray.toHexString(Args.getLocalWitnesses()
        .getWitnessAccountAddress());
    Assert.assertEquals("41", DecodeUtil.addressPreFixString);
    Assert.assertEquals(TestConstants.TEST_CONF, Args.getConfigFilePath());
    Assert.assertEquals(0, parameter.getBackupPriority());

    Assert.assertEquals(3000, parameter.getKeepAliveInterval());

    Assert.assertEquals(10001, parameter.getBackupPort());

    Assert.assertEquals("database", parameter.getStorage().getDbDirectory());

    Assert.assertEquals(11, parameter.getSeedNode().getAddressList().size());

    GenesisBlock genesisBlock = parameter.getGenesisBlock();

    Assert.assertEquals(4, genesisBlock.getAssets().size());

    Assert.assertEquals(11, genesisBlock.getWitnesses().size());

    Assert.assertEquals("0", genesisBlock.getTimestamp());

    Assert.assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000",
        genesisBlock.getParentHash());

    Assert.assertEquals(
        Lists.newArrayList(privateKey),
        Args.getLocalWitnesses().getPrivateKeys());

    Assert.assertTrue(parameter.isNodeDiscoveryEnable());
    Assert.assertTrue(parameter.isNodeDiscoveryPersist());
    Assert.assertEquals("46.168.1.1", parameter.getNodeExternalIp());
    Assert.assertEquals(18888, parameter.getNodeListenPort());
    Assert.assertEquals(2000, parameter.getNodeConnectionTimeout());
    Assert.assertEquals(0, parameter.getActiveNodes().size());
    Assert.assertEquals(30, parameter.getMaxConnections());
    Assert.assertEquals(43, parameter.getNodeP2pVersion());
    Assert.assertEquals(54, parameter.getMaxUnsolidifiedBlocks());
    Assert.assertEquals(false, parameter.isUnsolidifiedBlockCheck());
    Assert.assertEquals(1000, parameter.getMaxCreateAccountTxSize());
    //Assert.assertEquals(30, args.getSyncNodeCount());

    // gRPC network configs checking
    Assert.assertEquals(50051, parameter.getRpcPort());
    Assert.assertEquals(Integer.MAX_VALUE, parameter.getMaxConcurrentCallsPerConnection());
    Assert
        .assertEquals(NettyServerBuilder
            .DEFAULT_FLOW_CONTROL_WINDOW, parameter.getFlowControlWindow());
    Assert.assertEquals(60000L, parameter.getMaxConnectionIdleInMillis());
    Assert.assertEquals(Long.MAX_VALUE, parameter.getMaxConnectionAgeInMillis());
    Assert.assertEquals(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE, parameter.getMaxMessageSize());
    Assert.assertEquals(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE, parameter.getMaxHeaderListSize());
    Assert.assertEquals(1L, parameter.getAllowCreationOfContracts());
    Assert.assertEquals(0, parameter.getConsensusLogicOptimization());

    Assert.assertEquals(privateKey,
        Args.getLocalWitnesses().getPrivateKey());

    Assert.assertEquals(address,
        ByteArray.toHexString(Args.getLocalWitnesses()
            .getWitnessAccountAddress()));

    Assert.assertTrue(parameter.isKeystoreFactory());
  }

  @Test
  public void testIpFromLibP2p()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    CommonParameter parameter = Args.getInstance();
    Assert.assertEquals(TestConstants.TEST_CONF, Args.getConfigFilePath());

    String configuredExternalIp = parameter.getNodeExternalIp();
    Assert.assertEquals("46.168.1.1", configuredExternalIp);

    CommonParameter.getInstance().setNodeExternalIp(null);

    NodeConfig nc = new NodeConfig();
    Method method2 = Args.class.getDeclaredMethod("externalIp", NodeConfig.class);
    method2.setAccessible(true);
    method2.invoke(Args.class, nc);

    Assert.assertNotEquals(configuredExternalIp, parameter.getNodeExternalIp());
  }

  @Test
  public void testOldRewardOpt() {
    thrown.expect(IllegalArgumentException.class);
    Args.setParam(new String[] {"-c", "args-test.conf"}, TestConstants.NET_CONF);
  }

  @Test
  public void testInitService() {
    Map<String,String> storage = new HashMap<>();
    // avoid the exception for the missing storage
    storage.put("storage.db.directory", "database");
    Config config = ConfigFactory.defaultOverrides()
        .withFallback(ConfigFactory.parseMap(storage))
        .withFallback(ConfigFactory.defaultReference());
    // test default value
    Args.applyConfigParams(config);
    Assert.assertTrue(Args.getInstance().isRpcEnable());
    Assert.assertTrue(Args.getInstance().isRpcSolidityEnable());
    Assert.assertTrue(Args.getInstance().isRpcPBFTEnable());
    Assert.assertTrue(Args.getInstance().isFullNodeHttpEnable());
    Assert.assertTrue(Args.getInstance().isSolidityNodeHttpEnable());
    Assert.assertTrue(Args.getInstance().isPBFTHttpEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpFullNodeEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpSolidityNodeEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpPBFTNodeEnable());
    Assert.assertEquals(5000, Args.getInstance().getJsonRpcMaxBlockRange());
    Assert.assertEquals(1000, Args.getInstance().getJsonRpcMaxSubTopics());
    Args.clearParam();
    // test set all true value
    storage.put("node.rpc.enable", "true");
    storage.put("node.rpc.solidityEnable", "true");
    storage.put("node.rpc.PBFTEnable", "true");
    storage.put("node.http.fullNodeEnable", "true");
    storage.put("node.http.solidityEnable", "true");
    storage.put("node.http.PBFTEnable", "true");
    storage.put("node.jsonrpc.httpFullNodeEnable", "true");
    storage.put("node.jsonrpc.httpSolidityEnable", "true");
    storage.put("node.jsonrpc.httpPBFTEnable", "true");
    storage.put("node.jsonrpc.maxBlockRange", "10");
    storage.put("node.jsonrpc.maxSubTopics", "20");
    config = ConfigFactory.defaultOverrides()
        .withFallback(ConfigFactory.parseMap(storage))
        .withFallback(ConfigFactory.defaultReference());
    // test value
    Args.applyConfigParams(config);
    Assert.assertTrue(Args.getInstance().isRpcEnable());
    Assert.assertTrue(Args.getInstance().isRpcSolidityEnable());
    Assert.assertTrue(Args.getInstance().isRpcPBFTEnable());
    Assert.assertTrue(Args.getInstance().isFullNodeHttpEnable());
    Assert.assertTrue(Args.getInstance().isSolidityNodeHttpEnable());
    Assert.assertTrue(Args.getInstance().isPBFTHttpEnable());
    Assert.assertTrue(Args.getInstance().isJsonRpcHttpFullNodeEnable());
    Assert.assertTrue(Args.getInstance().isJsonRpcHttpSolidityNodeEnable());
    Assert.assertTrue(Args.getInstance().isJsonRpcHttpPBFTNodeEnable());
    Assert.assertEquals(10, Args.getInstance().getJsonRpcMaxBlockRange());
    Assert.assertEquals(20, Args.getInstance().getJsonRpcMaxSubTopics());
    Args.clearParam();
    // test set all false value
    storage.put("node.rpc.enable", "false");
    storage.put("node.rpc.solidityEnable", "false");
    storage.put("node.rpc.PBFTEnable", "false");
    storage.put("node.http.fullNodeEnable", "false");
    storage.put("node.http.solidityEnable", "false");
    storage.put("node.http.PBFTEnable", "false");
    storage.put("node.jsonrpc.httpFullNodeEnable", "false");
    storage.put("node.jsonrpc.httpSolidityEnable", "false");
    storage.put("node.jsonrpc.httpPBFTEnable", "false");
    storage.put("node.jsonrpc.maxBlockRange", "5000");
    storage.put("node.jsonrpc.maxSubTopics", "1000");
    config = ConfigFactory.defaultOverrides()
        .withFallback(ConfigFactory.parseMap(storage))
        .withFallback(ConfigFactory.defaultReference());
    // test value
    Args.applyConfigParams(config);
    Assert.assertFalse(Args.getInstance().isRpcEnable());
    Assert.assertFalse(Args.getInstance().isRpcSolidityEnable());
    Assert.assertFalse(Args.getInstance().isRpcPBFTEnable());
    Assert.assertFalse(Args.getInstance().isFullNodeHttpEnable());
    Assert.assertFalse(Args.getInstance().isSolidityNodeHttpEnable());
    Assert.assertFalse(Args.getInstance().isPBFTHttpEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpFullNodeEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpSolidityNodeEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpPBFTNodeEnable());
    Assert.assertEquals(5000, Args.getInstance().getJsonRpcMaxBlockRange());
    Assert.assertEquals(1000, Args.getInstance().getJsonRpcMaxSubTopics());
    Args.clearParam();
    // test set random value
    storage.put("node.rpc.enable", "false");
    storage.put("node.rpc.solidityEnable", "false");
    storage.put("node.rpc.PBFTEnable", "true");
    storage.put("node.http.fullNodeEnable", "false");
    storage.put("node.http.solidityEnable", "true");
    storage.put("node.http.PBFTEnable", "false");
    storage.put("node.jsonrpc.httpFullNodeEnable", "true");
    storage.put("node.jsonrpc.httpSolidityEnable", "false");
    storage.put("node.jsonrpc.httpPBFTEnable", "true");
    storage.put("node.jsonrpc.maxBlockRange", "30");
    storage.put("node.jsonrpc.maxSubTopics", "40");
    config = ConfigFactory.defaultOverrides()
        .withFallback(ConfigFactory.parseMap(storage))
        .withFallback(ConfigFactory.defaultReference());
    // test value
    Args.applyConfigParams(config);
    Assert.assertFalse(Args.getInstance().isRpcEnable());
    Assert.assertFalse(Args.getInstance().isRpcSolidityEnable());
    Assert.assertTrue(Args.getInstance().isRpcPBFTEnable());
    Assert.assertFalse(Args.getInstance().isFullNodeHttpEnable());
    Assert.assertTrue(Args.getInstance().isSolidityNodeHttpEnable());
    Assert.assertFalse(Args.getInstance().isPBFTHttpEnable());
    Assert.assertTrue(Args.getInstance().isJsonRpcHttpFullNodeEnable());
    Assert.assertFalse(Args.getInstance().isJsonRpcHttpSolidityNodeEnable());
    Assert.assertTrue(Args.getInstance().isJsonRpcHttpPBFTNodeEnable());
    Assert.assertEquals(30, Args.getInstance().getJsonRpcMaxBlockRange());
    Assert.assertEquals(40, Args.getInstance().getJsonRpcMaxSubTopics());

    // test set invalid value
    storage.put("node.jsonrpc.maxBlockRange", "0");
    storage.put("node.jsonrpc.maxSubTopics", "0");
    config = ConfigFactory.defaultOverrides()
        .withFallback(ConfigFactory.parseMap(storage))
        .withFallback(ConfigFactory.defaultReference());
    // check value
    Args.applyConfigParams(config);
    Assert.assertEquals(0, Args.getInstance().getJsonRpcMaxBlockRange());
    Assert.assertEquals(0, Args.getInstance().getJsonRpcMaxSubTopics());

    // test set invalid value
    storage.put("node.jsonrpc.maxBlockRange", "-2");
    storage.put("node.jsonrpc.maxSubTopics", "-4");
    config = ConfigFactory.defaultOverrides()
        .withFallback(ConfigFactory.parseMap(storage))
        .withFallback(ConfigFactory.defaultReference());
    // check value
    Args.applyConfigParams(config);
    Assert.assertEquals(-2, Args.getInstance().getJsonRpcMaxBlockRange());
    Assert.assertEquals(-4, Args.getInstance().getJsonRpcMaxSubTopics());

    Args.clearParam();
  }

  /**
   * Verify that CLI storage parameters correctly override config file values.
   *
   * <p>config-test.conf defines: db.directory = "database", db.engine = "LEVELDB".
   * When CLI passes different values (e.g. --storage-db-directory cli-db-dir),
   * the Storage object should reflect the CLI values, not the config file values.
   *
   * <p>This ensures the three-layer override chain works:
   * Storage defaults -> config file -> CLI arguments.
   */
  @Test
  public void testCliOverridesStorageConfig() {
    Args.setParam(new String[] {
        "--storage-db-directory", "cli-db-dir",
        "--storage-db-engine", "ROCKSDB",
        "--storage-db-synchronous", "true",
        "--storage-index-directory", "cli-index-dir",
        "--storage-index-switch", "cli-index-switch",
        "--storage-transactionHistory-switch", "off",
        "--contract-parse-enable", "false"
    }, TestConstants.TEST_CONF);

    CommonParameter parameter = Args.getInstance();

    Assert.assertEquals("cli-db-dir", parameter.getStorage().getDbDirectory());
    Assert.assertEquals("ROCKSDB", parameter.getStorage().getDbEngine());
    Assert.assertTrue(parameter.getStorage().isDbSync());
    Assert.assertEquals("cli-index-dir", parameter.getStorage().getIndexDirectory());
    Assert.assertEquals("cli-index-switch", parameter.getStorage().getIndexSwitch());
    Assert.assertEquals("off", parameter.getStorage().getTransactionHistorySwitch());
    Assert.assertFalse(parameter.getStorage().isContractParseSwitch());

    Args.clearParam();
  }

  /**
   * Verify that event.subscribe.enable = false from config is read correctly.
   */
  @Test
  public void testEventSubscribeFromConfig() {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    Assert.assertFalse(Args.getInstance().isEventSubscribe());
    Args.clearParam();
  }

  /**
   * Verify that CLI --es overrides event.subscribe.enable from config.
   * config-test.conf defines: event.subscribe.enable = false,
   * passing --es explicitly sets eventSubscribe = true, overriding config.
   */
  @Test
  public void testCliEsOverridesConfig() {
    Args.setParam(new String[] {"--es"}, TestConstants.TEST_CONF);
    Assert.assertTrue(Args.getInstance().isEventSubscribe());
    Args.clearParam();
  }

  /**
   * Verify that config file storage values are applied when no CLI override is present.
   *
   * <p>config-test.conf defines: db.directory = "database", db.engine = "LEVELDB".
   * On ARM64 CI, Gradle injects -Dstorage.db.engine=ROCKSDB via system property,
   * so the engine will be ROCKSDB instead of LEVELDB.
   */
  @Test
  public void testConfigStorageDefaults() {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);

    CommonParameter parameter = Args.getInstance();

    Assert.assertEquals("database", parameter.getStorage().getDbDirectory());
    String expectedEngine = System.getProperty("storage.db.engine") != null
        ? System.getProperty("storage.db.engine") : "LEVELDB";
    Assert.assertEquals(expectedEngine, parameter.getStorage().getDbEngine());

    Args.clearParam();
  }

  // ===========================================================================
  // Boundary tests for clamps applied in Args.java bridge code (not in
  // bean postProcess()).
  //
  // fetchBlockTimeout is read from NodeConfig but clamped in Args.applyNodeConfig
  // to range [100, 1000]. Pin this clamp here so any future refactor that moves
  // it (e.g. into NodeConfig.postProcess()) preserves the behavior.
  // ===========================================================================

  @Test
  public void testFetchBlockTimeoutClampedBelowMin() {
    Map<String, String> override = new HashMap<>();
    override.put("storage.db.directory", "database");
    override.put("node.fetchBlock.timeout", "50");
    Config config = ConfigFactory.parseMap(override)
        .withFallback(ConfigFactory.defaultReference());
    Args.applyConfigParams(config);
    Assert.assertEquals(100, Args.getInstance().getFetchBlockTimeout());
    Args.clearParam();
  }

  @Test
  public void testFetchBlockTimeoutClampedAboveMax() {
    Map<String, String> override = new HashMap<>();
    override.put("storage.db.directory", "database");
    override.put("node.fetchBlock.timeout", "2000");
    Config config = ConfigFactory.parseMap(override)
        .withFallback(ConfigFactory.defaultReference());
    Args.applyConfigParams(config);
    Assert.assertEquals(1000, Args.getInstance().getFetchBlockTimeout());
    Args.clearParam();
  }

  @Test
  public void testFetchBlockTimeoutInRangeUnchanged() {
    Map<String, String> override = new HashMap<>();
    override.put("storage.db.directory", "database");
    override.put("node.fetchBlock.timeout", "500");
    Config config = ConfigFactory.parseMap(override)
        .withFallback(ConfigFactory.defaultReference());
    Args.applyConfigParams(config);
    Assert.assertEquals(500, Args.getInstance().getFetchBlockTimeout());
    Args.clearParam();
  }
}

