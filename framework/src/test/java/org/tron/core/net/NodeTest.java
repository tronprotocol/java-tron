package org.tron.core.net;

import static org.tron.core.net.message.handshake.HelloMessage.getEndpointFromNode;

import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.config.args.Args;
import org.tron.p2p.discover.Node;
import org.tron.protos.Discover.Endpoint;

@Slf4j
public class NodeTest {

  @Test
  public void testIpV4() {
    InetSocketAddress address1 = Args.parseInetSocketAddress("192.168.0.1:18888");
    Assert.assertNotNull(address1);
    InetSocketAddress address2 = Args.parseInetSocketAddress("192.168.0.1");
    Assert.assertNull(address2);
  }

  @Test
  public void testIpV6() {
    InetSocketAddress address1 = Args.parseInetSocketAddress("fe80::216:3eff:fe0e:23bb:18888");
    Assert.assertNotNull(address1);
    InetSocketAddress address2 = Args.parseInetSocketAddress("fe80::216:3eff:fe0e:23bb");
    Assert.assertNull(address2);
  }

  @Test
  public void testEndpointFromNode() {
    Node node = new Node(null, null, null, 18888);
    Endpoint endpoint = getEndpointFromNode(node);
    Assert.assertTrue(endpoint.getNodeId().isEmpty());
    Assert.assertTrue(endpoint.getAddress().isEmpty());
    Assert.assertTrue(endpoint.getAddressIpv6().isEmpty());
  }
}
