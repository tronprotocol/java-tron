package org.tron.p2p.dns.update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.dns.update.AwsClient.RecordSet;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.GetChangeRequest;
import software.amazon.awssdk.services.route53.model.GetChangeResponse;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesByNameRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesByNameResponse;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

/**
 * The Route53-facing half of AwsClient with the SDK transport mocked: record
 * collection and its pagination, zone discovery, and the publish decision that
 * changeThreshold gates. Only the HTTP call is faked; the logic is real.
 */
public class AwsClientRecordsTest {

  private static final String DOMAIN = "nodes.example.org";
  private static final String PRIVATE_KEY =
      "b71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291";

  private Route53Client sdk;
  private AwsClient client;

  @Before
  public void setUp() throws Exception {
    sdk = mock(Route53Client.class);
    client = new AwsClient("access-key", "access-secret", "zone-id", "us-east-1", 0.1);
    Field field = AwsClient.class.getDeclaredField("route53Client");
    field.setAccessible(true);
    field.set(client, sdk);
  }

  private static ResourceRecordSet txt(String name, long ttl, String... values) {
    List<ResourceRecord> records = new ArrayList<>();
    for (String value : values) {
      records.add(ResourceRecord.builder().value(value).build());
    }
    return ResourceRecordSet.builder()
        .name(name).type(RRType.TXT).ttl(ttl).resourceRecords(records).build();
  }

  private static ListResourceRecordSetsResponse page(boolean truncated,
      ResourceRecordSet... sets) {
    return (ListResourceRecordSetsResponse) ListResourceRecordSetsResponse.builder()
        .resourceRecordSets(Arrays.asList(sets))
        .isTruncated(truncated)
        .nextRecordName("cursor")
        .nextRecordType(RRType.TXT)
        .build();
  }

  private static String quoted(String value) {
    return "\"" + value + "\"";
  }

  private static Tree signedTree(String... ips) throws Exception {
    List<String> enrs = new ArrayList<>();
    for (String ip : ips) {
      enrs.add(Entry.nodesPrefix
          + DnsNode.compress(Collections.singletonList(new DnsNode(null, ip, null, 10000))));
    }
    Tree tree = new Tree();
    tree.makeTree(1, enrs, new ArrayList<String>(), PRIVATE_KEY);
    return tree;
  }

