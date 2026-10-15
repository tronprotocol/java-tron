package org.tron.p2p.dns.update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.alidns20150109.Client;
import com.aliyun.alidns20150109.models.AddDomainRecordRequest;
import com.aliyun.alidns20150109.models.AddDomainRecordResponse;
import com.aliyun.alidns20150109.models.AddDomainRecordResponseBody;
import com.aliyun.alidns20150109.models.DeleteDomainRecordRequest;
import com.aliyun.alidns20150109.models.DeleteDomainRecordResponse;
import com.aliyun.alidns20150109.models.DeleteSubDomainRecordsRequest;
import com.aliyun.alidns20150109.models.DeleteSubDomainRecordsResponse;
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
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers AliClient's request/retry/decision logic with the Aliyun SDK transport
 * replaced by a mock. The logic under test is real — pagination, record matching,
 * add-vs-update selection, retry-then-give-up — only the HTTP call is faked, since
 * the alternative is a live Aliyun account.
 */
public class AliClientTest {

  private static final String DOMAIN = "example.org";
  private static final int SUCCESS = 200;
  private static final int FAILURE = 500;

  private Client sdk;
  private AliClient client;

  @Before
  public void setUp() throws Exception {
    sdk = mock(Client.class);
    client = new AliClient("alidns.aliyuncs.com", "key-id", "key-secret", 0.1);
    Field field = AliClient.class.getDeclaredField("aliDnsClient");
    field.setAccessible(true);
    field.set(client, sdk);
  }

  private static DescribeDomainRecordsResponseBodyDomainRecordsRecord record(
      String rr, String value, String recordId, long ttl) {
    return new DescribeDomainRecordsResponseBodyDomainRecordsRecord()
        .setRR(rr).setValue(value).setRecordId(recordId).setTTL(ttl);
  }

  private static DescribeDomainRecordsResponse describeResponse(
      long totalCount, DescribeDomainRecordsResponseBodyDomainRecordsRecord... records) {
    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody()
        .setTotalCount(totalCount)
        .setDomainRecords(new DescribeDomainRecordsResponseBodyDomainRecords()
            .setRecord(new ArrayList<>(Arrays.asList(records))));
    return (DescribeDomainRecordsResponse) new DescribeDomainRecordsResponse()
        .setStatusCode(SUCCESS).setBody(body);
  }

  private static AddDomainRecordResponse addResponse(int status, String recordId) {
    return (AddDomainRecordResponse) new AddDomainRecordResponse()
        .setStatusCode(status)
        .setBody(new AddDomainRecordResponseBody().setRecordId(recordId));
  }

  private static UpdateDomainRecordResponse updateResponse(int status) {
    return (UpdateDomainRecordResponse) new UpdateDomainRecordResponse().setStatusCode(status);
  }

  private static DeleteDomainRecordResponse deleteResponse(int status) {
    return (DeleteDomainRecordResponse) new DeleteDomainRecordResponse().setStatusCode(status);
  }

  // ---------- addRecord / updateRecord / deleteRecord ----------

