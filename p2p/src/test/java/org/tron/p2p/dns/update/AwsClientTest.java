package org.tron.p2p.dns.update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.dns.update.AwsClient.RecordSet;
import org.tron.p2p.dns.update.Publish;
import org.tron.p2p.exception.DnsException;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
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

public class AwsClientTest {

  private AwsClient awsClient;
  private Route53Client mockRoute53;

  @Before
  public void setUp() throws Exception {
    awsClient = new AwsClient("testKey", "testSecret", "Z12345", "us-east-1", 0.1);
    mockRoute53 = mock(Route53Client.class);
    Field field = AwsClient.class.getDeclaredField("route53Client");
    field.setAccessible(true);
    field.set(awsClient, mockRoute53);
  }

  @Test(expected = DnsException.class)
  public void testConstructorEmptyAccessKey() throws DnsException {
    new AwsClient("", "secret", "zone", "us-east-1", 0.1);
  }

  @Test(expected = DnsException.class)
  public void testConstructorEmptySecret() throws DnsException {
    new AwsClient("key", "", "zone", "us-east-1", 0.1);
  }

  @Test(expected = DnsException.class)
  public void testConstructorNullAccessKey() throws DnsException {
    new AwsClient(null, "secret", "zone", "us-east-1", 0.1);
  }

  @Test
  public void testIsSubdomain() {
    Assert.assertTrue(AwsClient.isSubdomain("sub.example.com", "example.com"));
    Assert.assertTrue(AwsClient.isSubdomain("sub.example.com.", "example.com"));
    Assert.assertTrue(AwsClient.isSubdomain("sub.example.com.", "example.com."));
    Assert.assertTrue(AwsClient.isSubdomain("example.com", "example.com"));
    Assert.assertFalse(AwsClient.isSubdomain("other.com", "example.com"));
    Assert.assertFalse(AwsClient.isSubdomain("notexample.com", "example.com"));
  }

  @Test
  public void testNewTXTChange() {
    Change change = awsClient.newTXTChange(
        ChangeAction.CREATE, "test.example.com", 3600, "\"value1\"");
    Assert.assertEquals(ChangeAction.CREATE, change.action());
    Assert.assertEquals("test.example.com", change.resourceRecordSet().name());
    Assert.assertEquals(Long.valueOf(3600), change.resourceRecordSet().ttl());
    Assert.assertEquals(RRType.TXT, change.resourceRecordSet().type());
    Assert.assertEquals(1, change.resourceRecordSet().resourceRecords().size());
    Assert.assertEquals("\"value1\"", change.resourceRecordSet().resourceRecords().get(0).value());
  }

  @Test
  public void testNewTXTChangeMultipleValues() {
    Change change = awsClient.newTXTChange(
        ChangeAction.DELETE, "test.example.com", 600, "\"val1\"", "\"val2\"");
    Assert.assertEquals(ChangeAction.DELETE, change.action());
    Assert.assertEquals(2, change.resourceRecordSet().resourceRecords().size());
  }

  @Test
  public void testIsSameChange() {
    Change c1 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");
    Change c2 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");
    Assert.assertTrue(AwsClient.isSameChange(c1, c2));
  }

  @Test
  public void testIsSameChangeDifferentAction() {
    Change c1 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");
    Change c2 = awsClient.newTXTChange(ChangeAction.DELETE, "a.com", 300, "\"v\"");
    Assert.assertFalse(AwsClient.isSameChange(c1, c2));
  }

