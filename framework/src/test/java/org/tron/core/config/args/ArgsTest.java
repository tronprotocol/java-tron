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
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.args.GenesisBlock;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;

@Slf4j
public class ArgsTest {

  private final String privateKey = PublicMethod.getRandomPrivateKey();
  private String address;
  private LocalWitnesses localWitnesses;

  @After
  public void destroy() {
    Args.clearParam();
  }

  @Test
  public void get() {
    Args.setParam(new String[]{"-w"}, Constant.TEST_CONF);

    CommonParameter parameter = Args.getInstance();

    Args.logConfig();

    localWitnesses = new LocalWitnesses();
    localWitnesses.setPrivateKeys(Arrays.asList(privateKey));
    localWitnesses.initWitnessAccountAddress(true);
    Args.setLocalWitnesses(localWitnesses);
    address = ByteArray.toHexString(Args.getLocalWitnesses()
            .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine()));

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
    Assert.assertEquals("127.0.0.1", parameter.getNodeDiscoveryBindIp());
    Assert.assertEquals("46.168.1.1", parameter.getNodeExternalIp());
    Assert.assertEquals(18888, parameter.getNodeListenPort());
    Assert.assertEquals(2000, parameter.getNodeConnectionTimeout());
    Assert.assertEquals(0, parameter.getActiveNodes().size());
    Assert.assertEquals(30, parameter.getMaxConnections());
    Assert.assertEquals(43, parameter.getNodeP2pVersion());
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

    Assert.assertEquals(privateKey,
        Args.getLocalWitnesses().getPrivateKey());

    Assert.assertEquals(address,
        ByteArray.toHexString(Args.getLocalWitnesses()
            .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine())));
  }
}