  @Test
  public void addRecordSucceedsOnFirstCall() throws Exception {
    when(sdk.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(addResponse(SUCCESS, "rec-1"));

    Assert.assertTrue(client.addRecord(DOMAIN, "abc", "\"value\"", 60));
    verify(sdk, times(1)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void addRecordGivesUpAfterRetries() throws Exception {
    when(sdk.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(addResponse(FAILURE, null));

    Assert.assertFalse(client.addRecord(DOMAIN, "abc", "\"value\"", 60));
    // one initial attempt plus maxRetryCount (3) retries
    verify(sdk, times(4)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void addRecordSucceedsOnRetry() throws Exception {
    when(sdk.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(addResponse(FAILURE, null))
        .thenReturn(addResponse(SUCCESS, "rec-1"));

    Assert.assertTrue(client.addRecord(DOMAIN, "abc", "\"value\"", 60));
    verify(sdk, times(2)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void updateRecordSucceedsAndGivesUp() throws Exception {
    when(sdk.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn(updateResponse(SUCCESS));
    Assert.assertTrue(client.updateRecord("rec-1", "abc", "\"value\"", 60));

    when(sdk.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn(updateResponse(FAILURE));
    Assert.assertFalse(client.updateRecord("rec-1", "abc", "\"value\"", 60));
  }

  @Test
  public void deleteRecordSucceedsAndGivesUp() throws Exception {
    when(sdk.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(deleteResponse(SUCCESS));
    Assert.assertTrue(client.deleteRecord("rec-1"));

    when(sdk.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(deleteResponse(FAILURE));
    Assert.assertFalse(client.deleteRecord("rec-1"));
  }

  @Test
  public void deleteDomainReportsStatus() throws Exception {
    when(sdk.deleteSubDomainRecords(any(DeleteSubDomainRecordsRequest.class)))
        .thenReturn((DeleteSubDomainRecordsResponse)
            new DeleteSubDomainRecordsResponse().setStatusCode(SUCCESS));
    Assert.assertTrue(client.deleteDomain(DOMAIN));

    when(sdk.deleteSubDomainRecords(any(DeleteSubDomainRecordsRequest.class)))
        .thenReturn((DeleteSubDomainRecordsResponse)
            new DeleteSubDomainRecordsResponse().setStatusCode(FAILURE));
    Assert.assertFalse(client.deleteDomain(DOMAIN));
  }

  // ---------- getRecId ----------

  @Test
  public void getRecIdMatchesCaseInsensitively() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(2,
            record("other", "\"v\"", "rec-other", 60),
            record("ABC", "\"v\"", "rec-abc", 60)));

    Assert.assertEquals("rec-abc", client.getRecId(DOMAIN, "abc"));
  }

  @Test
  public void getRecIdReturnsNullWhenNoRecords() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(0));

    Assert.assertNull(client.getRecId(DOMAIN, "abc"));
  }

  @Test
  public void getRecIdReturnsNullWhenNoNameMatches() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(1, record("other", "\"v\"", "rec-other", 60)));

    Assert.assertNull(client.getRecId(DOMAIN, "abc"));
  }

  @Test
  public void getRecIdSwallowsSdkFailure() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenThrow(new RuntimeException("network down"));

    // the lookup is best-effort: a transport failure yields null, not a throw
    Assert.assertNull(client.getRecId(DOMAIN, "abc"));
  }

  // ---------- update / deleteByRR ----------

  @Test
  public void updateAddsWhenRecordIsAbsent() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(0));
    when(sdk.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(addResponse(SUCCESS, "rec-new"));

    Assert.assertEquals("rec-new", client.update(DOMAIN, "abc", "\"value\"", 60));
    verify(sdk, times(1)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void updateUpdatesWhenRecordExists() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(1, record("abc", "\"old\"", "rec-existing", 60)));
    when(sdk.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn((UpdateDomainRecordResponse) new UpdateDomainRecordResponse()
            .setStatusCode(SUCCESS)
            .setBody(new com.aliyun.alidns20150109.models.UpdateDomainRecordResponseBody()
                .setRecordId("rec-existing")));

    Assert.assertEquals("rec-existing", client.update(DOMAIN, "abc", "\"new\"", 60));
    verify(sdk, times(1)).updateDomainRecord(any(UpdateDomainRecordRequest.class));
    verify(sdk, times(0)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void deleteByRrDeletesOnlyWhenFound() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(1, record("abc", "\"v\"", "rec-abc", 60)));
    when(sdk.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(deleteResponse(SUCCESS));

    Assert.assertTrue(client.deleteByRR(DOMAIN, "abc"));
    verify(sdk, times(1)).deleteDomainRecord(any(DeleteDomainRecordRequest.class));
  }

  @Test
  public void deleteByRrIsANoOpWhenAbsent() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(0));

    // nothing to delete is success, not failure
    Assert.assertTrue(client.deleteByRR(DOMAIN, "abc"));
    verify(sdk, times(0)).deleteDomainRecord(any(DeleteDomainRecordRequest.class));
  }

  @Test
  public void deleteByRrReportsNonSuccessStatus() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(1, record("abc", "\"v\"", "rec-abc", 60)));
    when(sdk.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(deleteResponse(FAILURE));

    Assert.assertFalse(client.deleteByRR(DOMAIN, "abc"));
  }

  // ---------- collectRecords ----------

  @Test
  public void collectRecordsStripsTrailingDotAndKeysByName() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(2,
            record("abc.", "\"v1\"", "rec-1", 60),
            record("def", "\"v2\"", "rec-2", 60)));

    Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> records =
        client.collectRecords(DOMAIN);

    Assert.assertEquals(2, records.size());
    Assert.assertTrue("trailing dot must be stripped from the RR", records.containsKey("abc"));
    Assert.assertTrue(records.containsKey("def"));
    Assert.assertEquals("rec-1", records.get("abc").getRecordId());
  }

  @Test
  public void collectRecordsWalksEveryPage() throws Exception {
    // pageSize is 20, so a totalCount of 25 forces a second request
    List<DescribeDomainRecordsResponseBodyDomainRecordsRecord> page1 = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      page1.add(record("n" + i, "\"v\"", "rec-" + i, 60));
    }
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(25,
            page1.toArray(new DescribeDomainRecordsResponseBodyDomainRecordsRecord[0])))
        .thenReturn(describeResponse(25,
            record("n20", "\"v\"", "rec-20", 60)));

    Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> records =
        client.collectRecords(DOMAIN);

    verify(sdk, times(2)).describeDomainRecords(any(DescribeDomainRecordsRequest.class));
    Assert.assertEquals(21, records.size());
    Assert.assertTrue(records.containsKey("n20"));
  }