  @Test
  public void testIsSameChangeDifferentTTL() {
    Change c1 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");
    Change c2 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 600, "\"v\"");
    Assert.assertFalse(AwsClient.isSameChange(c1, c2));
  }

  @Test
  public void testIsSameChangeDifferentName() {
    Change c1 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");
    Change c2 = awsClient.newTXTChange(ChangeAction.CREATE, "b.com", 300, "\"v\"");
    Assert.assertFalse(AwsClient.isSameChange(c1, c2));
  }

  @Test
  public void testIsSameChangeDifferentRecordCount() {
    Change c1 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");
    Change c2 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v1\"", "\"v2\"");
    Assert.assertFalse(AwsClient.isSameChange(c1, c2));
  }

  @Test
  public void testSortChanges() {
    Change create = awsClient.newTXTChange(ChangeAction.CREATE, "b.com", 300, "\"v\"");
    Change upsert = awsClient.newTXTChange(ChangeAction.UPSERT, "a.com", 300, "\"v\"");
    Change delete = awsClient.newTXTChange(ChangeAction.DELETE, "c.com", 300, "\"v\"");

    List<Change> changes = new ArrayList<>(Arrays.asList(delete, upsert, create));
    AwsClient.sortChanges(changes);

    Assert.assertEquals(ChangeAction.CREATE, changes.get(0).action());
    Assert.assertEquals(ChangeAction.UPSERT, changes.get(1).action());
    Assert.assertEquals(ChangeAction.DELETE, changes.get(2).action());
  }

  @Test
  public void testSortChangesSameActionByName() {
    Change c1 = awsClient.newTXTChange(ChangeAction.CREATE, "b.com", 300, "\"v\"");
    Change c2 = awsClient.newTXTChange(ChangeAction.CREATE, "a.com", 300, "\"v\"");

    List<Change> changes = new ArrayList<>(Arrays.asList(c1, c2));
    AwsClient.sortChanges(changes);

    Assert.assertEquals("a.com", changes.get(0).resourceRecordSet().name());
    Assert.assertEquals("b.com", changes.get(1).resourceRecordSet().name());
  }

  @Test
  public void testComputeChangesNewRecords() {
    Map<String, String> records = new HashMap<>();
    records.put("new.example.com", "value1");
    Map<String, RecordSet> existing = new HashMap<>();

    List<Change> changes = awsClient.computeChanges("example.com", records, existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.CREATE, changes.get(0).action());
  }

  @Test
  public void testComputeChangesUpdatedRecords() {
    Map<String, String> records = new HashMap<>();
    records.put("sub.example.com", "new-value");

    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("sub.example.com",
        new RecordSet(new String[]{"\"old-value\""}, AwsClient.treeNodeTTL));

    List<Change> changes = awsClient.computeChanges("example.com", records, existing);
    // Should have UPSERT for the changed record
    boolean hasUpsert = false;
    for (Change change : changes) {
      if (change.action() == ChangeAction.UPSERT) {
        hasUpsert = true;
        break;
      }
    }
    Assert.assertTrue(hasUpsert);
  }

  @Test
  public void testComputeChangesDeletedRecords() {
    Map<String, String> records = new HashMap<>();
    // empty new records

    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("old.example.com",
        new RecordSet(new String[]{"\"old-value\""}, AwsClient.treeNodeTTL));

    List<Change> changes = awsClient.computeChanges("example.com", records, existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.DELETE, changes.get(0).action());
  }

  @Test
  public void testComputeChangesUnchangedRecords() {
    Map<String, String> records = new HashMap<>();
    records.put("sub.example.com", "same-value");

    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("sub.example.com",
        new RecordSet(new String[]{"\"same-value\""}, AwsClient.treeNodeTTL));

    List<Change> changes = awsClient.computeChanges("example.com", records, existing);
    Assert.assertTrue(changes.isEmpty());
  }

  @Test
  public void testComputeChangesTTLChanged() {
    // If existing has wrong TTL, should UPSERT even with same value.
    // Use a subdomain (not the root domain) to avoid triggering RootEntry.parseEntry
    // which requires a valid "tree-root-v1:" prefixed value.
    Map<String, String> records = new HashMap<>();
    records.put("sub.example.com", "some-value");

    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("sub.example.com",
        new RecordSet(new String[]{"\"some-value\""}, Publish.rootTTL));
    // treeNodeTTL != rootTTL, so the TTL mismatch should trigger an UPSERT

    List<Change> changes = awsClient.computeChanges("example.com", records, existing);
    boolean hasUpsert = false;
    for (Change change : changes) {
      if (change.action() == ChangeAction.UPSERT) {
        hasUpsert = true;
        break;
      }
    }
    Assert.assertTrue(hasUpsert);
  }

  @Test
  public void testMakeDeletionChanges() {
    Map<String, String> keeps = new HashMap<>();
    keeps.put("keep.example.com", "value");

    Map<String, RecordSet> existing = new HashMap<>();
    existing.put("keep.example.com",
        new RecordSet(new String[]{"\"value\""}, 3600));
    existing.put("delete.example.com",
        new RecordSet(new String[]{"\"old\""}, 3600));

    List<Change> changes = awsClient.makeDeletionChanges(keeps, existing);
    Assert.assertEquals(1, changes.size());
    Assert.assertEquals(ChangeAction.DELETE, changes.get(0).action());
    Assert.assertEquals("delete.example.com", changes.get(0).resourceRecordSet().name());
  }

  @Test
  public void testMakeDeletionChangesEmpty() {
    Map<String, String> keeps = new HashMap<>();
    Map<String, RecordSet> existing = new HashMap<>();

    List<Change> changes = awsClient.makeDeletionChanges(keeps, existing);
    Assert.assertTrue(changes.isEmpty());
  }

  @Test
  public void testSubmitChangesEmpty() {
    List<Change> changes = Collections.emptyList();
    awsClient.submitChanges(changes, "test comment");
    // Should not call route53Client at all
    verify(mockRoute53, never())
        .changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class));
  }

  @Test
  public void testSubmitChangesSuccess() {
    Change change = awsClient.newTXTChange(
        ChangeAction.CREATE, "test.example.com", 3600, "\"value\"");
    List<Change> changes = Arrays.asList(change);

    ChangeInfo changeInfo = ChangeInfo.builder()
        .id("change-123")
        .status(ChangeStatus.PENDING)
        .build();
    ChangeResourceRecordSetsResponse submitResponse =
        ChangeResourceRecordSetsResponse.builder()
            .changeInfo(changeInfo)
            .build();

    when(mockRoute53.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
        .thenReturn(submitResponse);

    GetChangeResponse getChangeResponse = GetChangeResponse.builder()
        .changeInfo(ChangeInfo.builder()
            .id("change-123")
            .status(ChangeStatus.INSYNC)
            .build())
        .build();

    when(mockRoute53.getChange(any(GetChangeRequest.class)))
        .thenReturn(getChangeResponse);

    awsClient.submitChanges(changes, "test comment");

    verify(mockRoute53, times(1))
        .changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class));
    verify(mockRoute53, times(1)).getChange(any(GetChangeRequest.class));
  }

  @Test
  public void testTestConnect() throws Exception {
    ListHostedZonesByNameResponse response = ListHostedZonesByNameResponse.builder()
        .isTruncated(false)
        .hostedZones(Collections.<HostedZone>emptyList())
        .build();

    when(mockRoute53.listHostedZonesByName(any(ListHostedZonesByNameRequest.class)))
        .thenReturn(response);

    awsClient.testConnect();
    verify(mockRoute53, times(1))
        .listHostedZonesByName(any(ListHostedZonesByNameRequest.class));
  }

  @Test
  public void testCollectRecordsEmpty() throws Exception {
    ListResourceRecordSetsResponse response = ListResourceRecordSetsResponse.builder()
        .isTruncated(false)
        .resourceRecordSets(Collections.<ResourceRecordSet>emptyList())
        .build();

    when(mockRoute53.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(response);

    Map<String, RecordSet> records = awsClient.collectRecords("example.com");
    Assert.assertNotNull(records);
    Assert.assertTrue(records.isEmpty());
  }

  @Test
  public void testCollectRecordsWithTxtRecords() throws Exception {
    ResourceRecord rr = ResourceRecord.builder().value("\"some-value\"").build();
    ResourceRecordSet rrSet = ResourceRecordSet.builder()
        .name("sub.example.com.")
        .type(RRType.TXT)
        .ttl(3600L)
        .resourceRecords(Arrays.asList(rr))
        .build();

    ListResourceRecordSetsResponse response = ListResourceRecordSetsResponse.builder()
        .isTruncated(false)
        .resourceRecordSets(Arrays.asList(rrSet))
        .build();

    when(mockRoute53.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(response);

    Map<String, RecordSet> records = awsClient.collectRecords("example.com");
    Assert.assertEquals(1, records.size());
    Assert.assertTrue(records.containsKey("sub.example.com"));
  }

  @Test
  public void testCollectRecordsSkipsNonTxt() throws Exception {
    ResourceRecord rr = ResourceRecord.builder().value("1.2.3.4").build();
    ResourceRecordSet rrSet = ResourceRecordSet.builder()
        .name("sub.example.com.")
        .type(RRType.A)
        .ttl(3600L)
        .resourceRecords(Arrays.asList(rr))
        .build();

    ListResourceRecordSetsResponse response = ListResourceRecordSetsResponse.builder()
        .isTruncated(false)
        .resourceRecordSets(Arrays.asList(rrSet))
        .build();

    when(mockRoute53.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(response);

    Map<String, RecordSet> records = awsClient.collectRecords("example.com");
    Assert.assertTrue(records.isEmpty());
  }

  @Test
  public void testCollectRecordsSkipsOtherDomains() throws Exception {
    ResourceRecord rr = ResourceRecord.builder().value("\"value\"").build();
    ResourceRecordSet rrSet = ResourceRecordSet.builder()
        .name("other.com.")
        .type(RRType.TXT)
        .ttl(3600L)
        .resourceRecords(Arrays.asList(rr))
        .build();

    ListResourceRecordSetsResponse response = ListResourceRecordSetsResponse.builder()
        .isTruncated(false)
        .resourceRecordSets(Arrays.asList(rrSet))
        .build();

    when(mockRoute53.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
        .thenReturn(response);

    Map<String, RecordSet> records = awsClient.collectRecords("example.com");
    Assert.assertTrue(records.isEmpty());
  }

  @Test
  public void testRecordSetConstructor() {
    String[] values = new String[]{"v1", "v2"};
    RecordSet rs = new RecordSet(values, 3600);
    Assert.assertArrayEquals(values, rs.values);
    Assert.assertEquals(3600, rs.ttl);
  }

  @Test
  public void testConstants() {
    Assert.assertEquals(32000, AwsClient.route53ChangeSizeLimit);
    Assert.assertEquals(1000, AwsClient.route53ChangeCountLimit);
    Assert.assertEquals(60, AwsClient.maxRetryLimit);
  }
}
