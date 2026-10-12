package org.tron.p2p.dns.update;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers PublishService.checkConfig, the gate that decides whether the DNS publish
 * service starts at all. Every rejection branch is exercised: a misconfigured node
 * that silently starts publishing (or silently refuses to) is hard to diagnose in
 * production, so each condition is pinned individually.
 */
public class PublishServiceTest {

  private static Method checkConfig;
  private static PublishService service;

  @BeforeClass
  public static void init() throws Exception {
    checkConfig = PublishService.class.getDeclaredMethod("checkConfig", boolean.class,
        PublishConfig.class);
    checkConfig.setAccessible(true);
    service = new PublishService();
  }

  private boolean check(boolean supportV4, PublishConfig config) throws Exception {
    return (Boolean) checkConfig.invoke(service, supportV4, config);
  }

  /** A config that would pass, so each test can invalidate exactly one field. */
  private PublishConfig validAliYunConfig() {
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AliYun);
    config.setDnsDomain("nodes.example.org");
    config.setAccessKeyId("key-id");
    config.setAccessKeySecret("key-secret");
    config.setAliDnsEndpoint("alidns.aliyuncs.com");
    return config;
  }

  private PublishConfig validAwsConfig() {
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AwsRoute53);
    config.setDnsDomain("nodes.example.org");
    config.setAccessKeyId("key-id");
    config.setAccessKeySecret("key-secret");
    config.setAwsRegion("us-east-1");
    return config;
  }

  @Test
  public void acceptsFullyConfiguredAliYun() throws Exception {
    Assert.assertTrue(check(true, validAliYunConfig()));
  }

  @Test
  public void acceptsFullyConfiguredAwsRoute53() throws Exception {
    Assert.assertTrue(check(true, validAwsConfig()));
  }

  @Test
  public void rejectsWhenPublishDisabled() throws Exception {
    // disabled is the default; it short-circuits before any other validation,
    // so an otherwise-empty config must still be rejected without error
    Assert.assertFalse(check(true, new PublishConfig()));

    PublishConfig config = validAliYunConfig();
    config.setDnsPublishEnable(false);
    Assert.assertFalse(check(true, config));
  }

  @Test
  public void rejectsWithoutIpV4() throws Exception {
    // publishing advertises an A record, so a v4 address is required even when
    // every other field is present
    Assert.assertFalse(check(false, validAliYunConfig()));
    Assert.assertFalse(check(false, validAwsConfig()));
  }

  @Test
  public void rejectsMissingDnsType() throws Exception {
    PublishConfig config = validAliYunConfig();
    config.setDnsType(null);
    Assert.assertFalse(check(true, config));
  }

  @Test
  public void rejectsMissingDnsDomain() throws Exception {
    PublishConfig config = validAliYunConfig();
    config.setDnsDomain(null);
    Assert.assertFalse(check(true, config));

    config.setDnsDomain("");
    Assert.assertFalse(check(true, config));
  }

  @Test
  public void rejectsIncompleteAliYunCredentials() throws Exception {
    PublishConfig noKeyId = validAliYunConfig();
    noKeyId.setAccessKeyId(null);
    Assert.assertFalse(check(true, noKeyId));

    PublishConfig noSecret = validAliYunConfig();
    noSecret.setAccessKeySecret("");
    Assert.assertFalse(check(true, noSecret));

    PublishConfig noEndpoint = validAliYunConfig();
    noEndpoint.setAliDnsEndpoint(null);
    Assert.assertFalse(check(true, noEndpoint));
  }

  @Test
  public void rejectsIncompleteAwsCredentials() throws Exception {
    PublishConfig noKeyId = validAwsConfig();
    noKeyId.setAccessKeyId("");
    Assert.assertFalse(check(true, noKeyId));

    PublishConfig noSecret = validAwsConfig();
    noSecret.setAccessKeySecret(null);
    Assert.assertFalse(check(true, noSecret));

    PublishConfig noRegion = validAwsConfig();
    noRegion.setAwsRegion(null);
    Assert.assertFalse(check(true, noRegion));
  }

  @Test
  public void aliDnsEndpointNotRequiredForAws() throws Exception {
    // the endpoint check is scoped to AliYun; an AWS config must not be rejected
    // for leaving it unset
    PublishConfig config = validAwsConfig();
    config.setAliDnsEndpoint(null);
    Assert.assertTrue(check(true, config));
  }

  @Test
  public void awsRegionNotRequiredForAliYun() throws Exception {
    // and symmetrically, the region check is scoped to AwsRoute53
    PublishConfig config = validAliYunConfig();
    config.setAwsRegion(null);
    Assert.assertTrue(check(true, config));
  }

  @SuppressWarnings("unchecked")
  private List<String> nodesFor(PublishConfig config) throws Exception {
    Method getNodes = PublishService.class.getDeclaredMethod("getNodes", PublishConfig.class);
    getNodes.setAccessible(true);
    return (List<String>) getNodes.invoke(service, config);
  }

  /**
   * When staticNodes are configured they are published verbatim instead of whatever the
   * node happens to be connected to, so this path must not consult NodeManager at all.
   * It also has to route v4 and v6 addresses into the right field of Node.
   */
  @Test
  public void buildsPublishableNodesFromStaticV4Addresses() throws Exception {
    PublishConfig config = validAliYunConfig();
    config.setStaticNodes(Arrays.asList(
        new InetSocketAddress("1.2.3.4", 18888),
        new InetSocketAddress("5.6.7.8", 18889)));

    List<String> nodes = nodesFor(config);
    Assert.assertFalse(nodes.isEmpty());
    // Tree.merge emits nodes: entries, one per merged group
    for (String entry : nodes) {
      Assert.assertTrue("expected a nodes: entry, got " + entry, entry.startsWith("nodes:"));
    }
  }

  @Test
  public void buildsPublishableNodesFromStaticV6Addresses() throws Exception {
    PublishConfig config = validAliYunConfig();
    config.setStaticNodes(Collections.singletonList(
        new InetSocketAddress("2001:db8::1", 18888)));

    List<String> nodes = nodesFor(config);
    Assert.assertFalse(nodes.isEmpty());
    Assert.assertTrue(nodes.get(0).startsWith("nodes:"));
  }

  @Test
  public void maxMergeSizeBoundsEachPublishedEntry() throws Exception {
    PublishConfig config = validAliYunConfig();
    List<InetSocketAddress> statics = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      statics.add(new InetSocketAddress("10.0.0." + i, 18888));
    }
    config.setStaticNodes(statics);

    // 12 nodes at a merge size of 3 cannot fit in fewer than 4 entries
    config.setMaxMergeSize(3);
    List<String> merged = nodesFor(config);
    Assert.assertTrue("expected at least 4 entries, got " + merged.size(), merged.size() >= 4);

    // a larger merge size packs the same nodes into fewer entries
    config.setMaxMergeSize(12);
    Assert.assertTrue(nodesFor(config).size() <= merged.size());
  }
}