  /**
   * A value carrying the nodes: prefix reaches NodesEntry.parseEntry, and one that
   * fails to parse must be logged and skipped rather than aborting the collection.
   *
   * <p>The payload here is valid URL-safe base64 whose bytes are not a valid EndPoints
   * message, so decoding succeeds and the protobuf parse fails — which is the path
   * NodesEntry.parseEntry actually converts into DnsException.
   *
   * <p>Note it must be *valid* base64: Algorithm.decode64 (Algorithm.java:121) calls
   * Base64.getUrlDecoder().decode() directly, which throws an unchecked
   * IllegalArgumentException on malformed input. NodesEntry.parseEntry catches only
   * InvalidProtocolBufferException and UnknownHostException, so that escapes both it
   * and the DnsException-only catch at AliClient.collectRecords:138, aborting the whole
   * collection. A single corrupt TXT record under the domain is enough. Pre-existing in
   * libp2p, reported rather than fixed here.
   */
  @Test
  public void collectRecordsIgnoresUnparseableNodesEntry() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(describeResponse(2,
            record("abc", "nodes:QUJDRA==", "rec-1", 60),
            record("def", "\"v2\"", "rec-2", 60)));

    Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> records =
        client.collectRecords(DOMAIN);

    Assert.assertEquals(2, records.size());
  }

  @Test
  public void collectRecordsPropagatesNonSuccessStatus() throws Exception {
    DescribeDomainRecordsResponse failed = describeResponse(0);
    failed.setStatusCode(FAILURE);
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(failed);

    try {
      client.collectRecords(DOMAIN);
      Assert.fail("a non-200 status must not be reported as an empty record set");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("Failed to request domain records"));
    }
  }

  @Test
  public void collectRecordsPropagatesSdkFailure() throws Exception {
    when(sdk.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenThrow(new RuntimeException("network down"));

    try {
      client.collectRecords(DOMAIN);
      Assert.fail("a transport failure must propagate, not yield an empty map");
    } catch (Exception e) {
      Assert.assertEquals("network down", e.getMessage());
    }
  }
}