  @Test
  public void collectRecordsKeepsTxtSubdomainsAndStripsTheTrailingDot() throws Exception {
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class))).thenReturn(
        page(false,
            txt("a." + DOMAIN + ".", 600, quoted("hello")),
            // Not a subdomain of DOMAIN, so it is skipped.
            txt("other.example.com.", 600, quoted("nope")),
            // Right name, wrong type.
            ResourceRecordSet.builder().name("b." + DOMAIN + ".").type(RRType.A).ttl(60L)
                .resourceRecords(Collections.<ResourceRecord>emptyList()).build()));

    Map<String, RecordSet> existing = client.collectRecords(DOMAIN);

    Assert.assertEquals(1, existing.size());
    Assert.assertTrue(existing.containsKey("a." + DOMAIN));
  }

  @Test
  public void collectRecordsWalksEveryPage() throws Exception {
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(page(true, txt("a." + DOMAIN + ".", 600, quoted("one"))))
        .thenReturn(page(false, txt("b." + DOMAIN + ".", 600, quoted("two"))));

    Map<String, RecordSet> existing = client.collectRecords(DOMAIN);

    Assert.assertEquals(2, existing.size());
    verify(sdk, times(2)).listResourceRecordSets(any(ListResourceRecordSetsRequest.class));
  }

  @Test
  public void collectRecordsJoinsSplitValuesBeforeParsing() throws Exception {
    // Route53 stores long TXT values as several quoted chunks; they have to be
    // rejoined before the entry can be recognised.
    String enr = Entry.nodesPrefix
        + DnsNode.compress(Collections.singletonList(
            new DnsNode(null, "192.168.0.1", null, 10000)));
    int half = enr.length() / 2;
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class))).thenReturn(
        page(false, txt("a." + DOMAIN + ".", 600,
            quoted(enr.substring(0, half)), quoted(enr.substring(half)))));

    client.collectRecords(DOMAIN);

    Field field = AwsClient.class.getDeclaredField("serverNodes");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Set<DnsNode> serverNodes = (Set<DnsNode>) field.get(client);
    Assert.assertEquals(1, serverNodes.size());
  }

  @Test
  public void malformedBase64InANodesEntryAbortsTheWholeCollection() throws Exception {
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class))).thenReturn(
        page(false, txt("a." + DOMAIN + ".", 600, quoted(Entry.nodesPrefix + "!!!not-base64!!!"))));

    // The catch around parseEntry only handles DnsException, but
    // Algorithm.decode64 raises an unchecked IllegalArgumentException, so a
    // single corrupt TXT record takes down the entire publish rather than being
    // skipped. Reported in the PR description as a deferred upstream defect and
    // pinned here so a fix is visible as a test change.
    try {
      client.collectRecords(DOMAIN);
      Assert.fail("expected the unchecked decoder failure to escape");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("base64"));
    }
  }

  @Test
  public void findZoneIdIsUsedWhenNoZoneWasConfigured() throws Exception {
    AwsClient noZone = new AwsClient("access-key", "access-secret", null, "us-east-1", 0.1);
    Field field = AwsClient.class.getDeclaredField("route53Client");
    field.setAccessible(true);
    field.set(noZone, sdk);

    when(sdk.listHostedZonesByName(any(ListHostedZonesByNameRequest.class))).thenReturn(
        (ListHostedZonesByNameResponse) ListHostedZonesByNameResponse.builder()
            .hostedZones(Collections.singletonList(HostedZone.builder()
                .id("/hostedzone/Z0404776204LVYA8EZNVH").name("example.org.").build()))
            .isTruncated(false).build());
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(page(false));
    when(sdk.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
        .thenReturn(changeResponse());
    when(sdk.getChange(any(GetChangeRequest.class))).thenReturn(insync());

    noZone.deploy(DOMAIN, signedTree("192.168.0.1"));

    Field zoneId = AwsClient.class.getDeclaredField("zoneId");
    zoneId.setAccessible(true);
    Assert.assertEquals("Z0404776204LVYA8EZNVH", zoneId.get(noZone));
  }

  private static ChangeResourceRecordSetsResponse changeResponse() {
    return (ChangeResourceRecordSetsResponse) ChangeResourceRecordSetsResponse.builder()
        .changeInfo(ChangeInfo.builder().id("C1").status(ChangeStatus.PENDING).build()).build();
  }

  private static GetChangeResponse insync() {
    return (GetChangeResponse) GetChangeResponse.builder()
        .changeInfo(ChangeInfo.builder().id("C1").status(ChangeStatus.INSYNC).build()).build();
  }

  @Test
  public void deployOnAnEmptyZoneSubmitsEverything() throws Exception {
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(page(false));
    when(sdk.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
        .thenReturn(changeResponse());
    when(sdk.getChange(any(GetChangeRequest.class))).thenReturn(insync());

    client.deploy(DOMAIN, signedTree("192.168.0.1", "192.168.0.2"));

    // serverNodes was empty, so the threshold check is bypassed entirely.
    verify(sdk, times(1)).changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class));
  }

  @Test
  public void submitChangesDoesNothingWhenThereIsNothingToDo() {
    client.submitChanges(new ArrayList<>(), "no-op");
    verify(sdk, times(0)).changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class));
  }

  @Test
  public void deleteDomainRemovesEveryCollectedRecord() throws Exception {
    when(sdk.listResourceRecordSets(any(ListResourceRecordSetsRequest.class))).thenReturn(
        page(false,
            txt("a." + DOMAIN + ".", 600, quoted("one")),
            txt("b." + DOMAIN + ".", 600, quoted("two"))));
    when(sdk.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
        .thenReturn(changeResponse());
    when(sdk.getChange(any(GetChangeRequest.class))).thenReturn(insync());

    Assert.assertTrue(client.deleteDomain(DOMAIN));
    verify(sdk, times(1)).changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class));
  }
}
