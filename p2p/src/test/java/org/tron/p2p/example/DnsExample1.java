package org.tron.p2p.example;


import static java.lang.Thread.sleep;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.discover.Node;
import org.tron.p2p.dns.update.DnsType;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.stats.P2pStats;

public class DnsExample1 {

  private P2pService p2pService = new P2pService();

  public void startP2pService() {
    // config p2p parameters
    P2pConfig config = new P2pConfig();
    initDnsPublishConfig(config);

    // start p2p service
    p2pService.start(config);

    // after start about 300 seconds, you can find following log:
    // Trying to publish tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example.org
    // that is your tree url. you can publish your tree url on any somewhere such as github.
    // for others, this url is a known tree url
    while (true) {
      try {
        sleep(1000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public void closeP2pService() {
    p2pService.close();
  }

  public void connect(InetSocketAddress address) {
    p2pService.connect(address);
  }

  public P2pStats getP2pStats() {
    return p2pService.getP2pStats();
  }

  public List<Node> getAllNodes() {
    return p2pService.getAllNodes();
  }

  public List<Node> getTableNodes() {
    return p2pService.getTableNodes();
  }

  public List<Node> getConnectableNodes() {
    return p2pService.getConnectableNodes();
  }

  private void initDnsPublishConfig(P2pConfig config) {
    // set p2p version
    config.setNetworkId(11111);

    // set tcp and udp listen port
    config.setPort(18888);

    // must turn node discovery on
    config.setDiscoverEnable(true);

    // set discover seed nodes
    List<InetSocketAddress> seedNodeList = new ArrayList<>();
    seedNodeList.add(new InetSocketAddress("13.124.62.58", 18888));
    seedNodeList.add(new InetSocketAddress("2600:1f13:908:1b00:e1fd:5a84:251c:a32a", 18888));
    seedNodeList.add(new InetSocketAddress("127.0.0.4", 18888));
    config.setSeedNodes(seedNodeList);

    PublishConfig publishConfig = new PublishConfig();
    // config node private key, and then you should publish your public key
    publishConfig.setDnsPrivate("b71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291");

    // config your domain
    publishConfig.setDnsDomain("nodes.example.org");

    // if you know other tree urls, you can attach it. it is optional
    String[] urls = new String[] {
        "tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example1.org",
        "tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example2.org",};
    publishConfig.setKnownTreeUrls(Arrays.asList(urls));

    //add your api key of aws or aliyun
    publishConfig.setDnsType(DnsType.AwsRoute53);
    publishConfig.setAccessKeyId("your access key");
    publishConfig.setAccessKeySecret("your access key secret");
    publishConfig.setAwsHostZoneId("your host zone id");
    publishConfig.setAwsRegion("us-east-1");

    // enable dns publish
    publishConfig.setDnsPublishEnable(true);

    // enable publish, so your nodes can be automatically published on domain periodically and others can download them
    config.setPublishConfig(publishConfig);
  }

}
