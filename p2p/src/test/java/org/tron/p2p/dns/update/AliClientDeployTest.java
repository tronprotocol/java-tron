package org.tron.p2p.dns.update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.alidns20150109.Client;
import com.aliyun.alidns20150109.models.AddDomainRecordRequest;
import com.aliyun.alidns20150109.models.AddDomainRecordResponse;
import com.aliyun.alidns20150109.models.AddDomainRecordResponseBody;
import com.aliyun.alidns20150109.models.DeleteDomainRecordRequest;
import com.aliyun.alidns20150109.models.DeleteDomainRecordResponse;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsRequest;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponse;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecords;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord;
import com.aliyun.alidns20150109.models.UpdateDomainRecordRequest;
import com.aliyun.alidns20150109.models.UpdateDomainRecordResponse;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.Tree;
import org.tron.p2p.exception.DnsException;

/**
 * AliClient.deploy end to end with the Aliyun SDK mocked: the changeThreshold
 * decision, and the add / update / delete selection inside submitChanges.
 */
public class AliClientDeployTest {

  private static final String DOMAIN = "nodes.example.org";
  private static final int SUCCESS = 200;
  private static final String PRIVATE_KEY =
      "b71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291";

  private Client sdk;
  private AliClient client;

  @Before
  public void setUp() throws Exception {
    sdk = mock(Client.class);
    client = new AliClient("alidns.aliyuncs.com", "key-id", "key-secret", 0.1);
    Field field = AliClient.class.getDeclaredField("aliDnsClient");
    field.setAccessible(true);
    field.set(client, sdk);

    when(sdk.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn((AddDomainRecordResponse) new AddDomainRecordResponse()
            .setStatusCode(SUCCESS).setBody(new AddDomainRecordResponseBody().setRecordId("r1")));
    when(sdk.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn((UpdateDomainRecordResponse) new UpdateDomainRecordResponse()
            .setStatusCode(SUCCESS));
    when(sdk.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn((DeleteDomainRecordResponse) new DeleteDomainRecordResponse()
            .setStatusCode(SUCCESS));
  }

  private static DescribeDomainRecordsResponse records(
      DescribeDomainRecordsResponseBodyDomainRecordsRecord... items) {
    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody()
        .setTotalCount((long) items.length)
        .setDomainRecords(new DescribeDomainRecordsResponseBodyDomainRecords()
            .setRecord(new ArrayList<>(Arrays.asList(items))));
    return (DescribeDomainRecordsResponse) new DescribeDomainRecordsResponse()
        .setStatusCode(SUCCESS).setBody(body);
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

  @SuppressWarnings("unchecked")
  private void setServerNodes(Set<DnsNode> nodes) throws Exception {
    Field field = AliClient.class.getDeclaredField("serverNodes");
    field.setAccessible(true);
    field.set(client, nodes);
  }

  @Test
  public void deployOnAnEmptyZoneAddsEveryRecord() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(records());

    client.deploy(DOMAIN, signedTree("192.168.0.1", "192.168.0.2"));

    // Nothing existed, so every TXT record is an add and nothing is updated.
    verify(sdk, atLeastOnce()).addDomainRecord(any(AddDomainRecordRequest.class));
    verify(sdk, never()).updateDomainRecord(any(UpdateDomainRecordRequest.class));
  }

  @Test
  public void deployBelowTheChangeThresholdSkipsEverything() throws Exception {
    // deploy() re-reads serverNodes from DNS, so the "already published" set has
    // to come back through the mocked describeDomainRecords rather than being
    // planted on the client.
    String[] ips = new String[40];
    for (int i = 0; i < ips.length; i++) {
      ips[i] = "10.0.0." + (i + 1);
    }
    Tree tree = signedTree(ips);

    List<DescribeDomainRecordsResponseBodyDomainRecordsRecord> published = new ArrayList<>();
    int index = 0;
    for (String entry : tree.getNodesEntry()) {
      published.add(new DescribeDomainRecordsResponseBodyDomainRecordsRecord()
          .setRR("n" + index++).setValue(entry).setRecordId("r" + index).setTTL(86400L));
    }
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(records(published.toArray(
            new DescribeDomainRecordsResponseBodyDomainRecordsRecord[0])));

    client.deploy(DOMAIN, tree);

    // The tree and DNS hold the same nodes, so add+delete is zero against a
    // non-empty serverNodes set and the 0.1 threshold is not met.
    verify(sdk, never()).addDomainRecord(any(AddDomainRecordRequest.class));
    verify(sdk, never()).updateDomainRecord(any(UpdateDomainRecordRequest.class));
    verify(sdk, never()).deleteDomainRecord(any(DeleteDomainRecordRequest.class));
  }

  @Test
  public void deployWrapsAnySdkFailureAsADnsException() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenThrow(new RuntimeException("aliyun is down"));

    try {
      client.deploy(DOMAIN, signedTree("192.168.0.1"));
      Assert.fail("expected a DnsException");
    } catch (DnsException expected) {
      Assert.assertEquals(DnsException.TypeEnum.DEPLOY_DOMAIN_FAILED, expected.getType());
    }
  }

  @Test
  public void deployRefreshesServerNodesFromDnsRatherThanTrustingTheCachedSet() throws Exception {
    // deploy() clears serverNodes at the end, but collectRecords() has already
    // reassigned it from the DNS response on the way in -- so asserting it is
    // empty afterwards would pass no matter what deploy() did. What is worth
    // pinning is that a stale cached set does not survive the round trip.
    setServerNodes(new HashSet<>(
        Collections.singletonList(new DnsNode(null, "10.0.0.99", null, 10000))));

    String enr = Entry.nodesPrefix + DnsNode.compress(
        Collections.singletonList(new DnsNode(null, "192.168.0.7", null, 10000)));
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(records(new DescribeDomainRecordsResponseBodyDomainRecordsRecord()
            .setRR("n0").setValue(enr).setRecordId("r0").setTTL(86400L)));

    Set<DnsNode> seen = new HashSet<>();
    Field field = AliClient.class.getDeclaredField("serverNodes");
    field.setAccessible(true);

    client.deploy(DOMAIN, signedTree("192.168.0.7"));

    // 10.0.0.99 was never in DNS, so it must be gone; and the set is emptied at
    // the end of a successful deploy.
    seen.addAll((Set<DnsNode>) field.get(client));
    Assert.assertTrue(seen.isEmpty());
  }

  @Test
  public void deleteDomainReportsTheStatusCode() throws Exception {
    when(sdk.deleteSubDomainRecords(any(
        com.aliyun.alidns20150109.models.DeleteSubDomainRecordsRequest.class)))
        .thenReturn((com.aliyun.alidns20150109.models.DeleteSubDomainRecordsResponse)
            new com.aliyun.alidns20150109.models.DeleteSubDomainRecordsResponse()
                .setStatusCode(SUCCESS));

    Assert.assertTrue(client.deleteDomain(DOMAIN));
  }
}
