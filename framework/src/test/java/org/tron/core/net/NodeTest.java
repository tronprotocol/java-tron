package org.tron.core.net;

import static org.tron.core.net.message.handshake.HelloMessage.getEndpointFromNode;

import com.typesafe.config.Config;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.p2p.discover.Node;
import org.tron.p2p.dns.update.DnsType;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Discover.Endpoint;

@Slf4j
public class NodeTest {

  @Test
  public void testIpV4() {
    InetSocketAddress address1 = NetUtil.parseInetSocketAddress("192.168.0.1:18888");
    Assert.assertNotNull(address1);
    try {
      NetUtil.parseInetSocketAddress("192.168.0.1");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testIpV6() {
    try {
      NetUtil.parseInetSocketAddress("fe80::216:3eff:fe0e:23bb:18888");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }
    InetSocketAddress address2 = NetUtil.parseInetSocketAddress("[fe80::216:3eff:fe0e:23bb]:18888");
    Assert.assertNotNull(address2);
    try {
      NetUtil.parseInetSocketAddress("fe80::216:3eff:fe0e:23bb");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testIpStack() {
    Set<String> ipSet = new HashSet<>(Collections.singletonList("192.168.0.1"));
    Assert.assertTrue(TronNetService.hasIpv4Stack(ipSet));
    ipSet = new HashSet<>(Collections.singletonList("127.0.0.1"));
    Assert.assertTrue(TronNetService.hasIpv4Stack(ipSet));
    ipSet = new HashSet<>(Collections.singletonList("fe80:0:0:0:0:0:0:1"));
    Assert.assertFalse(TronNetService.hasIpv4Stack(ipSet));
    ipSet = new HashSet<>(Arrays.asList("127.0.0.1", "fe80:0:0:0:0:0:0:1"));
    Assert.assertTrue(TronNetService.hasIpv4Stack(ipSet));
    ipSet = new HashSet<>(Collections.emptyList());
    Assert.assertFalse(TronNetService.hasIpv4Stack(ipSet));
  }

  @Test
  public void testEndpointFromNode() {
    Node node = new Node(null, null, null, 18888);
    Endpoint endpoint = getEndpointFromNode(node);
    Assert.assertTrue(endpoint.getNodeId().isEmpty());
    Assert.assertTrue(endpoint.getAddress().isEmpty());
    Assert.assertTrue(endpoint.getAddressIpv6().isEmpty());
  }

  @Test
  public void testPublishConfig() {
    Config config = Configuration.getByFileName(Constant.TEST_CONF, Constant.TEST_CONF);

    PublishConfig publishConfig = new PublishConfig();
    Assert.assertFalse(publishConfig.isDnsPublishEnable());

    publishConfig.setDnsPublishEnable(true);
    Assert.assertTrue(publishConfig.isDnsPublishEnable());
    Args.loadDnsPublishParameters(config, publishConfig);
    Assert.assertTrue(publishConfig.isDnsPublishEnable());
    Assert.assertEquals(5, publishConfig.getMaxMergeSize());
    Assert.assertEquals(DnsType.AwsRoute53, publishConfig.getDnsType());
  }

  @After
  public void destroy() {
    Args.clearParam();
  }
}
